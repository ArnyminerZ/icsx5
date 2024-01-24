/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.service

import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData

/**
 * Provides the possibility to display some composable (intended for dialogs) if a given condition
 * is met.
 */
interface ComposableStartupService: StartupService {
    /**
     * Provides a stateful response to whether this composable should be shown or not.
     * @return A [LiveData] that can be observed, and will make [Content] visible when `true`.
     */
    @Composable
    fun shouldShow(): LiveData<Boolean>

    /**
     * The content to display. It's not constrained, will be rendered together with the main UI.
     * Usually an `AlertDialog`.
     */
    @Composable
    fun Content()
}
