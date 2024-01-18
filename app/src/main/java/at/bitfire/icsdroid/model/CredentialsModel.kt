package at.bitfire.icsdroid.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CredentialsModel : ViewModel() {
    val requiresAuth = MutableLiveData(false)
    val username = MutableLiveData("")
    val password = MutableLiveData("")

    val isInsecure = MutableLiveData(false)

    // TODO: We still need the dirty model mechanism find a nice solution
    fun dirty(): Boolean = false
}