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
     * Get all chat rooms for a specific user (either as client or craftsman)
     */
    fun getChatRoomsForUser(userId: String): Flow<List<ChatRoom>> = callbackFlow {
        // We need to query for rooms where user is client OR user is craftsman
        // Firestore doesn't support logical OR in queries easily, so we might need two queries
        // However, for simplicity and since we can't do complex ORs, we'll rely on a composite index or separate listeners
        // A common pattern is to have an array of "participants" in the document and use array-contains
        // But given our schema has clientId and craftsmanId, we'll try to set up two listeners and merge,
        // or better yet, just use two separate queries if the user role is known.
        // Since we don't always know the role context here, let's try a simpler approach:
        // We'll assume the UI passes the role or we query both fields.
        
        // Actually, for this app, a user is usually one or the other in a specific context, 
        // but let's just listen to both fields to be safe and merge.
        // LIMITATION: This might require two listeners.
        
        // Optimization: Let's just use the 'participants' array approach if we could change schema,
        // but sticking to the plan: we will query based on the user's ID in either field.
        
        // Since we can't easily merge two live streams without custom logic, let's try to query 
        // based on the user's primary role if possible. 
        // But wait, a user *could* technically be both (though unlikely in this app's current logic).
        // Let's just set up two listeners and combine them locally.
        
        val clientQuery = firestore.collection("chat_rooms")
            .whereEqualTo("clientId", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            
        val craftsmanQuery = firestore.collection("chat_rooms")
            .whereEqualTo("craftsmanId", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        val roomsMap = mutableMapOf<String, ChatRoom>()
        
        val clientRegistration = clientQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
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
                close(error)
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
        // If sender is CLIENT, increment CRAFTSMAN unread count
        // If sender is CRAFTSMAN, increment CLIENT unread count
        val updates = mutableMapOf<String, Any>(
            "lastMessage" to message.message,
            "lastMessageTime" to message.timestamp
        )
        
        if (message.senderRole == "CLIENT") {
            // Increment craftsman's unread count
            // Note: Firestore increment is safer for concurrent updates
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
        
        val fieldToReset = if (userRole == "CLIENT") {
            "unreadCountClient"
        } else {
            "unreadCountCraftsman"
        }
        
        roomRef.update(fieldToReset, 0).await()
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
