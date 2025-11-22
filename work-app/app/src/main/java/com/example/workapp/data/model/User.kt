package com.example.workapp.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

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
    
    // Craftsman-specific fields
    val craft: String? = null,
    val bio: String? = null,
    val experience: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val completedProjects: Int? = null,
    val specialties: List<String>? = null,
    val hourlyRate: Double? = null,
    val availability: String? = null,
    val workDistance: Int? = null
) {
    // Computed property for type-safe role access
    @get:Exclude
    val userRole: UserRole
        get() = try {
            UserRole.valueOf(roleString.uppercase())
        } catch (e: IllegalArgumentException) {
            UserRole.CLIENT
        }
    
    @Exclude
    fun isCraftsman(): Boolean = userRole == UserRole.CRAFTSMAN
}

enum class UserRole {
    CLIENT,
    CRAFTSMAN
}