package com.example.workapp.data.model

import com.google.firebase.firestore.PropertyName

/**
 * In-app notification model for user activity tracking
 * Supports Material 3 notification patterns with priority and rich content
 */
data class Notification(
    @get:PropertyName("_id")
    @set:PropertyName("_id")
    var id: String = "",
    
    val userId: String = "",              // Recipient user ID
    val type: String = NotificationType.APPLICATION_RECEIVED.name,
    val title: String = "",
    val message: String = "",
    val data: Map<String, String> = emptyMap(), // Contextual data (jobId, applicationId, etc.)
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val actionUrl: String? = null,        // Deep link for navigation (e.g., "applications/jobId")
    val imageUrl: String? = null,         // Optional image (e.g., craftsman profile)
    val priority: String = NotificationPriority.NORMAL.name
) {
    // Computed properties for type-safe access
    val notificationType: NotificationType
        get() = try {
            NotificationType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            NotificationType.APPLICATION_RECEIVED
        }
    
    val notificationPriority: NotificationPriority
        get() = try {
            NotificationPriority.valueOf(priority)
        } catch (e: IllegalArgumentException) {
            NotificationPriority.NORMAL
        }
    
    fun toMap(): Map<String, Any?> = buildMap {
        put("_id", id)
        put("userId", userId)
        put("type", type)
        put("title", title)
        put("message", message)
        put("data", data)
        put("isRead", isRead)
        put("createdAt", createdAt)
        put("actionUrl", actionUrl)
        put("imageUrl", imageUrl)
        put("priority", priority)
    }
}

/**
 * Notification type enumeration for different notification categories
 */
enum class NotificationType {
    // Application lifecycle notifications
    APPLICATION_RECEIVED,        // Client: New application received for their job
    APPLICATION_ACCEPTED,        // Professional: Their application was accepted
    APPLICATION_REJECTED,        // Professional: Their application was rejected
    APPLICATION_WITHDRAWN,       // Client: Professional withdrew their application
    
    // Job lifecycle notifications 
    JOB_STARTED,                // Both: Job work has officially started
    JOB_COMPLETED,              // Both: Job marked as complete
    JOB_CANCELLED,              // Both: Job was cancelled
    
    // Messaging notifications
    NEW_MESSAGE,                // Both: New chat message received
    
    // System notifications
    PROFILE_VIEWED,             // Professional: Client viewed their profile
    REVIEW_RECEIVED             // Professional: New review posted on their profile
}

/**
 * Notification priority levels for different urgency levels
 * Used for notification channel importance and display prominence
 */
enum class NotificationPriority {
    LOW,        // Background information, minimal interruption
    NORMAL,     // Standard notifications, default behavior
    HIGH,       // Important updates, show immediately with sound/vibration
    URGENT      // Critical actions required, persistent notification
}