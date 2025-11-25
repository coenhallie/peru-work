package com.example.workapp.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.ChatRoom
import com.example.workapp.data.model.Message
import com.example.workapp.data.model.MessageType
import com.example.workapp.data.model.NotificationPriority
import com.example.workapp.data.model.NotificationType
import com.example.workapp.data.model.User
import com.example.workapp.data.repository.ChatRepository
import com.example.workapp.data.repository.AuthRepository
import com.example.workapp.data.repository.CloudinaryRepository
import com.example.workapp.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _startChatResult = MutableStateFlow<StartChatResult?>(null)
    val startChatResult: StateFlow<StartChatResult?> = _startChatResult.asStateFlow()

    private var chatRoomsJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                if (user != null) {
                    loadChatRooms(user.uid)
                } else {
                    _chatRooms.value = emptyList()
                    chatRoomsJob?.cancel()
                }
            }
        }
    }

    /**
     * Refresh chat rooms
     */
    fun refresh() {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            loadChatRooms(uid)
        }
    }

    fun loadChatRooms(userId: String? = null) {
        val uid = userId ?: authRepository.currentUser?.uid ?: return
        
        chatRoomsJob?.cancel()
        chatRoomsJob = viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getChatRoomsForUser(uid)
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { rooms ->
                    _chatRooms.value = rooms
                    _isLoading.value = false
                }
        }
    }

    fun loadMessages(chatRoomId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(chatRoomId)
                .catch { e ->
                    _error.value = e.message
                }
                .collect { msgs ->
                    _messages.value = msgs
                    
                    // Mark messages as read when loaded
                    val currentUser = authRepository.currentUser
                    if (currentUser != null) {
                        // We need to know the user's role to mark correct field
                        // For now, let's try to infer or pass it. 
                        // Ideally, we should fetch the user profile to know the role.
                        // But for now, let's assume the repository handles it or we pass a generic "read"
                        // Actually, the repository needs the role string ("CLIENT" or "CRAFTSMAN")
                        // Let's fetch the user profile first or store it in AuthState.
                        
                        // Quick fix: We'll fetch the user role from the chat room itself if possible,
                        // or just try both? No, that's bad.
                        // Let's just use the AuthViewModel's user if available, but we are in ChatViewModel.
                        // We can fetch the user document.
                        
                        // Optimization: We'll just mark it based on the current user ID matching client or professional in the room.
                        // We need the room details first.
                        val room = _chatRooms.value.find { it.id == chatRoomId }
                        if (room != null) {
                            val role = if (room.clientId == currentUser.uid) "CLIENT" else "PROFESSIONAL"
                            chatRepository.markMessagesAsRead(chatRoomId, role)
                        }
                    }
                }
        }
    }

    fun sendMessage(chatRoomId: String, messageText: String) {
        val currentUser = authRepository.currentUser ?: return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            // Determine sender details
            // We need the chat room to know the role
            val room = _chatRooms.value.find { it.id == chatRoomId } ?: return@launch
            
            val role = if (room.clientId == currentUser.uid) "CLIENT" else "PROFESSIONAL"
            val senderName = if (role == "CLIENT") room.clientName else {
                if (room.professionalName.isNotEmpty()) room.professionalName else room.craftsmanName
            }
            
            val message = Message(
                chatRoomId = chatRoomId,
                senderId = currentUser.uid,
                senderName = senderName,
                senderRole = role,
                message = messageText,
                timestamp = System.currentTimeMillis()
            )

            chatRepository.sendMessage(chatRoomId, message)
                .onFailure { e ->
                    _error.value = "Failed to send message: ${e.message}"
                }
        }
    }
    
    /**
     * Send a message with an image attachment
     */
    fun sendImageMessage(chatRoomId: String, imageUri: Uri) {
        val currentUser = authRepository.currentUser ?: return

        viewModelScope.launch {
            _isUploading.value = true
            
            try {
                // First upload the image to Cloudinary
                cloudinaryRepository.uploadImage(imageUri, "chat_images")
                    .onSuccess { imageUrl ->
                        // Determine sender details
                        val room = _chatRooms.value.find { it.id == chatRoomId } ?: return@launch
                        
                        val role = if (room.clientId == currentUser.uid) "CLIENT" else "PROFESSIONAL"
                        val senderName = if (role == "CLIENT") room.clientName else {
                            if (room.professionalName.isNotEmpty()) room.professionalName else room.craftsmanName
                        }
                        
                        val message = Message(
                            chatRoomId = chatRoomId,
                            senderId = currentUser.uid,
                            senderName = senderName,
                            senderRole = role,
                            message = "Image", // Default message for images
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.IMAGE.name,
                            attachmentUrl = imageUrl
                        )

                        chatRepository.sendMessage(chatRoomId, message)
                            .onFailure { e ->
                                _error.value = "Failed to send image: ${e.message}"
                            }
                    }
                    .onFailure { e ->
                        _error.value = "Failed to upload image: ${e.message}"
                    }
            } finally {
                _isUploading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Start or continue a chat with a professional
     * Creates a new chat room if one doesn't exist, or returns the existing one
     * Also sends a notification to the professional
     *
     * @param currentUser The current logged-in user (client)
     * @param professional The professional to start a chat with
     */
    fun startChatWithProfessional(currentUser: User, professional: User, initialMessage: String? = null) {
        Log.d(TAG, "startChatWithProfessional called")
        Log.d(TAG, "currentUser.id: ${currentUser.id}, currentUser.name: ${currentUser.name}")
        Log.d(TAG, "professional.id: ${professional.id}, professional.name: ${professional.name}")
        
        viewModelScope.launch {
            _isLoading.value = true
            _startChatResult.value = null
            
            try {
                chatRepository.getOrCreateDirectChat(currentUser, professional)
                    .onSuccess { chatRoomId ->
                        Log.d(TAG, "Chat room created/retrieved: $chatRoomId")
                        
                        // Check if this is a new chat room (no messages yet)
                        val room = chatRepository.getChatRoom(chatRoomId).getOrNull()
                        val isNewChat = room?.lastMessage == null
                        
                        Log.d(TAG, "isNewChat: $isNewChat")
                        
                        // Send initial message if provided
                        if (!initialMessage.isNullOrBlank()) {
                            sendMessage(chatRoomId, initialMessage)
                        }
                        
                        if (isNewChat) {
                            Log.d(TAG, "Creating notification for professional: ${professional.id}")
                            // Send notification to the professional about new chat request
                            notificationRepository.createNotification(
                                userId = professional.id,
                                type = NotificationType.NEW_MESSAGE,
                                title = "New Service Request",
                                message = if (!initialMessage.isNullOrBlank()) 
                                    "${currentUser.name}: $initialMessage" 
                                else 
                                    "${currentUser.name} wants to discuss a service with you",
                                data = mapOf(
                                    "chatRoomId" to chatRoomId,
                                    "senderId" to currentUser.id,
                                    "senderName" to currentUser.name,
                                    "type" to "SERVICE_REQUEST"
                                ),
                                actionUrl = "chat/$chatRoomId",
                                imageUrl = currentUser.profileImageUrl,
                                priority = NotificationPriority.HIGH
                            ).onSuccess {
                                Log.d(TAG, "Notification created successfully")
                            }.onFailure { e ->
                                Log.e(TAG, "Failed to create notification: ${e.message}", e)
                            }
                        }
                        
                        _startChatResult.value = StartChatResult.Success(chatRoomId)
                        _isLoading.value = false
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Failed to create chat room: ${e.message}", e)
                        _error.value = "Failed to start chat: ${e.message}"
                        _startChatResult.value = StartChatResult.Error(e.message ?: "Unknown error")
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in startChatWithProfessional: ${e.message}", e)
                _error.value = "Failed to start chat: ${e.message}"
                _startChatResult.value = StartChatResult.Error(e.message ?: "Unknown error")
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear the start chat result after handling
     */
    fun clearStartChatResult() {
        _startChatResult.value = null
    }
    
    companion object {
        private const val TAG = "ChatViewModel"
    }
}

/**
 * Result of starting a chat with a professional
 */
sealed class StartChatResult {
    data class Success(val chatRoomId: String) : StartChatResult()
    data class Error(val message: String) : StartChatResult()
}
