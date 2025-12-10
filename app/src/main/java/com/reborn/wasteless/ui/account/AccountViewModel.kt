package com.reborn.wasteless.ui.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.reborn.wasteless.data.entity.UserEntity
import com.reborn.wasteless.repo.AuthRepository
import com.reborn.wasteless.repo.UserRepository
import java.text.SimpleDateFormat
import kotlin.text.ifEmpty
import java.util.Date
import java.util.Locale

class AccountViewModel (
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    /**
     * Add a logged out state to allow the ui to observe state changes
     */
    private val _loggedOut = MutableLiveData<Boolean>()
    val loggedOut: LiveData<Boolean> get() = _loggedOut

    fun logOut() {
        authRepository.signOut()
        _loggedOut.value = true
    }

    /**
     * LiveData for current user's profile.
     * Automatically updates when Firestore document changes.
     * Safe to observe with viewLifecycleOwner in Fragment.
     */
    val currentUser: LiveData<UserEntity> = userRepository.getCurrentUser()

    val usernameTag: LiveData<String> = currentUser.map { user ->
        val displayName = user.username.ifEmpty {
            userRepository.extractUsernameFromEmail(user.email)
        }
        "Hello, $displayName!"
    }

    val dateOfCreation: LiveData<String> = currentUser.map { user ->
        val date = Date(user.createdAt)
        val format = SimpleDateFormat("MMM yyyy", Locale.getDefault()) // e.g., "Dec 2025"
        "Since: ${format.format(date)}"
    }
}