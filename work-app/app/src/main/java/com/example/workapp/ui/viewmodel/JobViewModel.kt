package com.example.workapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.Job
import com.example.workapp.data.model.JobStatus
import android.net.Uri
import com.example.workapp.data.repository.AuthRepository
import com.example.workapp.data.repository.JobRepository
import com.example.workapp.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for managing job operations
 */
@HiltViewModel
class JobViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _createJobState = MutableStateFlow<CreateJobState>(CreateJobState.Idle)
    val createJobState: StateFlow<CreateJobState> = _createJobState.asStateFlow()

    private val _updateJobState = MutableStateFlow<UpdateJobState>(UpdateJobState.Idle)
    val updateJobState: StateFlow<UpdateJobState> = _updateJobState.asStateFlow()

    private val _deleteJobState = MutableStateFlow<DeleteJobState>(DeleteJobState.Idle)
    val deleteJobState: StateFlow<DeleteJobState> = _deleteJobState.asStateFlow()

    private val _openJobs = MutableStateFlow<List<Job>>(emptyList())
    val openJobs: StateFlow<List<Job>> = _openJobs.asStateFlow()

    private val _filteredJobs = MutableStateFlow<List<Job>>(emptyList())
    val filteredJobs: StateFlow<List<Job>> = _filteredJobs.asStateFlow()

    private val _myJobs = MutableStateFlow<List<Job>>(emptyList())
    val myJobs: StateFlow<List<Job>> = _myJobs.asStateFlow()

    private val _currentJob = MutableStateFlow<Job?>(null)
    val currentJob: StateFlow<Job?> = _currentJob.asStateFlow()

    private val _totalApplicationCount = MutableStateFlow(0)
    val totalApplicationCount: StateFlow<Int> = _totalApplicationCount.asStateFlow()

    private val _isFiltering = MutableStateFlow(false)
    val isFiltering: StateFlow<Boolean> = _isFiltering.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Track active job subscriptions to cancel them on sign out
    private var openJobsSubscription: CoroutineJob? = null
    private var myJobsSubscription: CoroutineJob? = null
    private var professionalJobsSubscription: CoroutineJob? = null
    private var currentUserId: String? = null

    init {
        // Load job if jobId is present in SavedStateHandle
        savedStateHandle.get<String>("jobId")?.let { jobId ->
            loadJob(jobId)
        }

        // Monitor auth state to reload jobs when user changes
        viewModelScope.launch {
            authRepository.authState.collect { firebaseUser ->
                val newUserId = firebaseUser?.uid
                
                // If user changed (including sign-out), clear data and reload
                if (newUserId != currentUserId) {
                    clearAllJobData()
                    currentUserId = newUserId
                    
                    if (newUserId != null) {
                        // User signed in, load their jobs
                        loadOpenJobs()
                        loadMyJobs()
                    }
                }
            }
        }
    }
    
    /**
     * Clear all job data and cancel active subscriptions
     * Called when user signs out or switches accounts
     */
    private fun clearAllJobData() {
        // Cancel active subscriptions
        openJobsSubscription?.cancel()
        myJobsSubscription?.cancel()
        professionalJobsSubscription?.cancel()
        
        // Clear data
        _openJobs.value = emptyList()
        _myJobs.value = emptyList()
        _totalApplicationCount.value = 0
        _currentJob.value = null
        
        // Reset states
        _createJobState.value = CreateJobState.Idle
        _updateJobState.value = UpdateJobState.Idle
        _deleteJobState.value = DeleteJobState.Idle
    }

    /**
     * Create a new job listing
     */
    fun createJob(
        title: String,
        description: String,
        category: String,
        location: String,
        imageUris: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            if (_createJobState.value is CreateJobState.Loading || _createJobState.value is CreateJobState.Success) return@launch
            _createJobState.value = CreateJobState.Loading

            try {
                val currentUser = authRepository.currentUser
                if (currentUser == null) {
                    _createJobState.value = CreateJobState.Error("User not authenticated")
                    return@launch
                }

                // Get user profile to check role
                val userProfile = authRepository.getCurrentUserProfile().getOrNull()
                if (userProfile == null) {
                    _createJobState.value = CreateJobState.Error("Unable to get user profile")
                    return@launch
                }

                // Upload images if selected
                val uploadedImageUrls = mutableListOf<String>()
                
                for (uriString in imageUris) {
                    val uploadResult = jobRepository.uploadJobImage(Uri.parse(uriString))
                    if (uploadResult.isSuccess) {
                        uploadResult.getOrNull()?.let { uploadedImageUrls.add(it) }
                    } else {
                        // If one fails, we could either fail all or continue with partial.
                        // For now, let's log/ignore and continue, or we could fail.
                        // Let's fail to ensure data integrity for the user's intent.
                        _createJobState.value = CreateJobState.Error(
                            uploadResult.exceptionOrNull()?.message ?: "Failed to upload one of the images"
                        )
                        return@launch
                    }
                }

                val job = Job(
                    title = title,
                    description = description,
                    category = category,
                    location = location,
                    budget = null,
                    clientId = currentUser.uid,
                    clientName = userProfile.name,
                    clientRole = userProfile.roleString,
                    status = JobStatus.OPEN,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    images = if (uploadedImageUrls.isNotEmpty()) uploadedImageUrls else null,
                    imageUrl = uploadedImageUrls.firstOrNull() // Main display image is the first one
                )

                val result = jobRepository.createJob(job)
                result.fold(
                    onSuccess = { jobId ->
                        _createJobState.value = CreateJobState.Success(jobId)
                    },
                    onFailure = { error ->
                        _createJobState.value = CreateJobState.Error(
                            error.message ?: "Failed to create job"
                        )
                    }
                )
            } catch (e: Exception) {
                _createJobState.value = CreateJobState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Refresh job data
     */
    fun refresh() {
        loadOpenJobs()
        loadMyJobs()
    }

    /**
     * Load all open jobs (for craftsmen)
     */
    fun loadOpenJobs() {
        // Cancel existing subscription
        openJobsSubscription?.cancel()
        
        openJobsSubscription = viewModelScope.launch {
            _isLoading.value = true
            jobRepository.getOpenJobs().collect { jobs ->
                _openJobs.value = jobs
                // Initially, filtered jobs are the same as open jobs
                if (!_isFiltering.value) {
                    _filteredJobs.value = jobs
                }
                _isLoading.value = false
            }
        }
    }

    /**
     * Filter jobs by distance from user's location
     * @param maxDistanceKm Maximum distance in kilometers
     */
    fun filterJobsByDistance(maxDistanceKm: Double) {
        viewModelScope.launch {
            _isFiltering.value = true
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _filteredJobs.value = emptyList()
                return@launch
            }

            // Get user profile for location
            val userProfileResult = authRepository.getCurrentUserProfile()
            val userProfile = userProfileResult.getOrNull()
            
            if (userProfile == null || userProfile.location.isBlank()) {
                // If user has no location, we can't filter by distance
                // For now, just show all jobs or maybe show a message
                _filteredJobs.value = _openJobs.value
                return@launch
            }

            // Get user coordinates
            val userCoords = locationRepository.getCoordinates(userProfile.location)
            if (userCoords == null) {
                _filteredJobs.value = _openJobs.value
                return@launch
            }

            // Filter jobs
            val currentJobs = _openJobs.value
            val filteredList = currentJobs.filter { job ->
                if (job.location.isBlank()) return@filter false
                
                val jobCoords = locationRepository.getCoordinates(job.location) ?: return@filter false
                val distance = locationRepository.calculateDistanceKm(userCoords, jobCoords)
                
                distance <= maxDistanceKm
            }
            
            _filteredJobs.value = filteredList
        }
    }

    /**
     * Clear filters
     */
    fun clearFilters() {
        _isFiltering.value = false
        _filteredJobs.value = _openJobs.value
    }

    /**
     * Load jobs for current user (jobs they posted as client)
     */
    fun loadMyJobs() {
        // Cancel existing subscription
        myJobsSubscription?.cancel()
        
        myJobsSubscription = viewModelScope.launch {
            _isLoading.value = true
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                // Load jobs created by this client
                jobRepository.getJobsByClient(currentUser.uid).collect { jobs ->
                    _myJobs.value = jobs
                    _totalApplicationCount.value = jobs.filter { it.status == JobStatus.OPEN }.sumOf { it.applicationCount }
                    _isLoading.value = false
                }
            } else {
                _myJobs.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    /**
     * Load jobs assigned to professional
     */
    fun loadProfessionalJobs() {
        // Cancel existing subscriptions
        professionalJobsSubscription?.cancel()
        
        professionalJobsSubscription = viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                jobRepository.getJobsByProfessional(currentUser.uid).collect { jobs ->
                    _myJobs.value = jobs
                }
            } else {
                _myJobs.value = emptyList()
            }
        }
    }

    /**
     * Accept a job (for craftsmen)
     */
    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                jobRepository.assignProfessional(
                    jobId = jobId,
                    professionalId = currentUser.uid,
                    professionalName = currentUser.displayName ?: "Unknown"
                )
            }
        }
    }

    /**
     * Load a specific job by ID
     */
    fun loadJob(jobId: String) {
        viewModelScope.launch {
            val result = jobRepository.getJobById(jobId)
            result.fold(
                onSuccess = { job ->
                    _currentJob.value = job
                },
                onFailure = { error ->
                    // Handle error if needed
                }
            )
        }
    }

    /**
     * Update an existing job listing
     */
    fun updateJob(
        jobId: String,
        title: String,
        description: String,
        category: String,
        location: String
    ) {
        viewModelScope.launch {
            _updateJobState.value = UpdateJobState.Loading

            try {
                val currentUser = authRepository.currentUser
                if (currentUser == null) {
                    _updateJobState.value = UpdateJobState.Error("User not authenticated")
                    return@launch
                }



                // Get the current job to preserve fields we're not updating
                val currentJobResult = jobRepository.getJobById(jobId)
                currentJobResult.fold(
                    onSuccess = { currentJob ->
                        // Verify user owns this job
                        if (currentJob.clientId != currentUser.uid) {
                            _updateJobState.value = UpdateJobState.Error("You can only edit your own jobs")
                            return@launch
                        }

                        val updatedJob = currentJob.copy(
                            title = title,
                            description = description,
                            category = category,
                            location = location,
                            budget = null,
                            updatedAt = System.currentTimeMillis()
                        )

                        val result = jobRepository.updateJob(updatedJob)
                        result.fold(
                            onSuccess = {
                                _updateJobState.value = UpdateJobState.Success
                            },
                            onFailure = { error ->
                                _updateJobState.value = UpdateJobState.Error(
                                    error.message ?: "Failed to update job"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _updateJobState.value = UpdateJobState.Error(
                            error.message ?: "Failed to load job"
                        )
                    }
                )
            } catch (e: Exception) {
                _updateJobState.value = UpdateJobState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Delete a job listing
     */
    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            _deleteJobState.value = DeleteJobState.Loading

            try {
                val currentUser = authRepository.currentUser
                if (currentUser == null) {
                    _deleteJobState.value = DeleteJobState.Error("User not authenticated")
                    return@launch
                }

                // Verify user owns this job before deleting
                val currentJobResult = jobRepository.getJobById(jobId)
                currentJobResult.fold(
                    onSuccess = { currentJob ->
                        if (currentJob.clientId != currentUser.uid) {
                            _deleteJobState.value = DeleteJobState.Error("You can only delete your own jobs")
                            return@launch
                        }

                        val result = jobRepository.deleteJob(jobId)
                        result.fold(
                            onSuccess = {
                                _deleteJobState.value = DeleteJobState.Success
                            },
                            onFailure = { error ->
                                _deleteJobState.value = DeleteJobState.Error(
                                    error.message ?: "Failed to delete job"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _deleteJobState.value = DeleteJobState.Error(
                            error.message ?: "Failed to load job"
                        )
                    }
                )
            } catch (e: Exception) {
                _deleteJobState.value = DeleteJobState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Reset create job state
     */
    fun resetCreateJobState() {
        _createJobState.value = CreateJobState.Idle
    }

    /**
     * Reset update job state
     */
    fun resetUpdateJobState() {
        _updateJobState.value = UpdateJobState.Idle
    }

    /**
     * Reset delete job state
     */
    fun resetDeleteJobState() {
        _deleteJobState.value = DeleteJobState.Idle
    }

    /**
     * Clear current job
     */
    fun clearCurrentJob() {
        _currentJob.value = null
    }
}

/**
 * State for job creation
 */
sealed class CreateJobState {
    data object Idle : CreateJobState()
    data object Loading : CreateJobState()
    data class Success(val jobId: String) : CreateJobState()
    data class Error(val message: String) : CreateJobState()
}

/**
 * State for job update
 */
sealed class UpdateJobState {
    data object Idle : UpdateJobState()
    data object Loading : UpdateJobState()
    data object Success : UpdateJobState()
    data class Error(val message: String) : UpdateJobState()
}

/**
 * State for job deletion
 */
sealed class DeleteJobState {
    data object Idle : DeleteJobState()
    data object Loading : DeleteJobState()
    data object Success : DeleteJobState()
    data class Error(val message: String) : DeleteJobState()
}