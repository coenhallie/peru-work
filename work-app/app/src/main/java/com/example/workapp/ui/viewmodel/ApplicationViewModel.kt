package com.example.workapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workapp.data.model.Job
import com.example.workapp.data.model.JobApplication
import com.example.workapp.data.repository.ApplicationRepository
import com.example.workapp.data.repository.AuthRepository
import com.example.workapp.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing job application operations
 */
@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val authRepository: AuthRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _submitApplicationState = MutableStateFlow<SubmitApplicationState>(SubmitApplicationState.Idle)
    val submitApplicationState: StateFlow<SubmitApplicationState> = _submitApplicationState.asStateFlow()

    private val _acceptApplicationState = MutableStateFlow<AcceptApplicationState>(AcceptApplicationState.Idle)
    val acceptApplicationState: StateFlow<AcceptApplicationState> = _acceptApplicationState.asStateFlow()

    private val _rejectApplicationState = MutableStateFlow<RejectApplicationState>(RejectApplicationState.Idle)
    val rejectApplicationState: StateFlow<RejectApplicationState> = _rejectApplicationState.asStateFlow()

    private val _withdrawApplicationState = MutableStateFlow<WithdrawApplicationState>(WithdrawApplicationState.Idle)
    val withdrawApplicationState: StateFlow<WithdrawApplicationState> = _withdrawApplicationState.asStateFlow()

    private val _jobApplications = MutableStateFlow<List<JobApplication>>(emptyList())
    val jobApplications: StateFlow<List<JobApplication>> = _jobApplications.asStateFlow()

    private val _myApplications = MutableStateFlow<List<JobApplication>>(emptyList())
    val myApplications: StateFlow<List<JobApplication>> = _myApplications.asStateFlow()
    
    private val _myApplicationsWithJobs = MutableStateFlow<List<Pair<JobApplication, Job?>>>(emptyList())
    val myApplicationsWithJobs: StateFlow<List<Pair<JobApplication, Job?>>> = _myApplicationsWithJobs.asStateFlow()

    private val _hasApplied = MutableStateFlow<Boolean>(false)
    val hasApplied: StateFlow<Boolean> = _hasApplied.asStateFlow()

    private val _applicationCount = MutableStateFlow<Int>(0)
    val applicationCount: StateFlow<Int> = _applicationCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Submit a new job application
     */
    fun submitApplication(
        jobId: String,
        jobTitle: String,
        jobBudget: Double?,
        clientId: String,
        clientName: String,
        proposedPrice: String?,
        estimatedDuration: String?,
        coverLetter: String?,
        availability: String?
    ) {
        viewModelScope.launch {
            _submitApplicationState.value = SubmitApplicationState.Loading

            try {
                val currentUser = authRepository.currentUser
                if (currentUser == null) {
                    _submitApplicationState.value = SubmitApplicationState.Error("User not authenticated")
                    return@launch
                }

                // Get user profile to get craftsman details
                val userProfile = authRepository.getCurrentUserProfile().getOrNull()
                if (userProfile == null) {
                    _submitApplicationState.value = SubmitApplicationState.Error("Unable to get user profile")
                    return@launch
                }

                if (!userProfile.isProfessional()) {
                    _submitApplicationState.value = SubmitApplicationState.Error("Only professionals can apply to jobs")
                    return@launch
                }

                // Parse proposed price if provided
                val parsedProposedPrice = proposedPrice?.toDoubleOrNull()
                if (proposedPrice != null && parsedProposedPrice == null) {
                    _submitApplicationState.value = SubmitApplicationState.Error("Invalid proposed price")
                    return@launch
                }

                val application = JobApplication(
                    jobId = jobId,
                    jobTitle = jobTitle,
                    jobBudget = jobBudget,
                    clientId = clientId,
                    clientName = clientName,
                    professionalId = currentUser.uid,
                    professionalName = userProfile.name,
                    professionalProfileImage = userProfile.profileImageUrl,
                    professionalRating = userProfile.rating,
                    professionalExperience = userProfile.experience,
                    professionalProfession = userProfile.craft,
                    // Legacy fields for backward compatibility
                    craftsmanId = currentUser.uid,
                    craftsmanName = userProfile.name,
                    craftsmanProfileImage = userProfile.profileImageUrl,
                    craftsmanRating = userProfile.rating,
                    craftsmanExperience = userProfile.experience,
                    craftsmanCraft = userProfile.craft,
                    proposedPrice = parsedProposedPrice,
                    estimatedDuration = estimatedDuration?.takeIf { it.isNotBlank() },
                    coverLetter = coverLetter?.takeIf { it.isNotBlank() },
                    availability = availability?.takeIf { it.isNotBlank() },
                    appliedAt = System.currentTimeMillis()
                )

                val result = applicationRepository.submitApplication(application)
                result.fold(
                    onSuccess = { applicationId ->
                        _submitApplicationState.value = SubmitApplicationState.Success(applicationId)
                    },
                    onFailure = { error ->
                        _submitApplicationState.value = SubmitApplicationState.Error(
                            error.message ?: "Failed to submit application"
                        )
                    }
                )
            } catch (e: Exception) {
                _submitApplicationState.value = SubmitApplicationState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Load applications for a specific job (for clients)
     */
    fun loadApplicationsForJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                applicationRepository.getApplicationsForJob(jobId).collect { applications ->
                    _jobApplications.value = applications
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load pending applications for a specific job
     */
    fun loadPendingApplicationsForJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                applicationRepository.getPendingApplicationsForJob(jobId).collect { applications ->
                    _jobApplications.value = applications
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh application data
     */
    fun refresh() {
        loadMyApplications()
    }

    /**
     * Load all applications submitted by current craftsman
     */
    fun loadMyApplications() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                try {
                    applicationRepository.getApplicationsByProfessional(currentUser.uid).collect { applications ->
                        _myApplications.value = applications
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    _isLoading.value = false
                }
            } else {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load applications with their associated jobs for current craftsman
     */
    fun loadMyApplicationsWithJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                try {
                    applicationRepository.getApplicationsByProfessional(currentUser.uid).collect { applications ->
                        // Fetch job details for each application
                        val applicationsWithJobs = applications.mapNotNull { application ->
                            val jobResult = jobRepository.getJobById(application.jobId)
                            jobResult.getOrNull()?.let { job ->
                                application to job
                            }
                        }
                        _myApplicationsWithJobs.value = applicationsWithJobs
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    _isLoading.value = false
                }
            } else {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if current craftsman has already applied to a job
     */
    fun checkIfApplied(jobId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                val result = applicationRepository.hasApplied(jobId, currentUser.uid)
                _hasApplied.value = result.getOrDefault(false)
            } else {
                _hasApplied.value = false
            }
        }
    }

    /**
     * Get application count for a job
     */
    fun loadApplicationCount(jobId: String) {
        viewModelScope.launch {
            val result = applicationRepository.getApplicationCount(jobId)
            _applicationCount.value = result.getOrDefault(0)
        }
    }

    /**
     * Accept an application (for clients)
     */
    fun acceptApplication(
        applicationId: String,
        jobId: String,
        professionalId: String,
        professionalName: String
    ) {
        viewModelScope.launch {
            _acceptApplicationState.value = AcceptApplicationState.Loading

            try {
                val result = applicationRepository.acceptApplication(
                    applicationId = applicationId,
                    jobId = jobId,
                    professionalId = professionalId,
                    professionalName = professionalName
                )

                result.fold(
                    onSuccess = {
                        _acceptApplicationState.value = AcceptApplicationState.Success
                    },
                    onFailure = { error ->
                        _acceptApplicationState.value = AcceptApplicationState.Error(
                            error.message ?: "Failed to accept application"
                        )
                    }
                )
            } catch (e: Exception) {
                _acceptApplicationState.value = AcceptApplicationState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Reject an application (for clients)
     */
    fun rejectApplication(applicationId: String, message: String? = null) {
        viewModelScope.launch {
            _rejectApplicationState.value = RejectApplicationState.Loading

            try {
                val result = applicationRepository.rejectApplication(applicationId, message)

                result.fold(
                    onSuccess = {
                        _rejectApplicationState.value = RejectApplicationState.Success
                    },
                    onFailure = { error ->
                        _rejectApplicationState.value = RejectApplicationState.Error(
                            error.message ?: "Failed to reject application"
                        )
                    }
                )
            } catch (e: Exception) {
                _rejectApplicationState.value = RejectApplicationState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Withdraw an application (for craftsmen)
     */
    fun withdrawApplication(applicationId: String) {
        viewModelScope.launch {
            _withdrawApplicationState.value = WithdrawApplicationState.Loading

            try {
                val result = applicationRepository.withdrawApplication(applicationId)

                result.fold(
                    onSuccess = {
                        _withdrawApplicationState.value = WithdrawApplicationState.Success
                    },
                    onFailure = { error ->
                        _withdrawApplicationState.value = WithdrawApplicationState.Error(
                            error.message ?: "Failed to withdraw application"
                        )
                    }
                )
            } catch (e: Exception) {
                _withdrawApplicationState.value = WithdrawApplicationState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Reset submit application state
     */
    fun resetSubmitApplicationState() {
        _submitApplicationState.value = SubmitApplicationState.Idle
    }

    /**
     * Reset accept application state
     */
    fun resetAcceptApplicationState() {
        _acceptApplicationState.value = AcceptApplicationState.Idle
    }

    /**
     * Reset reject application state
     */
    fun resetRejectApplicationState() {
        _rejectApplicationState.value = RejectApplicationState.Idle
    }

    /**
     * Reset withdraw application state
     */
    fun resetWithdrawApplicationState() {
        _withdrawApplicationState.value = WithdrawApplicationState.Idle
    }

    /**
     * Reset has applied state
     */
    fun resetHasAppliedState() {
        _hasApplied.value = false
    }
}

/**
 * State for submitting an application
 */
sealed class SubmitApplicationState {
    data object Idle : SubmitApplicationState()
    data object Loading : SubmitApplicationState()
    data class Success(val applicationId: String) : SubmitApplicationState()
    data class Error(val message: String) : SubmitApplicationState()
}

/**
 * State for accepting an application
 */
sealed class AcceptApplicationState {
    data object Idle : AcceptApplicationState()
    data object Loading : AcceptApplicationState()
    data object Success : AcceptApplicationState()
    data class Error(val message: String) : AcceptApplicationState()
}

/**
 * State for rejecting an application
 */
sealed class RejectApplicationState {
    data object Idle : RejectApplicationState()
    data object Loading : RejectApplicationState()
    data object Success : RejectApplicationState()
    data class Error(val message: String) : RejectApplicationState()
}

/**
 * State for withdrawing an application
 */
sealed class WithdrawApplicationState {
    data object Idle : WithdrawApplicationState()
    data object Loading : WithdrawApplicationState()
    data object Success : WithdrawApplicationState()
    data class Error(val message: String) : WithdrawApplicationState()
}