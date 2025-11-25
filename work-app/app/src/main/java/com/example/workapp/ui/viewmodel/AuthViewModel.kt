package com.example.workapp.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.PreviousJob
import com.example.workapp.data.model.User
import com.example.workapp.data.model.UserRole
import com.example.workapp.data.repository.AuthRepository
import com.example.workapp.data.repository.CloudinaryRepository
import com.example.workapp.ui.screens.auth.PreviousJobItem
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
    private val authRepository: AuthRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val notificationRepository: com.example.workapp.data.repository.NotificationRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Cached user role to avoid repeated checks
    private val _cachedUserRole = MutableStateFlow<UserRole?>(null)
    val cachedUserRole: StateFlow<UserRole?> = _cachedUserRole.asStateFlow()

    private val _emailValidationState = MutableStateFlow<EmailValidationState>(EmailValidationState.Idle)
    val emailValidationState: StateFlow<EmailValidationState> = _emailValidationState.asStateFlow()
    
    // Quick access to role information
    val isProfessional: Boolean
        get() = _cachedUserRole.value == UserRole.PROFESSIONAL

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
        profession: String? = null,
        bio: String? = null,
        workDistance: Int? = null,
        imageUri: Uri? = null,
        previousJobs: List<PreviousJobItem>? = null
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
                profession = profession,
                bio = bio,
                workDistance = workDistance
            ).onSuccess { user ->
                // Upload profile image if provided
                var updatedUser = user
                if (imageUri != null) {
                    authRepository.updateProfileWithImage(user, imageUri)
                        .onSuccess { userWithImage ->
                            updatedUser = userWithImage
                        }
                        .onFailure { error ->
                            // Continue with user without profile image
                        }
                }
                
                // Upload previous jobs if provided
                if (previousJobs != null && previousJobs.isNotEmpty()) {
                    val uploadedJobs = uploadPreviousJobs(previousJobs)
                    if (uploadedJobs.isNotEmpty()) {
                        updatedUser = updatedUser.copy(previousJobs = uploadedJobs)
                        authRepository.updateUserProfile(updatedUser)
                            .onSuccess { finalUser ->
                                updatedUser = finalUser
                            }
                    }
                }
                
                _currentUser.value = updatedUser
                _cachedUserRole.value = updatedUser.userRole
                _authState.value = AuthState.Authenticated(updatedUser)
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
        imageUri: Uri? = null,
        previousJobs: List<PreviousJobItem>? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val firebaseUser = authRepository.currentUser
            if (firebaseUser == null) {
                _authState.value = AuthState.Error("No user signed in")
                return@launch
            }

            var user = User(
                id = firebaseUser.uid,
                email = email,
                name = name,
                phone = phone,
                location = location,
                roleString = role.name,
                profession = craft,
                bio = bio,
                workDistance = workDistance,
                experience = if (role == UserRole.PROFESSIONAL) 0 else null,
                rating = if (role == UserRole.PROFESSIONAL) 0.0 else null,
                reviewCount = if (role == UserRole.PROFESSIONAL) 0 else null,
                completedProjects = if (role == UserRole.PROFESSIONAL) 0 else null,
                profileImageUrl = firebaseUser.photoUrl?.toString() // Default to Google photo
            )

            // Complete profile with image
            authRepository.completeProfile(user, imageUri)
                .onSuccess { updatedUser ->
                    var finalUser = updatedUser
                    
                    // Upload previous jobs if provided
                    if (previousJobs != null && previousJobs.isNotEmpty()) {
                        val uploadedJobs = uploadPreviousJobs(previousJobs)
                        if (uploadedJobs.isNotEmpty()) {
                            finalUser = finalUser.copy(previousJobs = uploadedJobs)
                            authRepository.updateUserProfile(finalUser)
                                .onSuccess { userWithJobs ->
                                    finalUser = userWithJobs
                                }
                        }
                    }
                    
                    _currentUser.value = finalUser
                    _cachedUserRole.value = finalUser.userRole
                    _authState.value = AuthState.Authenticated(finalUser)
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
        imageUri: Uri? = null,
        newPreviousJobs: List<PreviousJobItem>? = null,
        existingPreviousJobs: List<PreviousJob>? = null
    ) {
        try {
            val currentUser = _currentUser.value ?: throw Exception("No user signed in")
            
            // Create updated user object
            val updatedUser = currentUser.copy(
                name = name,
                phone = phone,
                location = location,
                profession = craft,
                bio = bio,
                hourlyRate = hourlyRate,
                availability = availability,
                workDistance = workDistance
            )
            
            var finalUser = updatedUser

            // Handle previous jobs
            val allPreviousJobs = mutableListOf<PreviousJob>()
            
            // Add existing jobs that weren't deleted
            if (existingPreviousJobs != null) {
                allPreviousJobs.addAll(existingPreviousJobs)
            }
            
            // Upload and add new jobs
            if (newPreviousJobs != null && newPreviousJobs.isNotEmpty()) {
                val uploadedNewJobs = uploadPreviousJobs(newPreviousJobs)
                allPreviousJobs.addAll(uploadedNewJobs)
            }
            
            // Only update previousJobs if we have changes (either new jobs added or existing passed explicitly)
            // If both are null, we assume no changes to previous jobs section
            if (newPreviousJobs != null || existingPreviousJobs != null) {
                finalUser = finalUser.copy(previousJobs = allPreviousJobs)
            }
            
            // Update profile with optional image
            authRepository.updateProfileWithImage(finalUser, imageUri)
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
    
    /**
     * Upload previous jobs photos to Cloudinary and return PreviousJob objects with URLs
     */
    private suspend fun uploadPreviousJobs(previousJobs: List<PreviousJobItem>): List<PreviousJob> {
        val uploadedJobs = mutableListOf<PreviousJob>()
        
        previousJobs.forEach { jobItem ->
            val photoUrls = mutableListOf<String>()
            
            // Upload each photo to Cloudinary
            jobItem.photoUris.forEach { uri ->
                try {
                    cloudinaryRepository.uploadImage(uri, "previous_jobs")
                        .onSuccess { url ->
                            photoUrls.add(url)
                        }
                        .onFailure { error ->
                            // Log error but continue with other photos
                            android.util.Log.e("AuthViewModel", "Failed to upload photo", error)
                        }
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Error uploading photo", e)
                }
            }
            
            // Add job even if no photos uploaded successfully (description is still valuable)
            uploadedJobs.add(
                PreviousJob(
                    description = jobItem.description,
                    photoUrls = photoUrls
                )
            )
        }
        
        return uploadedJobs
    }


    fun validateEmail(email: String) {
        if (email.isBlank()) {
            _emailValidationState.value = EmailValidationState.Invalid("Email cannot be empty")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailValidationState.value = EmailValidationState.Invalid("Invalid email format")
            return
        }

        viewModelScope.launch {
            _emailValidationState.value = EmailValidationState.Validating
            authRepository.checkEmailExists(email)
                .onSuccess { exists ->
                    if (exists) {
                        _emailValidationState.value = EmailValidationState.Invalid("Email already registered")
                    } else {
                        _emailValidationState.value = EmailValidationState.Valid
                    }
                }
                .onFailure { error ->
                    _emailValidationState.value = EmailValidationState.Invalid("Failed to validate email: ${error.message}")
                }
        }
    }

    fun resetEmailValidation() {
        _emailValidationState.value = EmailValidationState.Idle
    }

    fun updateFCMToken(token: String) {
        viewModelScope.launch {
            notificationRepository.updateFCMToken(token)
                .onSuccess {
                    android.util.Log.d("AuthViewModel", "FCM token updated successfully")
                }
                .onFailure { error ->
                    android.util.Log.e("AuthViewModel", "Failed to update FCM token", error)
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

sealed class EmailValidationState {
    data object Idle : EmailValidationState()
    data object Validating : EmailValidationState()
    data object Valid : EmailValidationState()
    data class Invalid(val reason: String) : EmailValidationState()
}