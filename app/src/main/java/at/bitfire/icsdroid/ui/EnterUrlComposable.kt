/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R

@Composable
fun EnterUrlComposable(
    requiresAuth: Boolean,
    onRequiresAuthChange: (Boolean) -> Unit,
    username: String?,
    onUsernameChange: (String) -> Unit,
    password: String?,
    onPasswordChange: (String) -> Unit,
    isInsecure: Boolean,
    url: String,
    onUrlChange: (String) -> Unit,
    urlError: String?,
    supportsAuthentication: Boolean,
    onPickFileRequested: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Instead of adding vertical padding to column, use spacer so that if content is
        // scrolled, it is not spaced
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.add_calendar_url_text),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions { /*TODO*/ },
            maxLines = 1,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.add_calendar_url_sample)) },
            isError = urlError != null
        )
        AnimatedVisibility(visible = urlError != null) {
            Text(
                text = urlError ?: "",
                color = MaterialTheme.colors.error,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.caption
            )
        }

        Text(
            text = stringResource(R.string.add_calendar_pick_file_text),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        TextButton(
            onClick = onPickFileRequested,
            modifier = Modifier.padding(vertical = 15.dp)
        ) {
            Text(stringResource(R.string.add_calendar_pick_file))
        }

        AnimatedVisibility(
            visible = isInsecure,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Rounded.Warning, contentDescription = null)

                Text(
                    text = stringResource(R.string.add_calendar_authentication_without_https_warning),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        AnimatedVisibility(visible = supportsAuthentication) {
            LoginCredentialsComposable(
                requiresAuth,
                username,
                password,
                onRequiresAuthChange,
                onUsernameChange,
                onPasswordChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
