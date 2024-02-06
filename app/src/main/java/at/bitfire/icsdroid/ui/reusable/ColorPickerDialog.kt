/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.reusable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.calendar.LocalCalendar
import com.maxkeppeker.sheets.core.models.base.SelectionButton
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.color.ColorDialog
import com.maxkeppeler.sheets.color.models.ColorConfig
import com.maxkeppeler.sheets.color.models.ColorSelection
import com.maxkeppeler.sheets.color.models.MultipleColors
import com.maxkeppeler.sheets.color.models.SingleColor

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onSelectColor: (color: Int) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    val templateColors = MultipleColors.ColorsInt(
        LocalCalendar.DEFAULT_COLOR,
        Color.Red.toArgb(),
        Color.Green.toArgb(),
        Color.Yellow.toArgb(),
    )

    var selectedColor by remember { mutableIntStateOf(initialColor) }
    ColorDialog(
        state = rememberUseCaseState(visible = true, onCloseRequest = { onDialogDismissed() }),
        selection = ColorSelection(
            selectedColor = SingleColor(selectedColor),
            onSelectColor = onSelectColor,
            extraButton = SelectionButton(stringResource(R.string.add_calendar_default_color)),
            onExtraButtonClick = { selectedColor = LocalCalendar.DEFAULT_COLOR }
        ),
        config = ColorConfig(
            templateColors = templateColors,
            allowCustomColorAlphaValues = false
        ),
    )
}