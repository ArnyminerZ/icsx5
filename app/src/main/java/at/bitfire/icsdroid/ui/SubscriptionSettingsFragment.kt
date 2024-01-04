/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.reusable.SwitchSetting
import at.bitfire.icsdroid.databinding.SubscriptionSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import org.joda.time.Minutes
import org.joda.time.format.PeriodFormat
import java.net.URISyntaxException

class SubscriptionSettingsFragment : Fragment() {

    private val model by activityViewModels<SubscriptionSettingsModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val colorPickerContract = registerForActivityResult(ColorPickerActivity.Contract()) { color ->
            model.color.value = color
        }
        return ComposeView(requireActivity()).apply {
            setContent {
                val url by model.url.observeAsState("")
                val title by model.title.observeAsState("")
                val color by model.color.observeAsState(0)
                val ignoreAlerts by model.ignoreAlerts.observeAsState(false)
                val defaultAlarmMinutes by model.defaultAlarmMinutes.observeAsState()
                val defaultAllDayAlarmMinutes by model.defaultAllDayAlarmMinutes.observeAsState()
                SubscriptionSettingsComposable(
                    url = url,
                    title = title,
                    titleChanged = { model.title.postValue(it) },
                    color = color,
                    colorIconClicked = { colorPickerContract.launch(color) },
                    ignoreAlerts = ignoreAlerts,
                    ignoreAlertsChanged = { model.ignoreAlerts.postValue(it) },
                    defaultAlarmMinutes = defaultAlarmMinutes,
                    defaultAlarmMinutesChanged = { model.defaultAlarmMinutes.postValue(it.toLongOrNull()) },
                    defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                    defaultAllDayAlarmMinutesChanged = { model.defaultAllDayAlarmMinutes.postValue(it.toLongOrNull()) }
                )
            }
        }
    }

    class SubscriptionSettingsModel : ViewModel() {
        val url = MutableLiveData<String>()

        val urlError = MutableLiveData<String?>()

        var originalTitle: String? = null
        val title = MutableLiveData<String>()
        val color = MutableLiveData<Int>()
        val ignoreAlerts = MutableLiveData<Boolean>()
        val defaultAlarmMinutes = MutableLiveData<Long>()
        val defaultAllDayAlarmMinutes = MutableLiveData<Long>()

        val supportsAuthentication = MediatorLiveData(false).apply {
            addSource(url) {
                val uri = try {
                    Uri.parse(it)
                } catch (e: URISyntaxException) {
                    return@addSource
                }
                value = HttpUtils.supportsAuthentication(uri)
            }
        }

        /**
         * If [uri] has either `webcal` or `webcals` as scheme, the value of [url] is replaced with
         * the corresponding http* scheme (`http` and `https` respectively).
         *
         * @return The adjusted uri.
         */
        fun replaceUrlScheme(uri: Uri): Uri {
            return if (uri.scheme.equals("webcal", true)) {
                uri.buildUpon().scheme("http").build().also { url.value = it.toString() }
            } else if (uri.scheme.equals("webcals", true)) {
                uri.buildUpon().scheme("https").build().also { url.value = it.toString() }
            } else {
                uri
            }
        }
    }
}


@Composable
private fun SubscriptionSettingsComposable(
    url: String,
    title: String,
    titleChanged: (String) -> Unit,
    color: Int,
    colorIconClicked: () -> Unit,
    ignoreAlerts: Boolean,
    ignoreAlertsChanged: (Boolean) -> Unit,
    defaultAlarmMinutes: Long?,
    defaultAlarmMinutesChanged: (String) -> Unit,
    defaultAllDayAlarmMinutes: Long?,
    defaultAllDayAlarmMinutesChanged: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
    ) {

        // Title
        Text(
            text = stringResource(R.string.add_calendar_title),
            style = MaterialTheme.typography.h5,
        )

        // Name and color card
        Card (
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.weight(5f)
                ) {
                    Text(
                        text = url,
                        color = Color.Gray,
                        style = MaterialTheme.typography.body2,
                    )
                    TextField(
                        value = title,
                        onValueChange = titleChanged,
                        label = { Text(stringResource(R.string.add_calendar_title_hint)) },
                        singleLine = true,
                    )
                }
                IconButton(
                    onClick = colorIconClicked,
                    modifier = Modifier
                        .weight(1f)
                        .size(48.dp)
                        .padding(start = 8.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Circle,
                        contentDescription = stringResource(R.string.add_calendar_pick_color),
                        tint = Color(color),
                        modifier = Modifier
                            .size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(12.dp))

        // Alarms
        Text(
            text = stringResource(R.string.add_calendar_alarms_title),
            style = MaterialTheme.typography.h5,
        )

        // Ignore existing alarms
        SwitchSetting(
            title = stringResource(R.string.add_calendar_alarms_ignore_title),
            description = stringResource(R.string.add_calendar_alarms_ignore_description),
            checked = ignoreAlerts,
            onCheckedChange = ignoreAlertsChanged
        )

        Spacer(modifier = Modifier.padding(12.dp))

        // Default Alarm
        Text(
            text = stringResource(R.string.default_alarm_dialog_title),
            style = MaterialTheme.typography.body1,
        )
        Text(
            text = stringResource(R.string.default_alarm_dialog_message),
            color = Color.Gray,
            style = MaterialTheme.typography.body2,
        )
        OutlinedTextField(
            value = (defaultAlarmMinutes ?: "").toString(),
            onValueChange = defaultAlarmMinutesChanged,
            label = { Text(stringResource(R.string.default_alarm_dialog_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.padding(12.dp))

        // Default Alarm (All Day Events)
        Text(
            text = stringResource(R.string.add_calendar_alarms_default_all_day_title),
            style = MaterialTheme.typography.body1,
        )
        Text(
            text = stringResource(R.string.default_alarm_dialog_message),
            color = Color.Gray,
            style = MaterialTheme.typography.body2,
        )
        OutlinedTextField(
            value = (defaultAllDayAlarmMinutes ?: "").toString(),
            onValueChange = defaultAllDayAlarmMinutesChanged,
            label = { Text(stringResource(R.string.default_alarm_dialog_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.padding(12.dp))
    }
}
