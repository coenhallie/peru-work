package com.example.workapp.data.repository

import android.net.Uri
import com.example.workapp.data.model.User
import com.example.workapp.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
        bio: String? = null
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
    suspend fun signInWithGoogle(idToken: String): Result<User> = try {
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
            Result.success(user)
        } else {
            // New user - create profile with default client role
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "",
                phone = "",
                location = "",
                roleString = UserRole.CLIENT.name,
                profileImageUrl = firebaseUser.photoUrl?.toString()
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.success(user)
        }
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
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> = try {
        val firebaseUser = currentUser ?: throw Exception("No user signed in")
        
        // Create a reference to the profile image location
        val storageRef = storage.reference
            .child("profile_images")
            .child("${firebaseUser.uid}.jpg")
        
        // Upload the file
        storageRef.putFile(imageUri).await()
        
        // Get the download URL
        val downloadUrl = storageRef.downloadUrl.await()
        
        Result.success(downloadUrl.toString())
    } catch (e: Exception) {
        Result.failure(e)
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
            user.copy(profileImageUrl = imageUrlResult.getOrNull())
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
        try {
            val imageRef = storage.reference
                .child("profile_images")
                .child("${userId}.jpg")
            imageRef.delete().await()
        } catch (e: Exception) {
            // Image might not exist, ignore error
        }

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