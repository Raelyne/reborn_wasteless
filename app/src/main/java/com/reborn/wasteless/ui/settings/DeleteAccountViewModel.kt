package com.reborn.wasteless.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.reborn.wasteless.R
import com.reborn.wasteless.data.model.AuthState
import com.reborn.wasteless.repo.AuthRepository
import com.reborn.wasteless.repo.UserRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class DeleteAccountViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // You will need to add "title_delete_account", "edt_current_password", and "CONFIRM_DELETE" to strings.xml
    private val deleteConfirm = "CONFIRM DELETE"

    private val _deleteState = MutableLiveData<AuthState>(AuthState.Idle)
    val deleteState: LiveData<AuthState> = _deleteState

    /**
     * Handles the entire account deletion process.
     * 1. Validates password and confirmation text.
     * 2. Re-authenticates the user (resets the "recent login" window).
     * 3. Calls the repository to delete all data (Storage, Firestore) and the Auth account.
     */
    fun confirmAndDeleteAccount(password: String, confirmText: String) {
        //Check password input
        if (password.isBlank()) {
            _deleteState.value = AuthState.Error(messageId = R.string.error_password_empty)
            return
        }

        //Check confirmation text for safety
        if (confirmText.trim() != deleteConfirm) {
            // Assuming you add an error string for this
            _deleteState.value = AuthState.Error(message = "Please type '$deleteConfirm' to confirm.")
            return
        }

        _deleteState.value = AuthState.Loading

        //1. Re-authenticate the user
        authRepository.reauthenticate(password)
            .addOnSuccessListener {
                //2. Re-authentication succeeded, now proceed with deletion
                performDeletion()
            }
            .addOnFailureListener { exception ->
                // Handle re-authentication failures
                val errorResId = when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> R.string.error_login // Bad password
                    else -> R.string.error_generic_fail
                }
                _deleteState.value = AuthState.Error(messageId = errorResId)
            }
    }

    private fun performDeletion() {
        //3. Call the deleteAccount function you added to UserRepository
        userRepository.deleteAccount()
            .addOnSuccessListener {
                // Account and data successfully deleted
                _deleteState.value = AuthState.Success("Account deleted!")
            }
            .addOnFailureListener {
                // Deletion failed (e.g., Firestore/Storage error).
                // The Auth account might still exist or deletion might have partially failed
                _deleteState.value = AuthState.Error(messageId = R.string.error_generic_fail)
            }
    }

    fun resetState() {
        _deleteState.value = AuthState.Idle
    }
}