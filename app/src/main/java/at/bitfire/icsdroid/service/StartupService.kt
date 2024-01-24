/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.service

import androidx.appcompat.app.AppCompatActivity

/**
 * Used for interactions between flavors.
 * All classes that implement this interface will get their [initialize] method called every time
 * the app is launched.
 */
interface StartupService {
    /**
     * Will be called every time the main activity is created.
     * @param activity The calling activity
     */
    fun initialize(activity: AppCompatActivity)
}
