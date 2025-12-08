package com.reborn.wasteless.repo

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.reborn.wasteless.data.entity.FoodLogEntity
import com.reborn.wasteless.data.WasteType
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

class LogRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    // Firebase Storage root reference
    private val storageRef = Firebase.storage.reference


    /**
     * If Storage Bucket is used--
     * */
    fun uploadImage(uri: Uri): Task<String> {
        val uid = auth.currentUser?.uid
        val key = UUID.randomUUID().toString()
        val imgRef = storageRef.child("images/$uid/logs/$key.jpg")

        return imgRef
            .putFile(uri)                                   // Task<UploadTask.TaskSnapshot>
            .continueWithTask { uploadTask ->
                // Propagate upload exceptions
                if (!uploadTask.isSuccessful) {
                    throw uploadTask.exception!!
                }
                // Once uploaded, fetch download URL
                imgRef.downloadUrl                          // Task<Uri>
            }
            .continueWith { urlTask ->
                // Convert Uri → String
                urlTask.result.toString()                   // returns String
            }
    }

    /**
     * Saves a [com.example.wasteless.data.FoodLogEntity] to Firestore.
     * Returns Task<Void> when Firestore write completes.
     */
    fun saveLog(
        entity: FoodLogEntity
    ): Task<Void> {
        val uid = auth.currentUser?.uid
            ?: return Tasks.forException(IllegalStateException("No signed-in user"))

        val logsRef = firestore
            .collection("users")
            .document(uid)
            .collection("logs")

        val docId = entity.id.ifBlank { entity.date.toString() }
        val docRef = logsRef.document(docId)
        val toSave = entity.copy(id = docRef.id)

        return docRef.set(toSave)
    }

    fun getAllSession(): LiveData<List<FoodLogEntity>> {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user")
        val live = MutableLiveData<List<FoodLogEntity>>()

        val ref = firestore
            .collection("users")
            .document(uid)
            .collection("logs")
            .orderBy("date", Query.Direction.DESCENDING)

        ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val logs = snapshot
                ?.documents
                ?.mapNotNull { it.toObject(FoodLogEntity::class.java) }
                ?: emptyList()
            live.postValue(logs)
        }
        return live
    }

    // For future history/statistics screens IF I decide to implement by multi waste type per log..
    fun getTotalWeight(type: WasteType): LiveData<Double> {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user")

        val logsRef = firestore
            .collection("users")
            .document(uid)
            .collection("logs")

        val live = MutableLiveData<Double>()
        logsRef
            .whereEqualTo("wasteType", type.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val sum = snapshot
                    ?.documents
                    ?.mapNotNull { it.getDouble("totalWeight") }
                    ?.sum() ?: 0.0
                live.postValue(sum)
            }
        return live
    }
}