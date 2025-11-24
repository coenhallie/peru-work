package com.example.workapp.data.repository

import com.example.workapp.data.model.ChatRoom
import com.example.workapp.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing chat data
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Get all chat rooms for a specific user (either as client or professional)
     */
    fun getChatRoomsForUser(userId: String): Flow<List<ChatRoom>> = callbackFlow {
        val clientQuery = firestore.collection("chat_rooms")
            .whereEqualTo("clientId", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            
        // Query for professionalId (new) and craftsmanId (legacy)
        val professionalQuery = firestore.collection("chat_rooms")
            .whereEqualTo("professionalId", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        val craftsmanQuery = firestore.collection("chat_rooms")
            .whereEqualTo("craftsmanId", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        val roomsMap = mutableMapOf<String, ChatRoom>()
        
        val clientRegistration = clientQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Ignore error for now, or handle appropriately
                return@addSnapshotListener
            }
            
            snapshot?.documents?.forEach { doc ->
                doc.toObject(ChatRoom::class.java)?.let { room ->
                    roomsMap[room.id] = room
                }
            }
            trySend(roomsMap.values.sortedByDescending { it.lastMessageTime })
        }
        
        val professionalRegistration = professionalQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            
            snapshot?.documents?.forEach { doc ->
                doc.toObject(ChatRoom::class.java)?.let { room ->
                    roomsMap[room.id] = room
                }
            }
            trySend(roomsMap.values.sortedByDescending { it.lastMessageTime })
        }

        val craftsmanRegistration = craftsmanQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            
            snapshot?.documents?.forEach { doc ->
                doc.toObject(ChatRoom::class.java)?.let { room ->
                    roomsMap[room.id] = room
                }
            }
            trySend(roomsMap.values.sortedByDescending { it.lastMessageTime })
        }

        awaitClose {
            clientRegistration.remove()
            professionalRegistration.remove()
            craftsmanRegistration.remove()
        }
    }

    /**
     * Get messages for a specific chat room
     */
    fun getMessages(chatRoomId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chat_rooms")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(chatRoomId: String, message: Message): Result<Unit> = try {
        val batch = firestore.batch()
        
        // 1. Add message to subcollection
        val messageRef = firestore.collection("chat_rooms")
            .document(chatRoomId)
            .collection("messages")
            .document()
            
        val messageWithId = message.copy(id = messageRef.id)
        batch.set(messageRef, messageWithId.toMap())
        
        // 2. Update chat room with last message info and increment unread count
        val roomRef = firestore.collection("chat_rooms").document(chatRoomId)
        
        // Determine which counter to increment
        // If sender is CLIENT, increment PROFESSIONAL unread count
        // If sender is PROFESSIONAL, increment CLIENT unread count
        val updates = mutableMapOf<String, Any>(
            "lastMessage" to message.message,
            "lastMessageTime" to message.timestamp
        )
        
        if (message.senderRole == "CLIENT") {
            // Increment professional's unread count
            updates["unreadCountProfessional"] = com.google.firebase.firestore.FieldValue.increment(1)
            // Also update legacy field for backward compatibility
            updates["unreadCountCraftsman"] = com.google.firebase.firestore.FieldValue.increment(1)
        } else {
            // Increment client's unread count
            updates["unreadCountClient"] = com.google.firebase.firestore.FieldValue.increment(1)
        }
        
        batch.update(roomRef, updates)
        
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Mark messages as read for a user in a chat room
     */
    suspend fun markMessagesAsRead(chatRoomId: String, userRole: String): Result<Unit> = try {
        val roomRef = firestore.collection("chat_rooms").document(chatRoomId)
        
        val updates = mutableMapOf<String, Any>()
        
        if (userRole == "CLIENT") {
            updates["unreadCountClient"] = 0
        } else {
            updates["unreadCountProfessional"] = 0
            updates["unreadCountCraftsman"] = 0 // Reset legacy field too
        }
        
        roomRef.update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Create a chat room (internal use or manual creation)
     */
    suspend fun createChatRoom(chatRoom: ChatRoom): Result<Unit> = try {
        firestore.collection("chat_rooms")
            .document(chatRoom.id)
            .set(chatRoom.toMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
