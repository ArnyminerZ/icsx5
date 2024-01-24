package at.bitfire.icsdroid.ui.dialog

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable

/**
 * Provided by [GenericAlertDialog], gives access to some interactions from inside the composables
 * and callbacks.
 */
interface AlertDialogContext {
    /**
     * Calls the `onDismissRequest` method of the dialog.
     */
    fun dismiss()
}

/**
 * Provides a generic [AlertDialog] with some utilities.
 * @param confirmButton The first argument is the text of the button, the second one the callback.
 * @param dismissButton The first argument is the text of the button, the second one the callback.
 * @param title If any, the title to show in the dialog.
 * @param content Usually a [Text] element, though it can be whatever composable.
 * @param onDismissRequest Requested by the dialog when it should be closed.
 */
@Composable
fun GenericAlertDialog(
    confirmButton: Pair<String, AlertDialogContext.() -> Unit>,
    dismissButton: Pair<String, AlertDialogContext.() -> Unit>? = null,
    title: String? = null,
    content: (@Composable AlertDialogContext.() -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val context = object : AlertDialogContext {
        override fun dismiss() = onDismissRequest()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        // FIXME : I don't love this syntax, but I'm not sure if there's a cleaner way...
        title = title?.let {
            { Text(it) }
        },
        text = content?.let {
            { it(context) }
        },
        dismissButton = dismissButton?.let { (text, onClick) ->
            {
                TextButton(onClick = { onClick(context) }) { Text(text) }
            }
        },
        confirmButton = {
            val (text, onClick) = confirmButton
            TextButton(onClick = { onClick(context) }) { Text(text) }
        }
    )
}
