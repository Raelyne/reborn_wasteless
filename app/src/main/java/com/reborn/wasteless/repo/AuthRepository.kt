package com.reborn.wasteless.repo

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Repository that wraps FirebaseAuth operations.
 *
 * Purpose:
 * - Abstracts Firebase Auth from ViewModels
 * - Centralizes all authentication logic
 * - Follows MVVM architecture pattern
 *
 * Usage:
 * ```kotlin
 * val authRepo = AuthRepository()
 * authRepo.signIn(email, password)
 *     .addOnSuccessListener { result -> ... }
 *     .addOnFailureListener { error -> ... }
 * ```
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Signs in a user with email, user & password
     *
     * @param email User's email address
     * @param password User's password
     * @return Task<AuthResult> that completes when sign-in finishes
     */
    fun signIn(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }

    /**
     * Creates a new user account with email and password.
     *
     * @param email User's email address
     * @param password User's password (must be at least 6 characters)
     * @return Task<AuthResult> that completes when account creation finishes
     */
    fun signUp(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Gets the currently signed-in user.
     *
     * @return FirebaseUser if signed in, null otherwise
     */
    fun getCurrentUser() = auth.currentUser

    /**
     * Checks if a user is currently signed in.
     *
     * @return true if user is signed in, false otherwise
     */
    fun isUserSignedIn(): Boolean = auth.currentUser != null

    /**
     * Updates the current user's display name in Firebase Auth.
     *
     * This sets the displayName property in Firebase Auth, which can be accessed
     * quickly without a Firestore read. Useful for quick access to user's name.
     *
     * @param displayName The display name to set (e.g., "John Doe" or "johndoe")
     * @return Task<Void> that completes when the profile update finishes
     */
    fun updateDisplayName(displayName: String): Task<Void> {
        val user = auth.currentUser
            ?: return Tasks.forException(
                IllegalStateException("No signed-in user")
            )

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        return user.updateProfile(profileUpdates)
    }

    /**
     * Sends a password reset email to the given address.
     * @return Note: Task<Void>, not Task<AuthResult>
     */
    fun sendPasswordResetEmail(email: String): Task<Void> {
        return auth.sendPasswordResetEmail(email)
    }
}


