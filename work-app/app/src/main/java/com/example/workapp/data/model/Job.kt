package com.example.workapp.data.model

import com.google.firebase.firestore.Exclude
/**
 * Job data model representing work requests and bookings
 */
data class Job(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientRole: String = "CLIENT", // Role of the person who created the job
    val professionalId: String? = null,
    val professionalName: String? = null,
    // Legacy fields
    val craftsmanId: String? = null,
    val craftsmanName: String? = null,
    
    val status: JobStatus = JobStatus.OPEN,
    val budget: Double? = null,
    val proposedPrice: Double? = null,
    val finalPrice: Double? = null,
    val deadline: String? = null,
    val scheduledDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val images: List<String>? = null,
    val imageUrl: String? = null, // Main display image
    val notes: String? = null,
    // Application tracking
    val applicationCount: Int = 0,
    val hasActiveApplications: Boolean = false
) {
    // Helpers for backward compatibility
    @get:Exclude
    val assignedProfessionalId: String?
        get() = professionalId ?: craftsmanId
        
    @get:Exclude
    val assignedProfessionalName: String?
        get() = professionalName ?: craftsmanName

    fun toMap(): Map<String, Any?> = buildMap {
        put("id", id)
        put("title", title)
        put("description", description)
        put("category", category)
        put("location", location)
        put("clientId", clientId)
        put("clientName", clientName)
        put("clientRole", clientRole)
        put("professionalId", assignedProfessionalId)
        put("professionalName", assignedProfessionalName)
        // Also write legacy fields for safety? Or just new ones? 
        // Let's write new ones primarily.
        put("status", status.name)
        put("budget", budget)
        put("proposedPrice", proposedPrice)
        put("finalPrice", finalPrice)
        put("deadline", deadline)
        put("scheduledDate", scheduledDate)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("completedAt", completedAt)
        put("images", images)
        put("imageUrl", imageUrl)
        put("notes", notes)
        put("applicationCount", applicationCount)
        put("hasActiveApplications", hasActiveApplications)
    }
}

enum class JobStatus {
    OPEN,           // Available for professionals to accept
    PENDING,        // Waiting for professional response
    ACCEPTED,       // Professional accepted
    IN_PROGRESS,    // Work is being done
    COMPLETED,      // Work finished
    CANCELLED       // Job cancelled
}