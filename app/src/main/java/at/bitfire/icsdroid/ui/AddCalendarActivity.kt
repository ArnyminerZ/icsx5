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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.ValidationModel
import com.google.accompanist.themeadapter.material.MdcTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI
import java.net.URISyntaxException

@OptIn(ExperimentalFoundationApi::class)
class AddCalendarActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsFragment.SubscriptionSettingsModel>()
    private val credentialsModel by viewModels<CredentialsModel>()
    private val validationModel by viewModels<ValidationModel>()

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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setContent {
            MdcTheme {
                val scope = rememberCoroutineScope()

                val pagerState = rememberPagerState { 2 }

                val url: String? by subscriptionSettingsModel.url.observeAsState("")
                val urlError: String? by subscriptionSettingsModel.urlError.observeAsState(null)
                val supportsAuthentication: Boolean by subscriptionSettingsModel.supportsAuthentication.observeAsState(
                    false
                )
                val requiresAuth by credentialsModel.requiresAuth.observeAsState(false)
                val username: String? by credentialsModel.username.observeAsState("")
                val password: String? by credentialsModel.password.observeAsState("")
                val isInsecure: Boolean by credentialsModel.isInsecure.observeAsState(false)
                val isVerifyingUrl: Boolean by validationModel.isVerifyingUrl.observeAsState(false)
                val validationResult by validationModel.result.observeAsState()

                Scaffold(
                    topBar = { TopAppBar(pagerState) }
                ) { paddingValues ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) { page ->
                        when (page) {
                            0 -> {
                                EnterUrlComposable(
                                    requiresAuth = requiresAuth,
                                    onRequiresAuthChange = credentialsModel.requiresAuth::setValue,
                                    username = username,
                                    onUsernameChange = credentialsModel.username::setValue,
                                    password = password,
                                    onPasswordChange = credentialsModel.password::setValue,
                                    isInsecure = isInsecure,
                                    url = url,
                                    onUrlChange = subscriptionSettingsModel.url::setValue,
                                    urlError = urlError,
                                    supportsAuthentication = supportsAuthentication,
                                    isVerifyingUrl = isVerifyingUrl,
                                    validationResult = validationResult,
                                    onValidationResultDismiss = {
                                        validationModel.result.value = null
                                    },
                                    onPickFileRequested = { pickFile.launch(arrayOf("text/calendar")) },
                                    onSubmit = { onNextRequested(pagerState, scope) }
                                )
                            }

                            1 -> {
                                // TODO : Replace with composable when #34 is complete

                            }
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
                    subscriptionSettingsModel.color.value =
                        getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        HttpClient.setForeground(false)
    }

    override fun onResume() {
        super.onResume()
        HttpClient.setForeground(true)
    }

    @Composable
    private fun TopAppBar(pagerState: PagerState) {
        val scope = rememberCoroutineScope()

        var canGoNext by remember { mutableStateOf(false) }

        val url by subscriptionSettingsModel.url.observeAsState()
        val requiresAuth by credentialsModel.requiresAuth.observeAsState()
        val username by credentialsModel.username.observeAsState()
        val password by credentialsModel.password.observeAsState()
        val isVerifyingUrl by validationModel.isVerifyingUrl.observeAsState()

        LaunchedEffect(
            // FIXME - Not sure about this approach
            url, requiresAuth, username, password, isVerifyingUrl
        ) {
            if (validationModel.isVerifyingUrl.value == true) {
                canGoNext = false
                return@LaunchedEffect
            }

            val uri = validateUri()

            val authOK = if (requiresAuth == true)
                !username.isNullOrEmpty() && !password.isNullOrEmpty()
            else
                true
            canGoNext = uri != null && authOK
        }

        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        val page = pagerState.currentPage
                        if (page <= 0) {
                            finish()
                        } else scope.launch {
                            pagerState.animateScrollToPage(page - 1)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = null
                    )
                }
            },
            title = {
                Text(stringResource(R.string.activity_add_calendar))
            },
            actions = {
                AnimatedVisibility(visible = canGoNext) {
                    IconButton(
                        onClick = { onNextRequested(pagerState, scope) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }

    private fun onNextRequested(pagerState: PagerState, scope: CoroutineScope) {
        when (pagerState.currentPage) {
            0 -> {
                // flush the credentials if auth toggle is disabled
                if (credentialsModel.requiresAuth.value != true) {
                    credentialsModel.username.value = null
                    credentialsModel.password.value = null
                }

                val uri: Uri? = subscriptionSettingsModel.url.value?.let(Uri::parse)
                val authenticate = credentialsModel.requiresAuth.value ?: false

                if (uri != null) {
                    validationModel.validate(
                        uri,
                        if (authenticate) credentialsModel.username.value else null,
                        if (authenticate) credentialsModel.password.value else null
                    ).invokeOnCompletion {
                        val info = validationModel.result.value
                        if (info?.exception != null) {
                            // There has been an exception, a dialog will be shown
                        } else {
                            subscriptionSettingsModel.url.postValue(
                                info?.uri?.toString()
                            )

                            if (subscriptionSettingsModel.color.value == null)
                                subscriptionSettingsModel.color.postValue(
                                    info?.calendarColor ?: ContextCompat.getColor(
                                        this,
                                        R.color.lightblue
                                    )
                                )

                            if (subscriptionSettingsModel.title.value.isNullOrBlank())
                                subscriptionSettingsModel.title.postValue(
                                    info?.calendarName ?: info?.uri?.toString()
                                )

                            scope.launch {
                                // The url is valid, go to the next slide
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    }
                }
            }

            1 -> {
                // TODO : What to do in the details page
                // Depends on https://github.com/ArnyminerZ/icsx5/issues/34
            }
        }
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
            subscriptionSettingsModel.supportsAuthentication.value = supportsAuthenticate
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
            credentialsModel.isInsecure.value =
                credentialsModel.requiresAuth.value == true && !uri.scheme.equals("https", true)
        } finally {
            subscriptionSettingsModel.urlError.value = errorMsg
        }
        return uri
    }

}
