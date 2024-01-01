package at.bitfire.icsdroid.model

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CredentialsModel : ViewModel() {
    val requiresAuth = MutableLiveData(false)
    val username = MutableLiveData("")
    val password = MutableLiveData("")

    val isInsecure = MutableLiveData(false)

    val isValid = MediatorLiveData<Boolean>().apply {
        fun update() {
            value = if (requiresAuth.value == true)
                !username.value.isNullOrEmpty() && !password.value.isNullOrEmpty()
            else
                true
        }

        addSource(requiresAuth) { update() }
        addSource(username) { update() }
        addSource(password) { update() }
    }
}