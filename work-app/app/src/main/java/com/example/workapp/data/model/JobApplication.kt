package com.example.workapp.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude

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
    
    // Professional info
    val professionalId: String = "",
    val professionalName: String = "",
    val professionalProfileImage: String? = null,
    val professionalRating: Double? = null,
    val professionalExperience: Int? = null,
    val professionalProfession: String? = null,
    
    // Legacy fields
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
    
    // Helpers for backward compatibility
    @get:Exclude
    val applicantId: String
        get() = if (professionalId.isNotEmpty()) professionalId else craftsmanId
        
    @get:Exclude
    val applicantName: String
        get() = if (professionalName.isNotEmpty()) professionalName else craftsmanName

    @get:Exclude
    val applicantProfileImage: String?
        get() = professionalProfileImage ?: craftsmanProfileImage

    @get:Exclude
    val applicantRating: Double?
        get() = professionalRating ?: craftsmanRating

    @get:Exclude
    val applicantExperience: Int?
        get() = professionalExperience ?: craftsmanExperience

    @get:Exclude
    val applicantProfession: String?
        get() = professionalProfession ?: craftsmanCraft

    fun toMap(): Map<String, Any?> = buildMap {
        put("_id", id)
        put("jobId", jobId)
        put("jobTitle", jobTitle)
        put("jobBudget", jobBudget)
        put("clientId", clientId)
        put("clientName", clientName)
        
        put("professionalId", applicantId)
        put("professionalName", applicantName)
        put("professionalProfileImage", applicantProfileImage)
        put("professionalRating", applicantRating)
        put("professionalExperience", applicantExperience)
        put("professionalProfession", applicantProfession)
        
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
    WITHDRAWN   // Professional cancelled their application
}