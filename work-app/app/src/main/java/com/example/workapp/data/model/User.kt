package com.example.workapp.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Represents a previous job/project completed by a craftsman
 */
@IgnoreExtraProperties
data class PreviousJob(
    val description: String = "",
    val photoUrls: List<String> = emptyList() // Max 2 photos
)

/**
 * User data model representing both clients and craftsmen
 *
 * IMPORTANT: Supports legacy database records that use 'role' field name
 * The model will correctly read from either 'roleString' or 'role' field
 */
@IgnoreExtraProperties
data class User(
    var id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val location: String = "",
    
    // Primary field for role
    var roleString: String = "CLIENT",
    
    val profileImageUrl: String? = null,
    
    // Professional-specific fields
    val profession: String? = null, // Renamed from craft
    val craft: String? = null, // Legacy field support
    val bio: String? = null,
    val experience: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val completedProjects: Int? = null,
    val specialties: List<String>? = null,
    val hourlyRate: Double? = null,
    val availability: String? = null,
    val workDistance: Int? = null,
    val previousJobs: List<PreviousJob>? = null
) {
    // Computed property for type-safe role access
    @get:Exclude
    val userRole: UserRole
        get() = try {
            val role = roleString.uppercase()
            if (role == "CRAFTSMAN") UserRole.PROFESSIONAL else UserRole.valueOf(role)
        } catch (e: IllegalArgumentException) {
            UserRole.CLIENT
        }
    
    @Exclude
    fun isProfessional(): Boolean = userRole == UserRole.PROFESSIONAL

    // Helper to get profession from either new or legacy field
    @get:Exclude
    val currentProfession: String?
        get() = profession ?: craft
}

enum class UserRole {
    CLIENT,
    PROFESSIONAL
}