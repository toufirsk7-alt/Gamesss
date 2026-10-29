package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CustomMap
import com.example.model.GarageCar
import com.example.model.ScoreRecord
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java)
            } else {
                null
            }
        }.catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
            throw error
        }.collect { emit(it) }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val payload = profile.copy(userId = uid).toMap().toMutableMap()
            payload["updatedAt"] = FieldValue.serverTimestamp()
            if (profile.createdAt == null) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeGarageCars(): Flow<List<GarageCar>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/garage"
        val collRef = db.collection("users").document(uid).collection("garage")
        collRef.snapshots().map { snapshot ->
            snapshot.toObjects(GarageCar::class.java)
        }.catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
            throw error
        }.collect { emit(it) }
    }

    suspend fun saveGarageCar(car: GarageCar) {
        val uid = requireUserId()
        val docId = if (car.id.isNotBlank()) car.id else "car_${System.currentTimeMillis()}"
        val docRef = db.collection("users").document(uid).collection("garage").document(docId)
        try {
            val payload = car.copy(id = docId, userId = uid).toMap().toMutableMap()
            payload["updatedAt"] = FieldValue.serverTimestamp()
            if (car.createdAt == null) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeCustomMaps(): Flow<List<CustomMap>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/custom_maps"
        val collRef = db.collection("users").document(uid).collection("custom_maps")
        collRef.snapshots().map { snapshot ->
            snapshot.toObjects(CustomMap::class.java)
        }.catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
            throw error
        }.collect { emit(it) }
    }

    suspend fun saveCustomMap(map: CustomMap) {
        val uid = requireUserId()
        val docId = if (map.id.isNotBlank()) map.id else "map_${System.currentTimeMillis()}"
        val docRef = db.collection("users").document(uid).collection("custom_maps").document(docId)
        try {
            val payload = map.copy(id = docId, userId = uid).toMap().toMutableMap()
            payload["updatedAt"] = FieldValue.serverTimestamp()
            if (map.createdAt == null) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCustomMap(mapId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("custom_maps").document(mapId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun submitScore(score: ScoreRecord) {
        val uid = requireUserId()
        val docId = "score_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val docRef = db.collection("scores").document(docId)
        try {
            val payload = score.copy(id = docId, userId = uid).toMap().toMutableMap()
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeLeaderboard(): Flow<List<ScoreRecord>> = flow {
        val path = "scores"
        val collRef = db.collection("scores").orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
        collRef.snapshots().map { snapshot ->
            snapshot.toObjects(ScoreRecord::class.java)
        }.catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
            throw error
        }.collect { emit(it) }
    }
}
