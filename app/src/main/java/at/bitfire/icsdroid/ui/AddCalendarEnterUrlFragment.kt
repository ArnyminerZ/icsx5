/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.AddCalendarEnterUrlBinding
import at.bitfire.icsdroid.ui.viewmodel.ValidationModel
import java.net.URI
import java.net.URISyntaxException
import okhttp3.HttpUrl.Companion.toHttpUrl

class AddCalendarEnterUrlFragment: Fragment() {

    private val subscriptionSettingsModel by activityViewModels<SubscriptionSettingsFragment.SubscriptionSettingsModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()
    private val validationModel by activityViewModels<ValidationModel>()
    private lateinit var binding: AddCalendarEnterUrlBinding

    private var menu: Menu? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            // keep the picked file accessible after the first sync and reboots
            requireActivity().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            binding.url.editText?.setText(uri.toString())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val invalidate = Observer<Any?> {
            val itemNext = menu?.findItem(R.id.next)

            val uri = validateUri()

            val authOK =
                if (credentialsModel.requiresAuth.value == true)
                    !credentialsModel.username.value.isNullOrEmpty() && !credentialsModel.password.value.isNullOrEmpty()
                else
                    true
            itemNext?.isEnabled = uri != null && authOK
        }
        arrayOf(
            subscriptionSettingsModel.url,
            credentialsModel.requiresAuth,
            credentialsModel.username,
            credentialsModel.password
        ).forEach {
            it.observe(viewLifecycleOwner, invalidate)
        }

        validationModel.isVerifyingUrl.observe(viewLifecycleOwner) { isVerifyingUrl ->
            menu?.findItem(R.id.next)?.isEnabled = !isVerifyingUrl
            binding.urlEdit.isEnabled = !isVerifyingUrl
            binding.pickStorageFile.isEnabled = !isVerifyingUrl
        }

        validationModel.result.observe(viewLifecycleOwner) { info ->
            val exception = info.exception
            if (exception == null) {
                subscriptionSettingsModel.url.value = info.uri.toString()

                if (subscriptionSettingsModel.color.value == null)
                    subscriptionSettingsModel.color.value =
                        info.calendarColor ?: ContextCompat.getColor(requireContext(), R.color.lightblue)

                if (subscriptionSettingsModel.title.value.isNullOrBlank())
                    subscriptionSettingsModel.title.value = info.calendarName ?: info.uri.toString()

                parentFragmentManager
                    .beginTransaction()
                    .replace(android.R.id.content, AddCalendarDetailsFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            } else {
                val errorMessage =
                    exception.localizedMessage ?: exception.message ?: exception.toString()
                AlertFragment.create(errorMessage, exception).show(parentFragmentManager, null)
            }
        }

        binding = AddCalendarEnterUrlBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = subscriptionSettingsModel

        activity?.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.enter_url_fragment, menu)
                    this@AddCalendarEnterUrlFragment.menu = menu
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    TODO("Not yet implemented")
                }
            },
            viewLifecycleOwner
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.pickStorageFile.setOnClickListener {
            pickFile.launch(arrayOf("text/calendar"))
        }

        validateUri()
    }

    override fun onPause() {
        super.onPause()
        HttpClient.setForeground(false)
    }

    override fun onResume() {
        super.onResume()
        HttpClient.setForeground(true)
    }


    /* dynamic changes */

    private fun validateUri(): Uri? {
        var errorMsg: String? = null

        var uri: Uri
        try {
            try {
                uri = Uri.parse(subscriptionSettingsModel.url.value ?: return null)
            } catch (e: URISyntaxException) {
                Log.d(Constants.TAG, "Invalid URL", e)
                errorMsg = e.localizedMessage
                return null
            }

            Log.i(Constants.TAG, uri.toString())

            if (uri.scheme.equals("webcal", true)) {
                uri = uri.buildUpon().scheme("http").build()
                subscriptionSettingsModel.url.value = uri.toString()
                return null
            } else if (uri.scheme.equals("webcals", true)) {
                uri = uri.buildUpon().scheme("https").build()
                subscriptionSettingsModel.url.value = uri.toString()
                return null
            }

            val supportsAuthenticate = HttpUtils.supportsAuthentication(uri)
            binding.credentials.visibility = if (supportsAuthenticate) View.VISIBLE else View.GONE
            when (uri.scheme?.lowercase()) {
                "content" -> {
                    // SAF file, no need for auth
                }
                "http", "https" -> {
                    // check whether the URL is valid
                    try {
                        uri.toString().toHttpUrl()
                    } catch (e: IllegalArgumentException) {
                        Log.w(Constants.TAG, "Invalid URI", e)
                        errorMsg = e.localizedMessage
                        return null
                    }

                    // extract user name and password from URL
                    uri.userInfo?.let { userInfo ->
                        val credentials = userInfo.split(':')
                        credentialsModel.requiresAuth.value = true
                        credentialsModel.username.value = credentials.elementAtOrNull(0)
                        credentialsModel.password.value = credentials.elementAtOrNull(1)

                        val urlWithoutPassword = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
                        subscriptionSettingsModel.url.value = urlWithoutPassword.toString()
                        return null
                    }
                }
                else -> {
                    errorMsg = getString(R.string.add_calendar_need_valid_uri)
                    return null
                }
            }

            // warn if auth. required and not using HTTPS
            binding.insecureAuthenticationWarning.visibility =
                    if (credentialsModel.requiresAuth.value == true && !uri.scheme.equals("https", true))
                        View.VISIBLE
                    else
                        View.GONE
        } finally {
            binding.url.error = errorMsg
        }
        return uri
    }


    /* actions */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.next) {

            // flush the credentials if auth toggle is disabled
            if (credentialsModel.requiresAuth.value != true) {
                credentialsModel.username.value = null
                credentialsModel.password.value = null
            }

            val uri: Uri? = Uri.parse(subscriptionSettingsModel.url.value)
            // FIXME - this should be caught somehow
            check(uri != null) { "No URL given" }
            val authenticate = credentialsModel.requiresAuth.value ?: false

            validationModel.validate(
                uri,
                if (authenticate) credentialsModel.username.value else null,
                if (authenticate) credentialsModel.password.value else null
            )

            return true
        }
        return false
    }

}
