package at.bitfire.icsdroid.ui.theme

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import at.bitfire.icsdroid.Settings

private val LightColors = lightColors(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface
)

private val DarkColors = darkColors(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // TODO: Dark theme

    val context = LocalContext.current

    val colors = if (darkTheme)
        DarkColors
    else
        LightColors

    MaterialTheme(
        colors = colors
    ) {
        LaunchedEffect(darkTheme) {
            (context as? AppCompatActivity)?.let { activity ->
                val style = if (darkTheme)
                    SystemBarStyle.dark(
                        md_theme_dark_primaryContainer.toArgb()
                    )
                else
                    SystemBarStyle.light(
                        md_theme_light_surfaceTint.toArgb(),
                        md_theme_light_onBackground.toArgb()
                    )
                activity.enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
            } ?: Log.e("AppTheme", "Context is not activity!")
        }

        Box(
            modifier = Modifier
                // Required to make sure all paddings are correctly set
                .safeDrawingPadding()
                .fillMaxSize()
        ) {
            content()
        }
    }
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
 * @param darkTheme Calculates whether the UI should be shown in light or dark theme.
 * @param content A `@Composable` function declaring the UI contents
 */
fun ComponentActivity.setContentThemed(
    parent: CompositionContext? = null,
    darkTheme: @Composable () -> Boolean = {
        val forceDarkTheme by Settings(this).forceDarkModeLive().observeAsState()
        forceDarkTheme == true || isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    setContent(parent) {
        AppTheme(darkTheme = darkTheme()) {
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
@Deprecated("Fragments should not be used")
fun ComposeView.setContentThemed(content: @Composable () -> Unit) {
    setContent {
        AppTheme {
            content()
        }
    }
}
