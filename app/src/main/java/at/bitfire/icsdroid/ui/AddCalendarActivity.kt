/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.model.CredentialsModel
import com.google.accompanist.themeadapter.material.MdcTheme
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI
import java.net.URISyntaxException

@OptIn(ExperimentalFoundationApi::class)
class AddCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsFragment.SubscriptionSettingsModel>()
    private val credentialsModel by viewModels<CredentialsModel>()

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // keep the picked file accessible after the first sync and reboots
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                subscriptionSettingsModel.url.value = uri.toString()
            }
        }


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        setContent {
            MdcTheme {
                val pagerState = rememberPagerState { 2 }

                val requiresAuth by credentialsModel.requiresAuth.observeAsState(false)
                val username: String? by credentialsModel.username.observeAsState("")
                val password: String? by credentialsModel.password.observeAsState("")

                val isInsecure by credentialsModel.isInsecure.observeAsState(initial = false)

                val url by subscriptionSettingsModel.url.observeAsState(initial = "")
                val urlError by subscriptionSettingsModel.urlError.observeAsState(initial = "")

                val supportsAuthentication by subscriptionSettingsModel.supportsAuthentication.observeAsState(initial = false)

                Scaffold(
                    topBar = { TopBar() }
                ) { paddingValues ->
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) { page ->
                        when (page) {
                            0 -> EnterUrlComposable(
                                requiresAuth,
                                credentialsModel.requiresAuth::setValue,
                                username,
                                credentialsModel.username::setValue,
                                password,
                                credentialsModel.password::setValue,
                                isInsecure,
                                url,
                                subscriptionSettingsModel.url::setValue,
                                urlError,
                                supportsAuthentication
                            ) { pickFile.launch(arrayOf("text/calendar")) }
                        }
                    }
                }
            }
        }

        if (inState == null) {
            intent?.apply {
                data?.let { uri ->
                    subscriptionSettingsModel.url.value = uri.toString()
                }
                getStringExtra(EXTRA_TITLE)?.let {
                    subscriptionSettingsModel.title.value = it
                }
                if (hasExtra(EXTRA_COLOR))
                    subscriptionSettingsModel.color.value = getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
            }
        }
    }

    @Composable
    private fun TopBar() {
        val url by subscriptionSettingsModel.url.observeAsState()

        TopAppBar(
            title = { Text(stringResource(R.string.activity_add_calendar)) },
            navigationIcon = {
                IconButton(
                    onClick = {
                        // TODO: If not on the first page, go back instead of finishing
                        finish()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        // TODO: Content description for back
                        contentDescription = null
                    )
                }
            },
            actions = {
                val credentialsValid by credentialsModel.isValid.observeAsState(initial = true)
                var uriValid by remember { mutableStateOf(false) }

                LaunchedEffect(url) {
                    uriValid = validateUri() != null
                }

                IconButton(
                    onClick = {
                        // flush the credentials if auth toggle is disabled
                        if (credentialsModel.requiresAuth.value != true) {
                            credentialsModel.username.value = null
                            credentialsModel.password.value = null
                        }

                        // FIXME: Depends on https://github.com/ArnyminerZ/icsx5/pull/31
                        AddCalendarValidationFragment().show(supportFragmentManager, "validation")
                    },
                    enabled = uriValid && credentialsValid
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = stringResource(R.string.action_next)
                    )
                }
            }
        )
    }

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

            uri = subscriptionSettingsModel.replaceUrlScheme(uri)

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

                        val urlWithoutPassword =
                            URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
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
            credentialsModel.isInsecure.value = credentialsModel.requiresAuth.value == true &&
                !uri.scheme.equals("https", true)
        } finally {
            subscriptionSettingsModel.urlError.value = errorMsg
        }
        return uri
    }

}
