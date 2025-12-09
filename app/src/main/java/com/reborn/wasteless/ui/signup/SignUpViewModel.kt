package com.reborn.wasteless.ui.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.reborn.wasteless.R
import com.reborn.wasteless.data.model.AuthState
import com.reborn.wasteless.repo.AuthRepository
import com.reborn.wasteless.repo.UserRepository
import com.reborn.wasteless.utils.isValidEmail
import com.reborn.wasteless.utils.isValidPassword
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.FirebaseNetworkException

/**
 * ViewModel for SignUpFragment.
 *
 * Responsibilities:
 * - Validates registration input (email, password)
 * - Handles signup business logic
 * - Creates user document in Firestore after successful signup
 * - Exposes registration state via LiveData
 *
 * Memory leak prevention:
 * - Uses LiveData (lifecycle-aware)
 * - No direct Android dependencies
 * - Repository handles Firebase operations
 *
 * Usage in Fragment:
 * ```kotlin
 * viewModel.register(email, password)
 * viewModel.registerState.observe(viewLifecycleOwner) { state ->
 *     when (state) {
 *         is AuthState.Success -> navigateToLogin()
 *         is AuthState.Error -> showError(state.message)
 *         is AuthState.Loading -> showProgressBar()
 *         AuthState.Idle -> // Do nothing
 *     }
 * }
 * ```
 */
class SignUpViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    /**
     * LiveData representing the current registration state.
     * Fragment should observe this to react to registration results.
     */
    private val _registerState = MutableLiveData<AuthState>(AuthState.Idle)
    val registerState: LiveData<AuthState> = _registerState

    /**
     * Attempts to create a new user account.
     *
     * Flow:
     * 1. Sets state to Loading
     * 2. Validates input (username, email format, password length)
     * 3. Calls repository to create account
     * 4. Creates user document in Firestore with username
     * 5. Updates state to Success or Error
     *
     * @param username User's display name/username
     * @param email User's email address
     * @param password User's password (must be at least 6 characters)
     */
    fun register(username: String, email: String, password: String, confirmPassword: String) {
        //Trims whitespace from the start and end, if any
        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirmPassword = confirmPassword.trim()

        if (trimmedUsername.isBlank()) {
            _registerState.value = AuthState.Error(messageId = R.string.error_username_empty)
            return
        }

        if (trimmedUsername.length < 3) {
            _registerState.value = AuthState.Error(messageId = R.string.error_username_short)
            return
        }

        if (trimmedEmail.isBlank()) {
            _registerState.value = AuthState.Error(messageId = R.string.error_email_empty)
            return
        }

        if (!isValidEmail(trimmedEmail)) {
            _registerState.value = AuthState.Error(messageId = R.string.error_email_invalid)
            return
        }

        if (trimmedPassword.isBlank()) {
            _registerState.value = AuthState.Error(messageId = R.string.error_password_empty)
            return
        }

        if (trimmedPassword.length < 8 || trimmedPassword.length > 32) {
            _registerState.value = AuthState.Error(messageId = R.string.error_password_length)
            return
        }

        if (!isValidPassword(trimmedPassword)) {
            _registerState.value = AuthState.Error(messageId = R.string.error_password_complexity)
            return
        }

        if (trimmedPassword != trimmedConfirmPassword) {
            _registerState.value = AuthState.Error(messageId = R.string.error_password_mismatch)
            return
        }

        // Set loading state
        _registerState.value = AuthState.Loading

        //First we gotta check if the username is unique
        userRepository.isUsernameTaken(trimmedUsername)
            .addOnSuccessListener { exists ->
                if (exists) {
                    _registerState.value = AuthState.Error(messageId = R.string.error_username_taken)
                    return@addOnSuccessListener // @addonSuccessListener is a labeled return to exit only the lambda/if function
                }

                //If the username is available, proceed to registering the user on Firebase
                performSignUp(trimmedUsername, trimmedEmail, trimmedPassword)
            }
            .addOnFailureListener {
                _registerState.value = AuthState.Error(messageId = R.string.error_username_availability)
            }
        }

    /**
     * Registers the user on Firebase Auth, and also store a record of the user based UserEntity params
     *
     * @param username Username
     * @param email Email address
     * @param password Password
     * @return AuthState message based on success/failure
     */
    private fun performSignUp(username: String, email: String, password: String) {
        authRepository.signUp(email,password)
            .addOnSuccessListener { authResult ->
                // Account created successfully
                val userEmail = authResult.user?.email ?: email
                //Fetch uid from firebase
                val uid = authResult.user?.uid

                if (uid != null) {
                    //Define the user entity to store document with
                    val userEntity = com.reborn.wasteless.data.entity.UserEntity(
                        uid = uid,
                        email = userEmail,
                        username = username, // Stored in Firestore
                        createdAt = System.currentTimeMillis()
                    )
                    //Set Firebase Auth displayName (quick access, no DB read needed)
                    authRepository.updateDisplayName(username)
                        .addOnSuccessListener {
                            //Now, set the username
                            userRepository.createOrUpdateUser(userEntity)
                                .addOnSuccessListener {
                                    _registerState.value = AuthState.Success(userEmail)
                                }
                                .addOnFailureListener { exception ->
                                    // Account created and displayName set, but Firestore update failed
                                    // displayName is still available via Firebase Auth
                                    _registerState.value = AuthState.Success(userEmail)
                                }
                        }
                        .addOnFailureListener { exception ->
                            //In the event Display Name upd fails straight away, store firebase document
                            //cos it won't create a docs if it fails
                            userRepository.createOrUpdateUser(userEntity)
                                .addOnSuccessListener {
                                    _registerState.value = AuthState.Success(userEmail)
                                }
                                .addOnFailureListener {
                                    // Both failed, but account exists - still success
                                    _registerState.value = AuthState.Success(userEmail)
                                }
                        }
                } else {
                        _registerState.value = AuthState.Success(userEmail)
                }
            }
            .addOnFailureListener { exception ->
                // Registration failed, one of these reasons
                // Basically apparently, Firebase sends error messages by default when you fail a "createUserWithEmailAndPassword()"
                // So rn, we check if any of the exceptions return true
                val errorResId = when (exception) {
                    is FirebaseAuthUserCollisionException -> R.string.error_email_already_exists
                    is FirebaseAuthWeakPasswordException -> R.string.error_password_weak
                    is FirebaseAuthInvalidCredentialsException -> R.string.error_email_malformed
                    is FirebaseNetworkException -> R.string.error_network
                    else -> null
                }

                //Then, we update state based on what we found
                if (errorResId != null) {
                    // A specific known error was found, so use the ID
                    _registerState.value = AuthState.Error(messageId = errorResId)
                } else {
                    // else it's probably a random error (server down, etc), show the raw message or a fallback
                    _registerState.value = AuthState.Error(messageId = R.string.error_registration)
                }
            }
    }

    /**
     * Resets the registration state to Idle.
     * Call this when navigating away or clearing the form.
     */
    fun resetState() {
        _registerState.value = AuthState.Idle
    }
}

