package com.example.workapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.User
import com.example.workapp.data.repository.CraftsmenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for craftsmen listing and search
 */
@HiltViewModel
class CraftsmenViewModel @Inject constructor(
    private val craftsmenRepository: CraftsmenRepository,
    private val locationRepository: com.example.workapp.data.repository.LocationRepository,
    private val authRepository: com.example.workapp.data.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CraftsmenUiState>(CraftsmenUiState.Loading)
    val uiState: StateFlow<CraftsmenUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _maxDistance = MutableStateFlow<Double?>(null)
    val maxDistance: StateFlow<Double?> = _maxDistance.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    // Keep track of all loaded craftsmen to apply local filters
    private var allCraftsmen: List<User> = emptyList()

    init {
        loadCraftsmen()
        loadCategories()
    }

    /**
     * Refresh craftsmen data
     */
    fun refresh() {
        loadCraftsmen()
    }

    private fun loadCraftsmen() {
        viewModelScope.launch {
            try {
                craftsmenRepository.getAllCraftsmen().collect { craftsmen ->
                    allCraftsmen = craftsmen
                    applyFilters()
                }
            } catch (e: Exception) {
                _uiState.value = CraftsmenUiState.Error(
                    e.message ?: "Failed to load craftsmen"
                )
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            craftsmenRepository.getCraftCategories()
                .onSuccess { cats ->
                    _categories.value = cats
                }
        }
    }

    fun searchCraftsmen(query: String) {
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
            _uiState.value = CraftsmenUiState.Loading

            val query = _searchQuery.value
            val category = _selectedCategory.value
            val distance = _maxDistance.value

            var filteredList = allCraftsmen

            // 1. Apply Search
            if (query.isNotBlank()) {
                filteredList = filteredList.filter { user ->
                    user.name.contains(query, ignoreCase = true) ||
                    user.craft?.contains(query, ignoreCase = true) == true ||
                    user.specialties?.any { it.contains(query, ignoreCase = true) } == true
                }
            }

            // 2. Apply Category
            if (category != null) {
                filteredList = filteredList.filter { it.craft == category }
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
                            filteredList = filteredList.filter { craftsman ->
                                if (craftsman.location.isBlank()) return@filter false
                                val craftsmanCoords = locationRepository.getCoordinates(craftsman.location) ?: return@filter false
                                val dist = locationRepository.calculateDistanceKm(userCoords, craftsmanCoords)
                                dist <= distance
                            }
                        }
                    }
                }
            }

            _uiState.value = if (filteredList.isEmpty()) {
                CraftsmenUiState.Empty
            } else {
                CraftsmenUiState.Success(filteredList)
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

sealed class CraftsmenUiState {
    object Loading : CraftsmenUiState()
    object Empty : CraftsmenUiState()
    data class Success(val craftsmen: List<User>) : CraftsmenUiState()
    data class Error(val message: String) : CraftsmenUiState()
}