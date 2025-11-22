package com.example.workapp.data.repository

import com.example.workapp.data.model.Notification
import com.example.workapp.data.model.NotificationType
import com.example.workapp.data.model.NotificationPriority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing in-app notifications and FCM tokens
 * Follows Material 3 notification patterns with priority-based delivery
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    /**
     * Create a new in-app notification
     * This creates a persistent notification in Firestore that users can review later
     */
    suspend fun createNotification(
        userId: String,
        type: NotificationType,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap(),
        actionUrl: String? = null,
        imageUrl: String? = null,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ): Result<String> = try {
        val docRef = firestore.collection("notifications").document()
        val notification = Notification(
            id = docRef.id,
            userId = userId,
            type = type.name,
            title = title,
            message = message,
            data = data,
            actionUrl = actionUrl,
            imageUrl = imageUrl,
            priority = priority.name
        )
        
        docRef.set(notification.toMap()).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Get notifications for a user with real-time updates
     * Returns the most recent 50 notifications ordered by creation time
     */
    fun getNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On error (e.g., permission denied after sign-out), return empty list
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)
                } ?: emptyList()
                
                trySend(notifications)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get unread notification count for badge display
     * Real-time updates for Material 3 BadgedBox component
     */
    fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                
                trySend(snapshot?.size() ?: 0)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Mark a single notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> = try {
        firestore.collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Mark all notifications as read for a user
     * Useful for "Mark all read" action in notification center
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> = try {
        val batch = firestore.batch()
        
        val unreadDocs = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()
        
        unreadDocs.documents.forEach { doc ->
            batch.update(doc.reference, "isRead", true)
        }
        
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Delete old notifications (older than 30 days)
     * Call this periodically to keep notification list manageable
     */
    suspend fun deleteOldNotifications(userId: String, daysOld: Int = 30): Result<Unit> = try {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        
        val oldDocs = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereLessThan("createdAt", cutoffTime)
            .get()
            .await()
        
        val batch = firestore.batch()
        oldDocs.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Update FCM token for the current user
     * Called when a new FCM token is generated
     */
    suspend fun updateFCMToken(token: String): Result<Unit> = try {
        val userId = auth.currentUser?.uid 
            ?: return Result.failure(Exception("User not authenticated"))
        
        firestore.collection("fcm_tokens")
            .document(userId)
            .set(mapOf(
                "token" to token,
                "updatedAt" to System.currentTimeMillis(),
                "platform" to "android"
            ))
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Get FCM token for a user (for server-side notification sending)
     * This would typically be called by a Cloud Function, not the app directly
     */
    suspend fun getFCMToken(userId: String): Result<String> = try {
        val doc = firestore.collection("fcm_tokens")
            .document(userId)
            .get()
            .await()
        
        val token = doc.getString("token") 
            ?: return Result.failure(Exception("No FCM token found for user"))
        
        Result.success(token)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Delete FCM token on sign out
     */
    suspend fun deleteFCMToken(): Result<Unit> = try {
        val userId = auth.currentUser?.uid 
            ?: return Result.failure(Exception("User not authenticated"))
        
        firestore.collection("fcm_tokens")
            .document(userId)
            .delete()
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}