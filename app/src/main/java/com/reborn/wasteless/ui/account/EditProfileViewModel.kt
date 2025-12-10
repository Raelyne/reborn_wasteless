package com.reborn.wasteless.ui.account

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.reborn.wasteless.data.entity.UserEntity
import com.reborn.wasteless.repo.UserRepository

class EditProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    val currentUser: LiveData<UserEntity> = userRepository.getCurrentUser()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> get() = _updateSuccess

    fun updateProfile(newUsername: String, imageUri: Uri?) {
        _isLoading.value = true

        // Helper function to proceed with Firestore update after image upload (if any)
        fun proceedToUpdateFirestore(finalPhotoUrl: String?) {
            // Check username uniqueness if it changed
            val currentName = currentUser.value?.username

            if (newUsername != currentName) {
                userRepository.isUsernameTaken(newUsername).addOnSuccessListener { isTaken ->
                    if (isTaken) {
                        _statusMessage.value = "Username already taken"
                        _isLoading.value = false
                    } else {
                        performFirestoreUpdate(newUsername, finalPhotoUrl)
                    }
                }
            } else {
                performFirestoreUpdate(null, finalPhotoUrl) // Null username means no change
            }
        }

        //Uploads da image, but does check if a image uri exists
        if (imageUri != null) {
            userRepository.uploadProfilePicture(imageUri)
                .addOnSuccessListener { uri ->
                    proceedToUpdateFirestore(uri.toString())
                }
                .addOnFailureListener {
                    _statusMessage.value = "Failed to upload image"
                    _isLoading.value = false
                }
        } else {
            proceedToUpdateFirestore(null)
        }
    }

    private fun performFirestoreUpdate(newUsername: String?, newPhotoUrl: String?) {
        userRepository.updateUserProfile(newUsername, newPhotoUrl)
            .addOnSuccessListener {
                _statusMessage.value = "Profile updated!"
                _updateSuccess.value = true
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "Error: ${e.message}"
                _isLoading.value = false
            }
    }
}