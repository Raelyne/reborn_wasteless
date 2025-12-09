package com.reborn.wasteless.data.model

/**
 * Sealed class representing authentication operation states.
 * Used by ViewModels to communicate auth results to Fragments.
 *
 * Example usage:
 * ```kotlin
 * when (val state = viewModel.loginState.value) {
 *     is AuthState.Success -> navigateToHome()
 *     is AuthState.Error -> showError(state.message)
 *     is AuthState.Loading -> showProgressBar()
 *     null -> // Initial state, do nothing
 * }
 * ```
 */
sealed class AuthState {
    /**
     * Authentication operation is in progress.
     */
    object Loading : AuthState()

    /**
     * Authentication succeeded.
     * @param email The authenticated user's email
     */
    data class Success(val email: String) : AuthState()

    /**
     * Authentication failed.
     * @param message Error message to display to user, accept either string or int/id
     */
    data class Error(val message: String? = null, val messageId: Int? = null) : AuthState()

    /**
     * Initial state - no operation has been performed yet.
     */
    object Idle : AuthState()
}

