package com.example.workapp.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Job Application data model representing a craftsman's application to a job
 */
data class JobApplication(
    @get:PropertyName("_id")
    @set:PropertyName("_id")
    var id: String = "",
    
    // Job reference
    val jobId: String = "",
    val jobTitle: String = "",
    val jobBudget: Double? = null,
    val clientId: String = "",
    val clientName: String = "",
    
    // Craftsman info
    val craftsmanId: String = "",
    val craftsmanName: String = "",
    val craftsmanProfileImage: String? = null,
    val craftsmanRating: Double? = null,
    val craftsmanExperience: Int? = null,
    val craftsmanCraft: String? = null,
    
    // Application details
    val proposedPrice: Double? = null,
    val estimatedDuration: String? = null,
    val coverLetter: String? = null,
    val availability: String? = null,
    
    // Status tracking
    val statusString: String = ApplicationStatus.PENDING.name,
    val appliedAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null,
    val responseMessage: String? = null,
    
    // Notification tracking
    val isReadByClient: Boolean = false,        // Client has viewed this application
    val clientViewedAt: Long? = null            // When client first viewed the application
) {
    // Computed property for type-safe status access
    val status: ApplicationStatus
        get() = try {
            ApplicationStatus.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            ApplicationStatus.PENDING
        }
    
    fun toMap(): Map<String, Any?> = buildMap {
        put("_id", id)
        put("jobId", jobId)
        put("jobTitle", jobTitle)
        put("jobBudget", jobBudget)
        put("clientId", clientId)
        put("clientName", clientName)
        put("craftsmanId", craftsmanId)
        put("craftsmanName", craftsmanName)
        put("craftsmanProfileImage", craftsmanProfileImage)
        put("craftsmanRating", craftsmanRating)
        put("craftsmanExperience", craftsmanExperience)
        put("craftsmanCraft", craftsmanCraft)
        put("proposedPrice", proposedPrice)
        put("estimatedDuration", estimatedDuration)
        put("coverLetter", coverLetter)
        put("availability", availability)
        put("statusString", statusString)
        put("appliedAt", appliedAt)
        put("respondedAt", respondedAt)
        put("responseMessage", responseMessage)
        put("isReadByClient", isReadByClient)
        put("clientViewedAt", clientViewedAt)
    }
}

/**
 * Application status enumeration
 */
enum class ApplicationStatus {
    PENDING,    // Waiting for client decision
    ACCEPTED,   // Client accepted this application - job is assigned
    REJECTED,   // Client chose someone else
    WITHDRAWN   // Craftsman cancelled their application
}