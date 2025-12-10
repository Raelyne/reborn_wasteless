package com.reborn.wasteless.data.entity

// Data model for Firestore user document
data class UserEntity(
    val uid: String = "",
    val email: String = "",
    val username: String = "",  // Display name or username
    val profilePictureUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
