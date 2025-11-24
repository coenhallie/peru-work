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
 * Repository for managing professional data
 */
@Singleton
class ProfessionalRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Get all professionals as a Flow
     */
    fun getAllProfessionals(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .whereIn("roleString", listOf("CRAFTSMAN", "PROFESSIONAL"))
            .orderBy("rating", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On permission errors (like after sign-out), return empty list instead of crashing
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val professionals = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(professionals)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get professionals by profession (supports legacy 'craft' field)
     */
    suspend fun getProfessionalsByProfession(profession: String): Result<List<User>> = try {
        // We need to query both fields or query all and filter
        // Since OR queries are limited, and we want to support both fields,
        // we'll fetch all professionals and filter client-side for simplicity and correctness with mixed data
        val snapshot = firestore.collection("users")
            .whereIn("roleString", listOf("CRAFTSMAN", "PROFESSIONAL"))
            .orderBy("rating", Query.Direction.DESCENDING)
            .get()
            .await()
    
        val professionals = snapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(id = doc.id)
        }.filter { user -> 
            user.currentProfession.equals(profession, ignoreCase = true)
        }
    
        Result.success(professionals)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get a single professional by ID
     */
    suspend fun getProfessionalById(id: String): Result<User> = try {
        val doc = firestore.collection("users")
            .document(id)
            .get()
            .await()
    
        val professional = doc.toObject(User::class.java)
            ?.copy(id = doc.id)
            ?: throw Exception("Professional not found")
    
        Result.success(professional)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Search professionals by name or profession
     */
    suspend fun searchProfessionals(query: String): Result<List<User>> = try {
        val snapshot = firestore.collection("users")
            .whereIn("roleString", listOf("CRAFTSMAN", "PROFESSIONAL"))
            .get()
            .await()
    
        val professionals = snapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(id = doc.id)
        }.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.currentProfession?.contains(query, ignoreCase = true) == true ||
            user.specialties?.any { it.contains(query, ignoreCase = true) } == true
        }
    
        Result.success(professionals)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get unique profession categories
     */
    suspend fun getProfessionCategories(): Result<List<String>> = try {
        val snapshot = firestore.collection("users")
            .whereIn("roleString", listOf("CRAFTSMAN", "PROFESSIONAL"))
            .get()
            .await()

        val categories = snapshot.documents
            .mapNotNull { doc -> 
                val user = doc.toObject(User::class.java)
                user?.currentProfession
            }
            .distinct()
            .sorted()

        Result.success(categories)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
