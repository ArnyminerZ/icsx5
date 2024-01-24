package at.bitfire.icsdroid.ui.dialog

import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun AlertDialogBox(
    message: String,
    confirmButtonText: String,
    confirmButtonCallback: AlertDialogContext.() -> Unit = {},
    dismissButtonText: String,
    dismissButtonCallback: AlertDialogContext.() -> Unit = {},
    onDismissRequest: () -> Unit
) {
    GenericAlertDialog(
        confirmButton = confirmButtonText to confirmButtonCallback,
        dismissButton = dismissButtonText to dismissButtonCallback,
        content = { Text(message) },
        onDismissRequest = onDismissRequest
    )
}
