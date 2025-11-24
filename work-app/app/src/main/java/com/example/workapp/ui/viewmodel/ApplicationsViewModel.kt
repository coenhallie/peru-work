package com.example.workapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.Job
import com.example.workapp.data.model.JobApplication
import com.example.workapp.data.repository.ApplicationRepository
import com.example.workapp.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing job applications list and actions
 * Material 3 compliant with reactive state management
 */
@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val jobRepository: JobRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val jobId: String = checkNotNull(savedStateHandle["jobId"])
    
    // UI State for overall screen status
    private val _uiState = MutableStateFlow<ApplicationsUiState>(ApplicationsUiState.Loading)
    val uiState: StateFlow<ApplicationsUiState> = _uiState.asStateFlow()
    
    // Applications list with real-time updates
    val applications: StateFlow<List<JobApplication>> = 
        applicationRepository.getApplicationsForJob(jobId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    // Job details
    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()
    
    // Action state for accepting/rejecting
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    
    init {
        loadJobAndApplications()
    }
    
    /**
     * Load job details and its applications
     */
    private fun loadJobAndApplications() {
        viewModelScope.launch {
            _uiState.value = ApplicationsUiState.Loading
            
            // Load job details
            val jobResult = jobRepository.getJobById(jobId)
            jobResult.fold(
                onSuccess = { job ->
                    _job.value = job
                    _uiState.value = ApplicationsUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = ApplicationsUiState.Error(
                        error.message ?: "Failed to load job details"
                    )
                }
            )
        }
    }
    
    /**
     * Accept an application
     * This will also reject all other pending applications
     */
    fun acceptApplication(
        applicationId: String,
        professionalId: String,
        professionalName: String
    ) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            
            val result = applicationRepository.acceptApplication(
                applicationId = applicationId,
                jobId = jobId,
                professionalId = professionalId,
                professionalName = professionalName
            )
            
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Application accepted!")
                    
                    // Reset state after short delay
                    kotlinx.coroutines.delay(2000)
                    _actionState.value = ActionState.Idle
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(
                        error.message ?: "Failed to accept application"
                    )
                    
                    // Reset state after showing error
                    kotlinx.coroutines.delay(3000)
                    _actionState.value = ActionState.Idle
                }
            )
        }
    }
    
    /**
     * Reject an application with optional message
     */
    fun rejectApplication(
        applicationId: String,
        message: String? = null
    ) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            
            val result = applicationRepository.rejectApplication(
                applicationId = applicationId,
                message = message
            )
            
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Application rejected")
                    
                    // Reset state after short delay
                    kotlinx.coroutines.delay(2000)
                    _actionState.value = ActionState.Idle
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(
                        error.message ?: "Failed to reject application"
                    )
                    
                    // Reset state after showing error
                    kotlinx.coroutines.delay(3000)
                    _actionState.value = ActionState.Idle
                }
            )
        }
    }
    
    /**
     * Mark all applications as viewed by client
     * Called when client opens the applications list
     */
    fun markApplicationsAsViewed() {
        viewModelScope.launch {
            applicationRepository.markAllApplicationsAsViewed(jobId)
        }
    }
    
    /**
     * Refresh applications
     */
    fun refresh() {
        loadJobAndApplications()
    }
}

/**
 * UI state for applications list screen
 */
sealed class ApplicationsUiState {
    object Loading : ApplicationsUiState()
    object Success : ApplicationsUiState()
    data class Error(val message: String) : ApplicationsUiState()
}

/**
 * State for accept/reject actions
 */
sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}