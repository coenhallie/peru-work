package com.example.workapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.Notification
import com.example.workapp.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing notifications UI state
 * Follows Material 3 patterns with reactive state management
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    // UI State
    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    
    // Notifications list with real-time updates
    val notifications: StateFlow<List<Notification>> = currentUserId?.let { userId ->
        notificationRepository.getNotifications(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } ?: MutableStateFlow(emptyList())
    
    // Unread count for badge display
    val unreadCount: StateFlow<Int> = currentUserId?.let { userId ->
        notificationRepository.getUnreadCount(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )
    } ?: MutableStateFlow(0)
    
    init {
        loadNotifications()
    }
    
    /**
     * Load notifications for current user
     */
    private fun loadNotifications() {
        viewModelScope.launch {
            val userId = currentUserId
            if (userId == null) {
                _uiState.value = NotificationsUiState.Error("User not authenticated")
                return@launch
            }
            
            _uiState.value = NotificationsUiState.Loading
            
            // Notifications are loaded via Flow, so we just wait a bit for initial load
            kotlinx.coroutines.delay(500)
            
            if (notifications.value.isEmpty()) {
                _uiState.value = NotificationsUiState.Empty
            } else {
                _uiState.value = NotificationsUiState.Success
            }
        }
    }
    
    /**
     * Mark a single notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }
    
    /**
     * Mark all notifications as read
     * Used for "Mark all read" action
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            
            val result = notificationRepository.markAllAsRead(userId)
            result.onFailure { error ->
                _uiState.value = NotificationsUiState.Error(
                    error.message ?: "Failed to mark all as read"
                )
            }
        }
    }
    
    /**
     * Delete old notifications (cleanup)
     */
    fun deleteOldNotifications() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            notificationRepository.deleteOldNotifications(userId, daysOld = 30)
        }
    }
    
    /**
     * Refresh notifications
     */
    fun refresh() {
        loadNotifications()
    }
}

/**
 * UI state for notifications screen
 */
sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    object Success : NotificationsUiState()
    object Empty : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}