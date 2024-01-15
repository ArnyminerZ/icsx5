package at.bitfire.icsdroid.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.ComposeView

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = colorPrimary,
            primaryVariant = colorPrimaryDark,
            secondary = colorSecondary
        ),
        content = content
    )
}

/**
 * Composes the given composable into the given activity. The content will become the root view of
 * the given activity.
 * This is roughly equivalent to calling [ComponentActivity.setContentView] with a ComposeView i.e.:
 * ```kotlin
 * setContentView(
 *   ComposeView(this).apply {
 *     setContent {
 *       MyComposableContent()
 *     }
 *   }
 * )
 * ```
 *
 * Then, applies [AppTheme] to the UI.
 *
 * @param parent The parent composition reference to coordinate scheduling of composition updates
 * @param content A `@Composable` function declaring the UI contents
 */
fun ComponentActivity.setContentThemed(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit
) {
    setContent(parent) {
        AppTheme {
            content()
        }
    }
}

/**
 * Set the Jetpack Compose UI content for this view. Initial composition will occur when the view
 * becomes attached to a window or when `createComposition` is called, whichever comes first.
 *
 * Then, applies [AppTheme] to the UI.
 *
 * @param content A `@Composable` function declaring the UI contents
 */
fun ComposeView.setContentThemed(content: @Composable () -> Unit) {
    setContent {
        AppTheme {
            content()
        }
    }
}
