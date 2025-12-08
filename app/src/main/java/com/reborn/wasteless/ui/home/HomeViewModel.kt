package com.reborn.wasteless.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.reborn.wasteless.data.entity.UserEntity
import com.reborn.wasteless.repo.UserRepository
import androidx.lifecycle.map

/**
 * ViewModel for HomeFragment.
 * Manages user greeting and summary data.
 *
 * Memory leak prevention:
 * - Uses LiveData (lifecycle-aware)
 * - Repository handles Firestore listeners properly
 * - No observeForever or direct coroutines without proper cancellation
 */
class HomeViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    /**
     * LiveData for current user's profile.
     * Automatically updates when Firestore document changes.
     * Safe to observe with viewLifecycleOwner in Fragment.
     */
    val currentUser: LiveData<UserEntity> = userRepository.getCurrentUser()

    /**
     * LiveData for greeting text.
     * Formats "Hello, {username}!" from user data.
     * Uses map extension function to derive from currentUser safely.
     */
    val greeting: LiveData<String> = currentUser.map { user ->
        val displayName = if (user.username.isNotEmpty()) {
            user.username
        } else {
            // Fallback: extract from email if username is empty
            userRepository.extractUsernameFromEmail(user.email)
        }
        "Hello, $displayName!"
    }

}
