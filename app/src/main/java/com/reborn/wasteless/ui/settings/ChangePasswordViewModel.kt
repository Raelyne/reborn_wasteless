package com.reborn.wasteless.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.reborn.wasteless.repo.AuthRepository

class ChangePasswordViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> get() = _updateSuccess

    fun changePassword(currentPass: String, newPass: String, confirmPass: String) {
        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            _statusMessage.value = "Please fill in all fields"
            return
        }

        if (newPass != confirmPass) {
            _statusMessage.value = "New passwords do not match"
            return
        }

        if (newPass.length < 6) {
            _statusMessage.value = "Password must be at least 6 characters"
            return
        }

        //Re-auth first cuz of the 5 min token thing that firebase has..
        authRepository.reauthenticate(currentPass)
            .addOnSuccessListener {
                //update Password
                authRepository.updatePassword(newPass)
                    .addOnSuccessListener {
                        _statusMessage.value = "Password updated successfully"
                        _updateSuccess.value = true
                    }
                    .addOnFailureListener { e ->
                        _statusMessage.value = "Failed to update password: ${e.message}"
                    }
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "Incorrect current password"
            }
    }
}