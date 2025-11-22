package com.example.workapp.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.User
import com.example.workapp.data.model.UserRole
import com.example.workapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
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
        bio: String? = null,
        workDistance: Int? = null,
        imageUri: Uri? = null
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
                bio = bio,
                workDistance = workDistance
            ).onSuccess { user ->
                if (imageUri != null) {
                    // Upload image and update profile
                    authRepository.updateProfileWithImage(user, imageUri)
                        .onSuccess { updatedUser ->
                            _currentUser.value = updatedUser
                            _cachedUserRole.value = updatedUser.userRole
                            _authState.value = AuthState.Authenticated(updatedUser)
                        }
                        .onFailure { error ->
                            // Signup succeeded but image upload failed
                            // We still consider user authenticated but maybe show a warning?
                            // For now, just set authenticated with original user
                            _currentUser.value = user
                            _cachedUserRole.value = user.userRole
                            _authState.value = AuthState.Authenticated(user)
                        }
                } else {
                    _currentUser.value = user
                    _cachedUserRole.value = user.userRole
                    _authState.value = AuthState.Authenticated(user)
                }
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
            android.util.Log.d("AuthViewModel", "Starting Google Sign In...")
            
            authRepository.signInWithGoogle(idToken)
                .onSuccess { result ->
                    android.util.Log.d("AuthViewModel", "Google Sign In success: $result")
                    when (result) {
                        is AuthRepository.GoogleSignInResult.Success -> {
                            android.util.Log.d("AuthViewModel", "Existing user found")
                            _currentUser.value = result.user
                            _cachedUserRole.value = result.user.userRole
                            _authState.value = AuthState.Authenticated(result.user)
                        }
                        is AuthRepository.GoogleSignInResult.NewUser -> {
                            android.util.Log.d("AuthViewModel", "New user, needs profile completion")
                            _authState.value = AuthState.NeedsProfileCompletion(result.firebaseUser)
                        }
                    }
                }
                .onFailure { error ->
                    android.util.Log.e("AuthViewModel", "Google Sign In failed", error)
                    _authState.value = AuthState.Error(error.message ?: "Google sign in failed")
                }
        }
    }

    fun completeProfile(
        email: String,
        name: String,
        phone: String,
        location: String,
        role: UserRole,
        craft: String? = null,
        bio: String? = null,
        workDistance: Int? = null,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val firebaseUser = authRepository.currentUser
            if (firebaseUser == null) {
                _authState.value = AuthState.Error("No user signed in")
                return@launch
            }

            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name,
                phone = phone,
                location = location,
                roleString = role.name,
                craft = craft,
                bio = bio,
                workDistance = workDistance,
                experience = if (role == UserRole.CRAFTSMAN) 0 else null,
                rating = if (role == UserRole.CRAFTSMAN) 0.0 else null,
                reviewCount = if (role == UserRole.CRAFTSMAN) 0 else null,
                completedProjects = if (role == UserRole.CRAFTSMAN) 0 else null,
                profileImageUrl = firebaseUser.photoUrl?.toString() // Default to Google photo
            )

            authRepository.completeProfile(user, imageUri)
                .onSuccess { updatedUser ->
                    _currentUser.value = updatedUser
                    _cachedUserRole.value = updatedUser.userRole
                    _authState.value = AuthState.Authenticated(updatedUser)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Failed to complete profile")
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
    suspend fun updateProfile(
        name: String,
        phone: String,
        location: String,
        craft: String? = null,
        bio: String? = null,
        hourlyRate: Double? = null,
        availability: String? = null,
        workDistance: Int? = null,
        imageUri: Uri? = null
    ) {
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
                availability = availability,
                workDistance = workDistance
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
    data class NeedsProfileCompletion(val firebaseUser: FirebaseUser) : AuthState()
}