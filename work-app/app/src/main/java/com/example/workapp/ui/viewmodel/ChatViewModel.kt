package com.example.workapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.ChatRoom
import com.example.workapp.data.model.Message
import com.example.workapp.data.repository.ChatRepository
import com.example.workapp.data.repository.AuthRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadChatRooms()
    }

    fun loadChatRooms() {
        val currentUser = authRepository.currentUser ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getChatRoomsForUser(currentUser.uid)
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
                        
                        // Optimization: We'll just mark it based on the current user ID matching client or craftsman in the room.
                        // We need the room details first.
                        val room = _chatRooms.value.find { it.id == chatRoomId }
                        if (room != null) {
                            val role = if (room.clientId == currentUser.uid) "CLIENT" else "CRAFTSMAN"
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
            
            val role = if (room.clientId == currentUser.uid) "CLIENT" else "CRAFTSMAN"
            val senderName = if (role == "CLIENT") room.clientName else room.craftsmanName
            
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
    
    fun clearError() {
        _error.value = null
    }
}
