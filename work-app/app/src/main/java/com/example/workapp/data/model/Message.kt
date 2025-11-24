package com.example.workapp.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Message model for direct communication between client and craftsman
 * Supports text messages and system-generated messages
 */
data class Message(
    @get:PropertyName("_id")
    @set:PropertyName("_id")
    var id: String = "",
    
    val chatRoomId: String = "",          // Format: "job_{jobId}"
    val senderId: String = "",            // User ID of sender
    val senderName: String = "",          // Display name of sender
    val senderRole: String = "CLIENT",    // CLIENT or PROFESSIONAL
    val message: String = "",             // Message content
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = MessageType.TEXT.name,
    val attachmentUrl: String? = null,    // Optional image/file attachment URL
    val metadata: Map<String, String>? = null // Additional contextual data
) {
    // Computed property for type-safe message type access
    val messageType: MessageType
        get() = try {
            MessageType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            MessageType.TEXT
        }
    
    fun toMap(): Map<String, Any?> = buildMap {
        put("_id", id)
        put("chatRoomId", chatRoomId)
        put("senderId", senderId)
        put("senderName", senderName)
        put("senderRole", senderRole)
        put("message", message)
        put("timestamp", timestamp)
        put("isRead", isRead)
        put("type", type)
        put("attachmentUrl", attachmentUrl)
        put("metadata", metadata)
    }
}

/**
 * Message type enumeration
 */
enum class MessageType {
    TEXT,       // Regular text message
    IMAGE,      // Image attachment
    SYSTEM      // Automated system messages (e.g., "Application accepted")
}

/**
 * Chat room model representing a conversation between client and craftsman
 * One chat room per job assignment
 */
data class ChatRoom(
    @get:PropertyName("_id")
    @set:PropertyName("_id")
    var id: String = "",                  // Format: "job_{jobId}"
    
    val jobId: String = "",               // Associated job ID
    val jobTitle: String = "",            // Job title for display
    val clientId: String = "",            // Client user ID
    val clientName: String = "",          // Client display name
    val clientProfileImage: String? = null,
    val craftsmanId: String = "",         // Legacy: Craftsman user ID
    val craftsmanName: String = "",       // Legacy: Craftsman display name
    val craftsmanProfileImage: String? = null,
    val professionalId: String = "",      // Professional user ID
    val professionalName: String = "",    // Professional display name
    val professionalProfileImage: String? = null,
    val lastMessage: String? = null,      // Most recent message text
    val lastMessageTime: Long? = null,    // Timestamp of last message
    val unreadCountClient: Int = 0,       // Unread count for client
    val unreadCountCraftsman: Int = 0,    // Legacy: Unread count for craftsman
    val unreadCountProfessional: Int = 0, // Unread count for professional
    val isActive: Boolean = true,         // Chat is active (job not completed/cancelled)
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("_id", id)
        put("jobId", jobId)
        put("jobTitle", jobTitle)
        put("clientId", clientId)
        put("clientName", clientName)
        put("clientProfileImage", clientProfileImage)
        // put("craftsmanId", craftsmanId)
        // put("craftsmanName", craftsmanName)
        // put("craftsmanProfileImage", craftsmanProfileImage)
        put("professionalId", professionalId)
        put("professionalName", professionalName)
        put("professionalProfileImage", professionalProfileImage)
        put("lastMessage", lastMessage)
        put("lastMessageTime", lastMessageTime)
        put("unreadCountClient", unreadCountClient)
        put("unreadCountCraftsman", unreadCountCraftsman)
        put("unreadCountProfessional", unreadCountProfessional)
        put("isActive", isActive)
        put("createdAt", createdAt)
    }
}