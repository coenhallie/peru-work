package com.example.workapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.User
import com.example.workapp.data.repository.ProfessionalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for professional listing and search
 */
@HiltViewModel
class ProfessionalViewModel @Inject constructor(
    private val professionalRepository: ProfessionalRepository,
    private val locationRepository: com.example.workapp.data.repository.LocationRepository,
    private val authRepository: com.example.workapp.data.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfessionalUiState>(ProfessionalUiState.Loading)
    val uiState: StateFlow<ProfessionalUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _maxDistance = MutableStateFlow<Double?>(null)
    val maxDistance: StateFlow<Double?> = _maxDistance.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    // Keep track of all loaded professionals to apply local filters
    private var allProfessionals: List<User> = emptyList()

    init {
        loadProfessionals()
        loadCategories()
    }

    /**
     * Refresh professionals data
     */
    fun refresh() {
        loadProfessionals()
    }

    private fun loadProfessionals() {
        viewModelScope.launch {
            try {
                professionalRepository.getAllProfessionals().collect { professionals ->
                    allProfessionals = professionals
                    applyFilters()
                }
            } catch (e: Exception) {
                _uiState.value = ProfessionalUiState.Error(
                    e.message ?: "Failed to load professionals"
                )
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            professionalRepository.getProfessionCategories()
                .onSuccess { cats ->
                    _categories.value = cats
                }
        }
    }

    fun searchProfessionals(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        applyFilters()
    }

    fun filterByDistance(distance: Double?) {
        _maxDistance.value = distance
        applyFilters()
    }

    private fun applyFilters() {
        viewModelScope.launch {
            _uiState.value = ProfessionalUiState.Loading

            val query = _searchQuery.value
            val category = _selectedCategory.value
            val distance = _maxDistance.value

            var filteredList = allProfessionals

            // 1. Apply Search
            if (query.isNotBlank()) {
                filteredList = filteredList.filter { user ->
                    user.name.contains(query, ignoreCase = true) ||
                    user.currentProfession?.contains(query, ignoreCase = true) == true ||
                    user.specialties?.any { it.contains(query, ignoreCase = true) } == true
                }
            }

            // 2. Apply Category
            if (category != null) {
                filteredList = filteredList.filter { it.currentProfession == category }
            }

            // 3. Apply Distance
            if (distance != null) {
                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    val userProfileResult = authRepository.getCurrentUserProfile()
                    val userProfile = userProfileResult.getOrNull()

                    if (userProfile != null && userProfile.location.isNotBlank()) {
                        val userCoords = locationRepository.getCoordinates(userProfile.location)
                        if (userCoords != null) {
                            filteredList = filteredList.filter { professional ->
                                if (professional.location.isBlank()) return@filter false
                                val professionalCoords = locationRepository.getCoordinates(professional.location) ?: return@filter false
                                val dist = locationRepository.calculateDistanceKm(userCoords, professionalCoords)
                                dist <= distance
                            }
                        }
                    }
                }
            }

            _uiState.value = if (filteredList.isEmpty()) {
                ProfessionalUiState.Empty
            } else {
                ProfessionalUiState.Success(filteredList)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        applyFilters()
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _maxDistance.value = null
        applyFilters()
    }
}

sealed class ProfessionalUiState {
    object Loading : ProfessionalUiState()
    object Empty : ProfessionalUiState()
    data class Success(val professionals: List<User>) : ProfessionalUiState()
    data class Error(val message: String) : ProfessionalUiState()
}