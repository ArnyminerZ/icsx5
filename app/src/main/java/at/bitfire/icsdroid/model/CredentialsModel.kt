/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.db.entity.Credential

class CredentialsModel : ViewModel() {
    val requiresAuth = MutableLiveData(false)
    val username = MutableLiveData("")
    val password = MutableLiveData("")

    val isInsecure = MutableLiveData(false)

    fun equalsCredential(credential: Credential) =
        username.value == credential.username
        && password.value == credential.password
}