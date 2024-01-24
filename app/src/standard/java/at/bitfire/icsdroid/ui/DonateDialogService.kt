/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.service.ComposableStartupService

class DonateDialogService: ComposableStartupService {
    companion object {
        const val PREF_NEXT_REMINDER = "nextDonationReminder"

        const val DONATION_URI = "https://icsx5.bitfire.at/donate/?pk_campaign=icsx5-app"

        /**
         * The amount of milliseconds in a day.
         */
        private const val ONE_DAY_MILLIS = 1000L * 60 * 60 * 24

        /**
         * When the donate button is clicked, the donation link will be opened. The dialog will be
         * shown again after this amount of ms.
         *
         * Default: 60 days
         */
        const val SHOW_EVERY_MILLIS_DONATE = ONE_DAY_MILLIS * 60

        /**
         * When the dismiss button is clicked, the donation dialog will be dismissed. It will be
         * shown again after this amount of ms.
         *
         * Default: 14 days
         */
        const val SHOW_EVERY_MILLIS_DISMISS = ONE_DAY_MILLIS * 14
    }

    private lateinit var preferences: SharedPreferences

    override fun initialize(activity: AppCompatActivity) {
        preferences = activity.getPreferences(0)
    }

    /**
     * Observes the value of the preference with key [PREF_NEXT_REMINDER], and sets the value:
     * ```kotlin
     * value < System.currentTimeMillis()
     * ```
     */
    @Composable
    override fun shouldShow(): LiveData<Boolean> = remember { MutableLiveData(false) }.also {
        DisposableEffect(it) {
            val listener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
                // Just receive updates to PREF_NEXT_REMINDER
                if (key != PREF_NEXT_REMINDER) return@OnSharedPreferenceChangeListener
                // Get the value of the preference
                val value = sharedPreferences.getLong(PREF_NEXT_REMINDER, 0)
                it.postValue(value < System.currentTimeMillis())
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            listener.onSharedPreferenceChanged(preferences, PREF_NEXT_REMINDER)

            onDispose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }

    /**
     * Dismisses the dialog for the given amount of milliseconds by updating the preference.
     */
    private fun dismissDialogForMillis(millis: Long) {
        preferences
            .edit()
            .putLong(PREF_NEXT_REMINDER, System.currentTimeMillis() + millis)
            .apply()
    }

    @Composable
    override fun Content() {
        val uriHandler = LocalUriHandler.current

        // TODO: Replace with AlertDialogBox when https://github.com/ArnyminerZ/icsx5/pull/67 is merged
        AlertDialog(
            onDismissRequest = { /* Cannot be dismissed */ },
            title = { Text(stringResource(R.string.donate_title)) },
            text = { Text(stringResource(R.string.donate_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dismissDialogForMillis(SHOW_EVERY_MILLIS_DONATE)
                        uriHandler.openUri(DONATION_URI)
                    }
                ) { Text(stringResource(R.string.donate_now).uppercase()) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dismissDialogForMillis(SHOW_EVERY_MILLIS_DISMISS)
                    }
                ) { Text(stringResource(R.string.donate_later).uppercase()) }
            }
        )
    }
}
