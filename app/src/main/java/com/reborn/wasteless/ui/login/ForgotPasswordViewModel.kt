package com.reborn.wasteless.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.reborn.wasteless.R
import com.reborn.wasteless.data.model.AuthState
import com.reborn.wasteless.repo.AuthRepository
import com.reborn.wasteless.utils.isValidEmail
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseNetworkException

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    /**
     * LiveData representing the current state of resetting password
     * Fragment observes er to check the status of forgetting password lol
     */
    private val _resetPasswordState = MutableLiveData<AuthState>(AuthState.Idle)
    val resetPasswordState: LiveData<AuthState> = _resetPasswordState

    /**
     * ResetPassword function, similar to how the SignUp thing works but it only checks if the email is valid
     * before sending
     */
    fun resetPassword(email: String) {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            _resetPasswordState.value = AuthState.Error(messageId = R.string.error_email_empty)
            return
        }

        if (!isValidEmail(trimmedEmail)) {
            _resetPasswordState.value = AuthState.Error(messageId = R.string.error_email_invalid)
            return
        }

        _resetPasswordState.value = AuthState.Loading

        //Send the email
        authRepository.sendPasswordResetEmail(trimmedEmail)
            .addOnSuccessListener {
                // Task finished successfully -> Tell the UI
                _resetPasswordState.value = AuthState.Success(trimmedEmail)
            }
            .addOnFailureListener { exception ->
                // Task failed -> Tell the UI why
                when (exception) {
                    is FirebaseAuthInvalidUserException -> _resetPasswordState.value =
                        AuthState.Success(trimmedEmail) //its not valid for people to know if theres a user so we lie

                    is FirebaseNetworkException -> { _resetPasswordState.value =
                        AuthState.Error(messageId = R.string.error_network)
                    }
                    else -> { _resetPasswordState.value =
                        AuthState.Error(messageId = R.string.error_generic_fail)
                }
                }

            }
    }
        /**
         * Honestly- I had to search this up :skull: but this is
         * a pattern to check if it's a valid email
         * the [A-Za-z0-9+_.-]+ basically describes the list of allowed characters before @
         * "A-Za-z" is any uppercase/lowercase letter (A-Z & a-z)
         * "0-9" is digits
         * "+_.-" is just these 4 symbols being allowed as well
         * And the + outside means "one or more" of these characters
         *
         * @param email Email address to validate
         * @return true if email format is valid, false otherwise
         */
        private fun isValidEmail(email: String): Boolean {
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
            return email.matches(emailRegex.toRegex())
        }

        fun resetState() {
            _resetPasswordState.value = AuthState.Idle
        }
    }