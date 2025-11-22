package com.example.workapp.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.User
import com.example.workapp.data.model.UserRole
import com.example.workapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication operations
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Cached user role to avoid repeated checks
    private val _cachedUserRole = MutableStateFlow<UserRole?>(null)
    val cachedUserRole: StateFlow<UserRole?> = _cachedUserRole.asStateFlow()
    
    // Quick access to role information
    val isCraftsman: Boolean
        get() = _cachedUserRole.value == UserRole.CRAFTSMAN

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            if (authRepository.isSignedIn()) {
                authRepository.getCurrentUserProfile()
                    .onSuccess { user ->
                        _currentUser.value = user
                        _cachedUserRole.value = user.userRole
                        _authState.value = AuthState.Authenticated(user)
                    }
                    .onFailure { error ->
                        _authState.value = AuthState.Error(error.message ?: "Unknown error")
                    }
            } else {
                _currentUser.value = null
                _cachedUserRole.value = null
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        name: String,
        phone: String,
        location: String,
        role: UserRole,
        craft: String? = null,
        bio: String? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.signUp(
                email = email,
                password = password,
                name = name,
                phone = phone,
                location = location,
                role = role,
                craft = craft,
                bio = bio
            ).onSuccess { user ->
                _currentUser.value = user
                _cachedUserRole.value = user.userRole
                _authState.value = AuthState.Authenticated(user)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.signIn(email, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _cachedUserRole.value = user.userRole
                    _authState.value = AuthState.Authenticated(user)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Sign in failed")
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    _currentUser.value = user
                    _cachedUserRole.value = user.userRole
                    _authState.value = AuthState.Authenticated(user)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Google sign in failed")
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _currentUser.value = null
        _cachedUserRole.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _authState.value = AuthState.PasswordResetSent
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Failed to send reset email")
                }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = if (_currentUser.value != null) {
                AuthState.Authenticated(_currentUser.value!!)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    /**
     * Update user profile with optional image upload
     */
    fun updateProfile(
        name: String,
        phone: String,
        location: String,
        craft: String? = null,
        bio: String? = null,
        hourlyRate: Double? = null,
        availability: String? = null,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            try {
                val currentUser = _currentUser.value ?: throw Exception("No user signed in")
                
                // Create updated user object
                val updatedUser = currentUser.copy(
                    name = name,
                    phone = phone,
                    location = location,
                    craft = craft,
                    bio = bio,
                    hourlyRate = hourlyRate,
                    availability = availability
                )
                
                // Update profile with optional image
                authRepository.updateProfileWithImage(updatedUser, imageUri)
                    .onSuccess { user ->
                        _currentUser.value = user
                        _cachedUserRole.value = user.userRole
                        _authState.value = AuthState.Authenticated(user)
                    }
                    .onFailure { error ->
                        throw error
                    }
            } catch (e: Exception) {
                // Error is handled by the caller
                throw e
            }
        }
    }

    /**
     * Refresh current user profile from Firestore
     */
    fun refreshUserProfile() {
        viewModelScope.launch {
            authRepository.getCurrentUserProfile()
                .onSuccess { user ->
                    _currentUser.value = user
                    _cachedUserRole.value = user.userRole
                    _authState.value = AuthState.Authenticated(user)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Failed to refresh profile")
                }
        }
    }

    /**
     * Delete user account and all associated data
     * This will permanently remove the user's account, profile, job listings, and applications
     */
    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.deleteAccount()
                .onSuccess {
                    _currentUser.value = null
                    _cachedUserRole.value = null
                    _authState.value = AuthState.Unauthenticated
                    onSuccess()
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Failed to delete account")
                    onError(error.message ?: "Failed to delete account")
                }
        }
    }
}

sealed class AuthState {
    data object Initial : AuthState()
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    data object PasswordResetSent : AuthState()
}