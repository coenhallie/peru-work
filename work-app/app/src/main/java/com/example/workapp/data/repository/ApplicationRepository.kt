package com.example.workapp.data.repository

import com.example.workapp.data.model.JobApplication
import com.example.workapp.data.model.ApplicationStatus
import com.example.workapp.data.model.JobStatus
import com.example.workapp.data.model.NotificationType
import com.example.workapp.data.model.NotificationPriority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing job application data with integrated notification system
 */
@Singleton
class ApplicationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) {
    
    /**
     * Submit a new job application
     * Creates notification for the client about the new application
     */
    suspend fun submitApplication(application: JobApplication): Result<String> {
        return try {
            // Check if professional has already applied to this job
            val existingApplication = firestore.collection("job_applications")
                .whereEqualTo("jobId", application.jobId)
                .whereEqualTo("professionalId", application.professionalId)
                .whereIn("statusString", listOf(
                    ApplicationStatus.PENDING.name,
                    ApplicationStatus.ACCEPTED.name
                ))
                .get()
                .await()
            
            if (!existingApplication.isEmpty) {
                return Result.failure(Exception("You have already applied to this job"))
            }
            
            // Create new application
            val docRef = firestore.collection("job_applications").document()
            val applicationWithId = application.copy(id = docRef.id)
            
            docRef.set(applicationWithId.toMap()).await()
            
            // Update job's application count
            updateJobApplicationCount(application.jobId)
            
            // Create notification for client
            notificationRepository.createNotification(
                userId = application.clientId,
                type = NotificationType.APPLICATION_RECEIVED,
                title = "New Application Received",
                message = "${application.professionalName} applied to ${application.jobTitle}",
                data = mapOf(
                    "jobId" to application.jobId,
                    "applicationId" to docRef.id,
                    "professionalId" to application.professionalId
                ),
                actionUrl = "applications/${application.jobId}",
                imageUrl = application.professionalProfileImage,
                priority = NotificationPriority.HIGH
            )
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all applications for a specific job
     */
    fun getApplicationsForJob(jobId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = firestore.collection("job_applications")
            .whereEqualTo("jobId", jobId)
            .orderBy("appliedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On permission errors (like after sign-out), return empty list instead of crashing
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val applications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JobApplication::class.java)
                } ?: emptyList()
                
                trySend(applications)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get all applications submitted by a professional
     */
    fun getApplicationsByProfessional(professionalId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = firestore.collection("job_applications")
            .whereEqualTo("professionalId", professionalId)
            .orderBy("appliedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On permission errors (like after sign-out), return empty list instead of crashing
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val applications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JobApplication::class.java)
                } ?: emptyList()
                
                trySend(applications)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get pending applications for a job (not yet accepted/rejected)
     */
    fun getPendingApplicationsForJob(jobId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = firestore.collection("job_applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("statusString", ApplicationStatus.PENDING.name)
            .orderBy("appliedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On permission errors (like after sign-out), return empty list instead of crashing
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val applications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JobApplication::class.java)
                } ?: emptyList()
                
                trySend(applications)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get a single application by ID
     */
    suspend fun getApplicationById(applicationId: String): Result<JobApplication> = try {
        val doc = firestore.collection("job_applications")
            .document(applicationId)
            .get()
            .await()
        
        val application = doc.toObject(JobApplication::class.java)
            ?: throw Exception("Application not found")
        
        Result.success(application)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Accept an application and assign professional to job
     * This will also reject all other pending applications for the same job
     * Creates notifications for accepted professional and rejected professionals
     */
    suspend fun acceptApplication(
        applicationId: String,
        jobId: String,
        professionalId: String,
        professionalName: String
    ): Result<Unit> = try {
        // Get the accepted application details first
        val acceptedApp = getApplicationById(applicationId).getOrNull()
            ?: throw Exception("Application not found")
        
        val batch = firestore.batch()
        
        // Update the accepted application
        val applicationRef = firestore.collection("job_applications")
            .document(applicationId)
        batch.update(applicationRef, mapOf(
            "statusString" to ApplicationStatus.ACCEPTED.name,
            "respondedAt" to System.currentTimeMillis()
        ))
        
        // Update the job with assigned professional
        val jobRef = firestore.collection("jobs").document(jobId)
        batch.update(jobRef, mapOf(
            "professionalId" to professionalId,
            "professionalName" to professionalName,
            "status" to JobStatus.ACCEPTED.name,
            "updatedAt" to System.currentTimeMillis()
        ))
        
        // Reject all other pending applications for this job
        val otherApplications = firestore.collection("job_applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("statusString", ApplicationStatus.PENDING.name)
            .get()
            .await()
        
        val rejectedApps = mutableListOf<JobApplication>()
        otherApplications.documents.forEach { doc ->
            if (doc.id != applicationId) {
                batch.update(doc.reference, mapOf(
                    "statusString" to ApplicationStatus.REJECTED.name,
                    "respondedAt" to System.currentTimeMillis(),
                    "responseMessage" to "Client selected another professional"
                ))
                doc.toObject(JobApplication::class.java)?.let { rejectedApps.add(it) }
            }
        }
        
        // Create chat room for the accepted job
        val chatRoomId = "job_$jobId"
        val chatRoom = com.example.workapp.data.model.ChatRoom(
            id = chatRoomId,
            jobId = jobId,
            jobTitle = acceptedApp.jobTitle,
            clientId = acceptedApp.clientId,
            clientName = acceptedApp.clientName,
            // clientProfileImage = null, // We might need to fetch this if not in application
            professionalId = professionalId,
            professionalName = professionalName,
            professionalProfileImage = acceptedApp.professionalProfileImage,
            lastMessage = "Chat created",
            lastMessageTime = System.currentTimeMillis(),
            isActive = true
        )
        
        val chatRoomRef = firestore.collection("chat_rooms").document(chatRoomId)
        batch.set(chatRoomRef, chatRoom.toMap())
        
        // Add initial system message
        val messageRef = chatRoomRef.collection("messages").document()
        val systemMessage = com.example.workapp.data.model.Message(
            id = messageRef.id,
            chatRoomId = chatRoomId,
            senderId = "system",
            senderName = "System",
            senderRole = "SYSTEM",
            message = "Application accepted. You can now chat with each other.",
            type = com.example.workapp.data.model.MessageType.SYSTEM.name,
            timestamp = System.currentTimeMillis()
        )
        batch.set(messageRef, systemMessage.toMap())
        
        batch.commit().await()
        
        // Create notification for accepted professional
        notificationRepository.createNotification(
            userId = professionalId,
            type = NotificationType.APPLICATION_ACCEPTED,
            title = "Application Accepted! 🎉",
            message = "Your application for ${acceptedApp.jobTitle} was accepted",
            data = mapOf(
                "jobId" to jobId,
                "applicationId" to applicationId
            ),
            actionUrl = "job_detail/$jobId",
            priority = NotificationPriority.HIGH
        )
        
        // Create notifications for rejected professionals
        rejectedApps.forEach { app ->
            notificationRepository.createNotification(
                userId = app.professionalId,
                type = NotificationType.APPLICATION_REJECTED,
                title = "Application Update",
                message = "The client selected another professional for ${app.jobTitle}",
                data = mapOf(
                    "jobId" to jobId,
                    "applicationId" to app.id
                ),
                actionUrl = "jobs_list",
                priority = NotificationPriority.NORMAL
            )
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Reject an application
     * Creates notification for the professional
     */
    suspend fun rejectApplication(
        applicationId: String,
        message: String? = null
    ): Result<Unit> = try {
        // Get application details first
        val application = getApplicationById(applicationId).getOrNull()
            ?: throw Exception("Application not found")
        
        val updates = buildMap {
            put("statusString", ApplicationStatus.REJECTED.name)
            put("respondedAt", System.currentTimeMillis())
            message?.let { put("responseMessage", it) }
        }
        
        firestore.collection("job_applications")
            .document(applicationId)
            .update(updates)
            .await()
        
        // Create notification for professional
        notificationRepository.createNotification(
            userId = application.professionalId,
            type = NotificationType.APPLICATION_REJECTED,
            title = "Application Update",
            message = message ?: "Your application for ${application.jobTitle} was not selected",
            data = mapOf(
                "jobId" to application.jobId,
                "applicationId" to applicationId
            ),
            actionUrl = "jobs_list",
            priority = NotificationPriority.NORMAL
        )
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Withdraw an application (professional cancels their application)
     */
    suspend fun withdrawApplication(applicationId: String): Result<Unit> = try {
        val application = getApplicationById(applicationId).getOrNull()
            ?: throw Exception("Application not found")
        
        if (application.status != ApplicationStatus.PENDING) {
            throw Exception("Can only withdraw pending applications")
        }
        
        firestore.collection("job_applications")
            .document(applicationId)
            .update(mapOf(
                "statusString" to ApplicationStatus.WITHDRAWN.name,
                "respondedAt" to System.currentTimeMillis()
            ))
            .await()
        
        // Update job's application count
        updateJobApplicationCount(application.jobId)
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Get application count for a job
     */
    suspend fun getApplicationCount(jobId: String): Result<Int> = try {
        val snapshot = firestore.collection("job_applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("statusString", ApplicationStatus.PENDING.name)
            .get()
            .await()
        
        Result.success(snapshot.size())
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Check if a professional has already applied to a job
     */
    suspend fun hasApplied(jobId: String, professionalId: String): Result<Boolean> = try {
        val snapshot = firestore.collection("job_applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("professionalId", professionalId)
            .whereIn("statusString", listOf(
                ApplicationStatus.PENDING.name,
                ApplicationStatus.ACCEPTED.name
            ))
            .get()
            .await()
        
        Result.success(!snapshot.isEmpty)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Mark an application as viewed by the client
     * Used for tracking unread applications (NEW badge)
     */
    suspend fun markApplicationAsViewed(applicationId: String): Result<Unit> = try {
        firestore.collection("job_applications")
            .document(applicationId)
            .update(mapOf(
                "isReadByClient" to true,
                "clientViewedAt" to System.currentTimeMillis()
            ))
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Mark all unread applications for a job as viewed
     * Called when client opens the applications list
     */
    suspend fun markAllApplicationsAsViewed(jobId: String): Result<Unit> = try {
        val unreadApps = firestore.collection("job_applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("isReadByClient", false)
            .get()
            .await()
        
        val batch = firestore.batch()
        unreadApps.documents.forEach { doc ->
            batch.update(doc.reference, mapOf(
                "isReadByClient" to true,
                "clientViewedAt" to System.currentTimeMillis()
            ))
        }
        
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Update job's application count field
     */
    private suspend fun updateJobApplicationCount(jobId: String) {
        try {
            val count = getApplicationCount(jobId).getOrNull() ?: 0
            
            firestore.collection("jobs")
                .document(jobId)
                .update(mapOf(
                    "applicationCount" to count,
                    "hasActiveApplications" to (count > 0)
                ))
                .await()
        } catch (e: Exception) {
            // Log error but don't fail the operation
            println("Failed to update application count: ${e.message}")
        }
    }
}