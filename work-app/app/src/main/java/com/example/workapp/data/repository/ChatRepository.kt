package com.example.workapp.data.repository

import android.util.Log
import com.example.workapp.data.model.ChatRoom
import com.example.workapp.data.model.Message
import com.example.workapp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChatRepository"

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
        Log.d(TAG, "getChatRoomsForUser called with userId: $userId")
        
        // Query without orderBy to work while indexes are building
        // We'll sort the results in memory
        val clientQuery = firestore.collection("chat_rooms")
            .whereEqualTo("clientId", userId)
            
        // Query for professionalId (new) and craftsmanId (legacy)
        val professionalQuery = firestore.collection("chat_rooms")
            .whereEqualTo("professionalId", userId)

        val craftsmanQuery = firestore.collection("chat_rooms")
            .whereEqualTo("craftsmanId", userId)

        val roomsMap = mutableMapOf<String, ChatRoom>()
        var queriesResponded = 0
        
        val sendResults = {
            queriesResponded++
            Log.d(TAG, "Query responded. Total: $queriesResponded, Rooms found: ${roomsMap.size}")
            trySend(roomsMap.values.sortedByDescending { it.lastMessageTime ?: 0L })
        }
        
        val clientRegistration = clientQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in clientQuery: ${error.message}", error)
                sendResults()
                return@addSnapshotListener
            }
            
            Log.d(TAG, "Client query returned ${snapshot?.size() ?: 0} documents")
            snapshot?.documents?.forEach { doc ->
                Log.d(TAG, "Client doc: ${doc.id}, data: ${doc.data}")
                doc.toObject(ChatRoom::class.java)?.let { room ->
                    room.id = doc.id // Ensure ID is set
                    roomsMap[room.id] = room
                }
            }
            sendResults()
        }
        
        val professionalRegistration = professionalQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in professionalQuery: ${error.message}", error)
                sendResults()
                return@addSnapshotListener
            }
            
            Log.d(TAG, "Professional query returned ${snapshot?.size() ?: 0} documents")
            snapshot?.documents?.forEach { doc ->
                Log.d(TAG, "Professional doc: ${doc.id}, data: ${doc.data}")
                doc.toObject(ChatRoom::class.java)?.let { room ->
                    room.id = doc.id // Ensure ID is set
                    roomsMap[room.id] = room
                }
            }
            sendResults()
        }

        val craftsmanRegistration = craftsmanQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in craftsmanQuery: ${error.message}", error)
                sendResults()
                return@addSnapshotListener
            }
            
            Log.d(TAG, "Craftsman query returned ${snapshot?.size() ?: 0} documents")
            snapshot?.documents?.forEach { doc ->
                Log.d(TAG, "Craftsman doc: ${doc.id}, data: ${doc.data}")
                doc.toObject(ChatRoom::class.java)?.let { room ->
                    room.id = doc.id // Ensure ID is set
                    roomsMap[room.id] = room
                }
            }
            sendResults()
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
    
    /**
     * Get or create a chat room between a client and a professional
     * This is used when a client wants to start a conversation with a professional
     * before any job is created.
     *
     * @param client The client user initiating the chat
     * @param professional The professional user to chat with
     * @return The chat room ID (existing or newly created)
     */
    suspend fun getOrCreateDirectChat(
        client: User,
        professional: User
    ): Result<String> = try {
        Log.d(TAG, "getOrCreateDirectChat - client.id: ${client.id}, professional.id: ${professional.id}")
        
        // Generate a consistent chat room ID for direct messages
        // Format: direct_{sortedUserId1}_{sortedUserId2} to ensure consistency
        val sortedIds = listOf(client.id, professional.id).sorted()
        val chatRoomId = "direct_${sortedIds[0]}_${sortedIds[1]}"
        
        Log.d(TAG, "Generated chatRoomId: $chatRoomId")
        
        // Check if chat room already exists
        val existingRoom = firestore.collection("chat_rooms")
            .document(chatRoomId)
            .get()
            .await()
        
        if (existingRoom.exists()) {
            Log.d(TAG, "Chat room already exists: $chatRoomId")
            // Return existing chat room
            Result.success(chatRoomId)
        } else {
            Log.d(TAG, "Creating new chat room: $chatRoomId")
            // Create new chat room
            val chatRoom = ChatRoom(
                id = chatRoomId,
                jobId = "", // No job associated yet - this is a direct message
                jobTitle = "Direct Message",
                clientId = client.id,
                clientName = client.name,
                clientProfileImage = client.profileImageUrl,
                professionalId = professional.id,
                professionalName = professional.name,
                professionalProfileImage = professional.profileImageUrl,
                lastMessage = null,
                lastMessageTime = System.currentTimeMillis(),
                unreadCountClient = 0,
                unreadCountProfessional = 1, // Professional has 1 unread (the new chat request)
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            
            val chatRoomData = chatRoom.toMap()
            Log.d(TAG, "Chat room data to write: $chatRoomData")
            
            firestore.collection("chat_rooms")
                .document(chatRoomId)
                .set(chatRoomData)
                .await()
            
            Log.d(TAG, "Chat room created successfully: $chatRoomId")
            Result.success(chatRoomId)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error in getOrCreateDirectChat: ${e.message}", e)
        Result.failure(e)
    }
    
    /**
     * Get a specific chat room by ID
     */
    suspend fun getChatRoom(chatRoomId: String): Result<ChatRoom> = try {
        val doc = firestore.collection("chat_rooms")
            .document(chatRoomId)
            .get()
            .await()
        
        val chatRoom = doc.toObject(ChatRoom::class.java)
        if (chatRoom != null) {
            Result.success(chatRoom)
        } else {
            Result.failure(Exception("Chat room not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
