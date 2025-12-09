package com.reborn.wasteless.repo

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import java.lang.IllegalStateException

//Essential Functions for manipulating currently logged in user's user Data
class UserDataRepository {

    private val firestore = Firebase.firestore

    //update Username
    fun updateUsername(uid: String, username: String): Task<Void> {
        val userDocument = firestore.collection("users").document(uid)
        val userData = hashMapOf(
            "username" to username,
            "lastUpdatedAt" to FieldValue.serverTimestamp()
        )
        return userDocument.update(userData as Map<String, Any>)
    }

    //Delete Account
    fun deleteAccount(currentPassword: String): Task<Void> {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return Tasks.forException(IllegalStateException("User not logged in"))
        val email = user.email ?: return Tasks.forException(IllegalStateException("User has no email"))

        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        return user.reauthenticate(credential).continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            // Re-authentication successful, now delete Firestore data
            firestore.collection("users").document(user.uid).delete()
        }.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            // Firestore data deleted, now delete the user account
            user.delete()
        }
    }

}
