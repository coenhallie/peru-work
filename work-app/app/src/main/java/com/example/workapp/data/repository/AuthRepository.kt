package com.example.workapp.data.repository

import android.net.Uri
import com.example.workapp.data.model.User
import com.example.workapp.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Firebase Authentication operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    /**
     * Get current Firebase user
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Flow that emits the current authentication state
     */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean = currentUser != null

    /**
     * Sign up with email and password
     */
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        phone: String,
        location: String,
        role: UserRole,
        craft: String? = null,
        bio: String? = null,
        workDistance: Int? = null
    ): Result<User> = try {
        // Create auth account
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("User creation failed")

        // Create user profile
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
            completedProjects = if (role == UserRole.CRAFTSMAN) 0 else null
        )

        // Save to Firestore
        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(user)
            .await()

        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("Sign in failed")

        // Get user profile from Firestore
        val userDoc = firestore.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()

        android.util.Log.d("AuthRepository", "Fetched user profile: ${userDoc.data}")
        val user = userDoc.toObject(User::class.java) ?: throw Exception("User profile not found")

        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Sign in with Google using Credential Manager API
     * @param idToken The Google ID token obtained from Credential Manager
     */
    /**
     * Result of Google Sign In
     */
    sealed class GoogleSignInResult {
        data class Success(val user: User) : GoogleSignInResult()
        data class NewUser(val firebaseUser: FirebaseUser) : GoogleSignInResult()
    }

    /**
     * Sign in with Google using Credential Manager API
     * @param idToken The Google ID token obtained from Credential Manager
     */
    suspend fun signInWithGoogle(idToken: String): Result<GoogleSignInResult> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user ?: throw Exception("Google sign in failed")

        // Check if user profile exists in Firestore
        val userDoc = firestore.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()

        if (userDoc.exists()) {
            // Existing user - return their profile
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User profile not found")
            Result.success(GoogleSignInResult.Success(user))
        } else {
            // New user - return NewUser result to trigger registration flow
            Result.success(GoogleSignInResult.NewUser(firebaseUser))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Complete user profile for a user who is already authenticated (e.g. via Google)
     */
    suspend fun completeProfile(
        user: User,
        imageUri: Uri? = null
    ): Result<User> = try {
        val firebaseUser = currentUser ?: throw Exception("No user signed in")
        
        // Verify the user ID matches
        if (user.id != firebaseUser.uid) {
            throw Exception("User ID mismatch")
        }

        // Upload image if provided
        val userWithImage = if (imageUri != null) {
            val imageUrlResult = uploadProfileImage(imageUri)
            if (imageUrlResult.isFailure) {
                // If upload fails, we continue without image but maybe log it
                // or we could throw. For now let's continue.
                user
            } else {
                user.copy(profileImageUrl = imageUrlResult.getOrNull())
            }
        } else {
            user
        }

        // Save to Firestore
        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(userWithImage)
            .await()

        Result.success(userWithImage)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Sign out
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get current user profile from Firestore
     */
    suspend fun getCurrentUserProfile(): Result<User> = try {
        val firebaseUser = currentUser ?: throw Exception("No user signed in")
        
        val userDoc = firestore.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()

        val user = userDoc.toObject(User::class.java) ?: throw Exception("User profile not found")

        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(user: User): Result<Unit> = try {
        val firebaseUser = currentUser ?: throw Exception("No user signed in")
        
        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(user)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Upload profile image to Firebase Storage
     * @param imageUri The URI of the image to upload
     * @return The download URL of the uploaded image
     */
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            MediaManager.get().upload(imageUri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Optional: Handle start
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Optional: Handle progress
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String ?: resultData["url"] as? String
                        if (url != null) {
                            continuation.resume(Result.success(url))
                        } else {
                            continuation.resume(Result.failure(Exception("No URL in response")))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception(error.description)))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception("Upload rescheduled: ${error.description}")))
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Update user profile with optional image upload
     * @param user The updated user data
     * @param imageUri Optional image URI to upload
     */
    suspend fun updateProfileWithImage(user: User, imageUri: Uri?): Result<User> = try {
        val firebaseUser = currentUser ?: throw Exception("No user signed in")

        // Upload image if provided
        val updatedUser = if (imageUri != null) {
            val imageUrlResult = uploadProfileImage(imageUri)
            if (imageUrlResult.isFailure) {
                throw imageUrlResult.exceptionOrNull() ?: Exception("Failed to upload image")
            }
            val newImageUrl = imageUrlResult.getOrNull()
            
            // Delete old image if it exists and is different
            if (!user.profileImageUrl.isNullOrEmpty() && user.profileImageUrl != newImageUrl) {
                deleteImage(user.profileImageUrl)
            }
            
            user.copy(profileImageUrl = newImageUrl)
        } else {
            user
        }

        // Update Firestore
        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(updatedUser)
            .await()

        Result.success(updatedUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Delete image from Cloudinary
     */
    /**
     * Delete image from Cloudinary
     */
    private suspend fun deleteImage(imageUrl: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val publicId = extractPublicIdFromUrl(imageUrl)
            if (publicId != null) {
                // Use a dedicated Cloudinary instance with the full URL (including secret) for deletion
                // MediaManager might not expose the secret-containing instance for admin operations
                val cloudinary = com.cloudinary.Cloudinary(com.example.workapp.BuildConfig.CLOUDINARY_URL)
                
                // Use ObjectUtils.asMap for parameters
                val params = com.cloudinary.utils.ObjectUtils.asMap("invalidate", true)
                
                cloudinary.uploader().destroy(publicId, params)
                android.util.Log.d("AuthRepository", "Deleted old image: $publicId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("AuthRepository", "Failed to delete old image: ${e.message}")
            // We don't want to fail the whole operation if deletion fails
        }
    }

    private fun extractPublicIdFromUrl(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val path = uri.path ?: return null
            // Cloudinary URLs are typically /<cloud_name>/image/upload/v<version>/<public_id>.<extension>
            // or /<cloud_name>/image/upload/<public_id>.<extension>
            val lastSegment = uri.lastPathSegment ?: return null
            val fileName = lastSegment.substringBeforeLast(".")
            
            // If the public ID contains folders, we might need more complex logic.
            // For now, assuming simple public IDs or that we can extract from the path.
            // A more robust way is to look for "upload/" and take everything after the version.
            
            val uploadIndex = path.indexOf("upload/")
            if (uploadIndex != -1) {
                var publicIdWithVersion = path.substring(uploadIndex + 7)
                // Remove version if present (starts with v and followed by numbers)
                val parts = publicIdWithVersion.split("/")
                if (parts.isNotEmpty() && parts[0].startsWith("v") && parts[0].drop(1).all { it.isDigit() }) {
                    publicIdWithVersion = publicIdWithVersion.substringAfter("/")
                }
                return publicIdWithVersion.substringBeforeLast(".")
            }
            
            fileName
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete user account and all associated data
     * This will:
     * 1. Delete all job listings created by the user
     * 2. Delete all job applications submitted by the user
     * 3. Delete user's profile image from Storage
     * 4. Delete user document from Firestore
     * 5. Delete Firebase Auth account
     */
    suspend fun deleteAccount(): Result<Unit> = try {
        val firebaseUser = currentUser ?: throw Exception("No user signed in")
        val userId = firebaseUser.uid

        // Delete all job listings created by this user
        val userJobs = firestore.collection("jobs")
            .whereEqualTo("clientId", userId)
            .get()
            .await()

        userJobs.documents.forEach { doc ->
            doc.reference.delete().await()
        }

        // Delete all job applications submitted by this user
        val userApplications = firestore.collection("job_applications")
            .whereEqualTo("craftsmanId", userId)
            .get()
            .await()

        userApplications.documents.forEach { doc ->
            doc.reference.delete().await()
        }

        // Delete profile image from Storage if it exists
        // Note: Cloudinary image deletion is not handled here as it requires Admin API or signed signature
        // which is better handled via backend or Cloudinary dashboard/rules.
        /*
        try {
            val imageRef = storage.reference
                .child("profile_images")
                .child("${userId}.jpg")
            imageRef.delete().await()
        } catch (e: Exception) {
            // Image might not exist, ignore error
        }
        */

        // Delete user document from Firestore
        firestore.collection("users")
            .document(userId)
            .delete()
            .await()

        // Delete Firebase Auth account
        firebaseUser.delete().await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}