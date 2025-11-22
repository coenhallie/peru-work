package com.example.workapp.data.repository

import com.example.workapp.data.model.Job
import com.example.workapp.data.model.JobStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing job data
 */
@Singleton
class JobRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Create a new job
     */
    suspend fun createJob(job: Job): Result<String> = try {
        val docRef = firestore.collection("jobs").document()
        
        // Only use mock image if no image URL is provided
        val finalImageUrl = if (job.imageUrl.isNullOrBlank()) {
            // Add a random mock image if none provided (for testing/demo purposes)
            val mockImages = listOf(
                "https://images.unsplash.com/photo-1581578731117-104f2a863cc2?q=80&w=1000&auto=format&fit=crop", // Construction
                "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?q=80&w=1000&auto=format&fit=crop", // Electrical
                "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?q=80&w=1000&auto=format&fit=crop", // Welding
                "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?q=80&w=1000&auto=format&fit=crop", // Plumbing
                "https://images.unsplash.com/photo-1599696847727-920005c5090a?q=80&w=1000&auto=format&fit=crop"  // Carpentry
            )
            mockImages.random()
        } else {
            job.imageUrl
        }
        
        val jobWithId = job.copy(
            id = docRef.id,
            imageUrl = finalImageUrl
        )
        
        docRef.set(jobWithId.toMap()).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Upload job image to Cloudinary
     * @param imageUri The URI of the image to upload
     * @return The download URL of the uploaded image
     */
    suspend fun uploadJobImage(imageUri: Uri): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            MediaManager.get().upload(imageUri)
                .option("folder", "job_images")
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
     * Get jobs by client ID
     */
    fun getJobsByClient(clientId: String): Flow<List<Job>> = callbackFlow {
        val listener = firestore.collection("jobs")
            .whereEqualTo("clientId", clientId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On permission errors (like after sign-out), return empty list instead of crashing
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Job::class.java)
                } ?: emptyList()
                
                trySend(jobs)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get jobs by craftsman ID
     */
    fun getJobsByCraftsman(craftsmanId: String): Flow<List<Job>> = callbackFlow {
        val listener = firestore.collection("jobs")
            .whereEqualTo("craftsmanId", craftsmanId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On permission errors (like after sign-out), return empty list instead of crashing
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Job::class.java)
                } ?: emptyList()
                
                trySend(jobs)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get open jobs (available for craftsmen)
     * Shows all jobs posted by any user (temporarily for testing)
     */
    fun getOpenJobs(): Flow<List<Job>> = callbackFlow {
        val listener = firestore.collection("jobs")
            .whereEqualTo("status", JobStatus.OPEN.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On permission errors (like after sign-out), return empty list instead of crashing
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Job::class.java)
                } ?: emptyList()
                
                trySend(jobs)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get a single job by ID
     */
    suspend fun getJobById(jobId: String): Result<Job> = try {
        val doc = firestore.collection("jobs")
            .document(jobId)
            .get()
            .await()

        val job = doc.toObject(Job::class.java)
            ?: throw Exception("Job not found")

        Result.success(job)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Update job status
     */
    suspend fun updateJobStatus(jobId: String, status: JobStatus): Result<Unit> = try {
        val updates = mapOf(
            "status" to status.name,
            "updatedAt" to System.currentTimeMillis()
        )
        
        firestore.collection("jobs")
            .document(jobId)
            .update(updates)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Assign craftsman to job
     */
    suspend fun assignCraftsman(
        jobId: String,
        craftsmanId: String,
        craftsmanName: String
    ): Result<Unit> = try {
        val updates = mapOf(
            "craftsmanId" to craftsmanId,
            "craftsmanName" to craftsmanName,
            "status" to JobStatus.ACCEPTED.name,
            "updatedAt" to System.currentTimeMillis()
        )
        
        firestore.collection("jobs")
            .document(jobId)
            .update(updates)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Update job
     */
    suspend fun updateJob(job: Job): Result<Unit> = try {
        firestore.collection("jobs")
            .document(job.id)
            .set(job.toMap())
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Delete job
     */
    suspend fun deleteJob(jobId: String): Result<Unit> = try {
        firestore.collection("jobs")
            .document(jobId)
            .delete()
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}