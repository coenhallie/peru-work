package com.example.workapp.data.repository

import com.example.workapp.data.model.User
import com.example.workapp.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing craftsmen data
 */
@Singleton
class CraftsmenRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Get all craftsmen as a Flow
     */
    fun getAllCraftsmen(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .whereEqualTo("roleString", UserRole.CRAFTSMAN.name)
            .orderBy("rating", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On permission errors (like after sign-out), return empty list instead of crashing
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val craftsmen = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(craftsmen)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get craftsmen by craft type
     */
    suspend fun getCraftsmenByCraft(craft: String): Result<List<User>> = try {
        val snapshot = firestore.collection("users")
            .whereEqualTo("roleString", UserRole.CRAFTSMAN.name)
            .whereEqualTo("craft", craft)
            .orderBy("rating", Query.Direction.DESCENDING)
            .get()
            .await()
    
        val craftsmen = snapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(id = doc.id)
        }
    
        Result.success(craftsmen)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get a single craftsman by ID
     */
    suspend fun getCraftsmanById(id: String): Result<User> = try {
        val doc = firestore.collection("users")
            .document(id)
            .get()
            .await()
    
        val craftsman = doc.toObject(User::class.java)
            ?.copy(id = doc.id)
            ?: throw Exception("Craftsman not found")
    
        Result.success(craftsman)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Search craftsmen by name or craft
     */
    suspend fun searchCraftsmen(query: String): Result<List<User>> = try {
        val snapshot = firestore.collection("users")
            .whereEqualTo("roleString", UserRole.CRAFTSMAN.name)
            .get()
            .await()
    
        val craftsmen = snapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(id = doc.id)
        }.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.craft?.contains(query, ignoreCase = true) == true ||
            user.specialties?.any { it.contains(query, ignoreCase = true) } == true
        }
    
        Result.success(craftsmen)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get unique craft categories
     */
    suspend fun getCraftCategories(): Result<List<String>> = try {
        val snapshot = firestore.collection("users")
            .whereEqualTo("roleString", UserRole.CRAFTSMAN.name)
            .get()
            .await()

        val categories = snapshot.documents
            .mapNotNull { it.getString("craft") }
            .distinct()
            .sorted()

        Result.success(categories)
    } catch (e: Exception) {
        Result.failure(e)
    }
}