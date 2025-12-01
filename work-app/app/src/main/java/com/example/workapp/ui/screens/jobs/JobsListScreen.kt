package com.example.workapp.ui.screens.jobs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workapp.data.model.ApplicationStatus
import com.example.workapp.ui.components.JobImage
import com.example.workapp.ui.components.WorkAppTopBar
import com.example.workapp.data.model.Job
import com.example.workapp.data.model.JobApplication
import com.example.workapp.data.model.JobStatus
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.viewmodel.ApplicationViewModel
import com.example.workapp.ui.viewmodel.DeleteJobState
import com.example.workapp.ui.viewmodel.JobViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen displaying jobs based on user role:
 * - Regular users: Jobs they posted
 * - Professionals: Jobs they applied to with status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsListScreen(
    modifier: Modifier = Modifier,
    jobViewModel: JobViewModel = hiltViewModel(),
    applicationViewModel: ApplicationViewModel = hiltViewModel(),
    onJobClick: (String) -> Unit = {},
    onEditJob: (String) -> Unit = {},
    onViewApplications: (String) -> Unit = {},
    currentUserId: String? = null,
    showMyJobs: Boolean = false,
    showAvailableJobs: Boolean = false,
    isProfessional: Boolean = false
) {
    val openJobs by jobViewModel.openJobs.collectAsState()
    val filteredJobs by jobViewModel.filteredJobs.collectAsState()
    val myJobs by jobViewModel.myJobs.collectAsState()
    val myApplications by applicationViewModel.myApplications.collectAsState()
    val deleteJobState by jobViewModel.deleteJobState.collectAsState()
    val isFiltering by jobViewModel.isFiltering.collectAsState()
    val searchQuery by jobViewModel.searchQuery.collectAsState()
    val appliedJobIds by jobViewModel.appliedJobIds.collectAsState()
    val hideAppliedJobs by jobViewModel.hideAppliedJobs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var jobToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedDistance by remember { mutableStateOf<Double?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Load applications if professional and NOT showing available jobs (i.e. showing "My Applications")
    LaunchedEffect(isProfessional, showAvailableJobs) {
        if (isProfessional && !showAvailableJobs) {
            applicationViewModel.loadMyApplications()
            applicationViewModel.markApplicationsAsViewed()
        }
    }

    // Handle delete job state
    LaunchedEffect(deleteJobState) {
        when (deleteJobState) {
            is DeleteJobState.Success -> {
                snackbarHostState.showSnackbar("Job deleted successfully!")
                jobViewModel.resetDeleteJobState()
                jobToDelete = null
            }
            is DeleteJobState.Error -> {
                snackbarHostState.showSnackbar(
                    (deleteJobState as DeleteJobState.Error).message
                )
                jobViewModel.resetDeleteJobState()
            }
            else -> {}
        }
    }

    // Show delete confirmation dialog
    if (showDeleteDialog && jobToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Job") },
            text = { Text("Are you sure you want to delete this job listing? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        jobToDelete?.let { jobViewModel.deleteJob(it) }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Determine content mode
    // Show applications only if professional, not showing my jobs (posted), and NOT showing available jobs (home)
    val showApplications = isProfessional && !showMyJobs && !showAvailableJobs
    val jobsList = if (showMyJobs) myJobs else if (isFiltering) filteredJobs else openJobs

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                WorkAppTopBar(
                    title = when {
                        showMyJobs -> "My Posted Jobs"
                        showApplications -> "My Applications"
                        else -> "Available Jobs"
                    },
                    subtitle = when {
                        showMyJobs -> "${jobsList.size} jobs posted"
                        showApplications -> "${myApplications.size} applications"
                        else -> "${jobsList.size} jobs available"
                    }
                )
                

                
                // Search bar (only for available jobs view)
                if (!showMyJobs && !showApplications) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { jobViewModel.searchJobs(it) },
                        placeholder = { Text("Search jobs...") },
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Actions.search,
                                contentDescription = null,
                                modifier = Modifier.size(IconSizes.medium)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { jobViewModel.searchJobs("") }) {
                                    Icon(
                                        imageVector = AppIcons.Actions.close,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(IconSizes.medium)
                                    )
                                }
                            } else {
                                IconButton(onClick = { showFilterSheet = true }) {
                                    Icon(
                                        imageVector = AppIcons.Actions.filter,
                                        contentDescription = "Filters",
                                        modifier = Modifier.size(IconSizes.medium),
                                        tint = if (selectedDistance != null || hideAppliedJobs) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        singleLine = true
                    )
                }



            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    jobViewModel.refresh()
                    if (isProfessional && !showMyJobs) {
                        applicationViewModel.refresh()
                    }
                    delay(500)
                    isRefreshing = false
                }
            },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showApplications) {
                // Show applications for professionals
                ProfessionalApplicationsList(
                    applications = myApplications,
                    onJobClick = onJobClick
                )
            } else {
                val isLoading by jobViewModel.isLoading.collectAsState()

                com.example.workapp.ui.components.FadeInLoadingContent(
                    isLoading = isLoading,
                    modifier = modifier.fillMaxSize(),
                    skeletonContent = {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(5) {
                                com.example.workapp.ui.components.SkeletonJobCard()
                            }
                        }
                    }
                ) {
                    // Show jobs list for regular users or "My Jobs"
                    if (jobsList.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.Content.work,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .size(IconSizes.large),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = if (showMyJobs) "No jobs posted yet" else "No jobs found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (showMyJobs)
                                    "Create your first job listing"
                                else if (isFiltering)
                                    "Try adjusting your location filters"
                                else
                                    "Check back later for new opportunities",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        // Jobs list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(jobsList) { job ->
                                // Only show edit/delete buttons if the current user created this job
                                val isOwner = currentUserId != null && currentUserId == job.clientId
                                val isApplied = appliedJobIds.contains(job.id)
                                
                                JobCard(
                                    job = job,
                                    onClick = { onJobClick(job.id) },
                                    isApplied = isApplied,
                                    onEdit = if (isOwner) { { onEditJob(job.id) } } else null,
                                    onDelete = if (isOwner) {
                                        {
                                            jobToDelete = job.id
                                            showDeleteDialog = true
                                        }
                                    } else null,
                                    onViewApplications = if (isOwner && job.applicationCount > 0) {
                                        { onViewApplications(job.id) }
                                    } else null,
                                    showApplicationCount = isOwner
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    if (showFilterSheet) {
        JobFilterBottomSheet(
            currentDistance = selectedDistance,
            hideAppliedJobs = hideAppliedJobs,
            onDistanceSelected = { distance ->
                selectedDistance = distance
                if (distance != null) {
                    jobViewModel.filterJobsByDistance(distance)
                } else {
                    jobViewModel.clearFilters()
                }
            },
            onToggleHideAppliedJobs = { hide ->
                jobViewModel.toggleHideAppliedJobs(hide)
            },
            onDismiss = { showFilterSheet = false },
            onClear = {
                selectedDistance = null
                jobViewModel.clearFilters()
                showFilterSheet = false
            },
            sheetState = sheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFilterBottomSheet(
    currentDistance: Double?,
    hideAppliedJobs: Boolean,
    onDistanceSelected: (Double?) -> Unit,
    onToggleHideAppliedJobs: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    sheetState: SheetState
) {
    var tempDistance by remember { mutableStateOf(currentDistance ?: 50.0) }
    var isDistanceEnabled by remember { mutableStateOf(currentDistance != null) }
    var tempHideApplied by remember { mutableStateOf(hideAppliedJobs) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClear) {
                    Text("Clear All")
                }
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Distance Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isDistanceEnabled,
                        onCheckedChange = { isDistanceEnabled = it }
                    )
                }
                
                if (isDistanceEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Within ${tempDistance.toInt()} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = tempDistance.toFloat(),
                        onValueChange = { tempDistance = it.toDouble() },
                        valueRange = 1f..100f,
                        steps = 99
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Hide Applied Jobs Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hide Applied Jobs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Don't show jobs you've already applied to",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = tempHideApplied,
                        onCheckedChange = { tempHideApplied = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Apply Button
                Button(
                    onClick = {
                        onDistanceSelected(if (isDistanceEnabled) tempDistance else null)
                        onToggleHideAppliedJobs(tempHideApplied)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Apply Filters")
                }
            }
        }
    }
}

/**
 * List of applications for professionals
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessionalApplicationsList(
    applications: List<JobApplication>,
    onJobClick: (String) -> Unit
) {
    if (applications.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = AppIcons.Content.work,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(IconSizes.large),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = "No applications yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Apply to jobs to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(applications) { application ->
                ApplicationJobCard(
                    application = application,
                    onClick = { onJobClick(application.jobId) }
                )
            }
        }
    }
}

/**
 * Card displaying job application with status
 */
@Composable
private fun ApplicationJobCard(
    application: JobApplication,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = application.jobTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Status badge
                ApplicationStatusBadge(status = application.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Client name
            Text(
                text = "Posted by ${application.clientName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Budget and Proposed Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                application.jobBudget?.let { budget ->
                    Column {
                        Text(
                            text = "Job Budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "PEN ${String.format("%.0f", budget)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                application.proposedPrice?.let { price ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Your Proposal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "PEN ${String.format("%.0f", price)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Applied date
            Text(
                text = "Applied ${formatDate(application.appliedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            // Show response message if rejected
            if (application.status == ApplicationStatus.REJECTED && application.responseMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = application.responseMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Status badge for application status
 */
@Composable
private fun ApplicationStatusBadge(status: ApplicationStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        ApplicationStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Pending"
        )
        ApplicationStatus.ACCEPTED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Accepted"
        )
        ApplicationStatus.REJECTED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Rejected"
        )
        ApplicationStatus.WITHDRAWN -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Withdrawn"
        )
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.12f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Card displaying job information
 */
@Composable
private fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isApplied: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onViewApplications: (() -> Unit)? = null,
    showApplicationCount: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Job Image (smaller and on the left)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(MaterialTheme.shapes.small)
            ) {
                JobImage(
                    imageUrl = job.imageUrl,
                    category = job.category,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Application Badge
                if (showApplicationCount && job.applicationCount > 0 && job.status == JobStatus.OPEN) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "${job.applicationCount} Application${if (job.applicationCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Applied Badge
                if (isApplied) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Applied",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Information on the right
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Header with title and action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Job title
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Edit and Delete buttons (only for owned jobs)
                    if (onEdit != null || onDelete != null) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            if (onEdit != null) {
                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Actions.edit,
                                        contentDescription = "Edit job",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(IconSizes.small)
                                    )
                                }
                            }
                            if (onDelete != null) {
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Actions.delete,
                                        contentDescription = "Delete job",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(IconSizes.small)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Category and Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Content.work,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(IconSizes.small),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = job.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description preview
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Budget and Posted date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Budget
                    job.budget?.let { budget ->
                        Text(
                            text = "PEN ${String.format("%.0f", budget)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Posted date
                    Text(
                        text = formatDate(job.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Review Applications Button or Status Indicator
                if (onViewApplications != null && job.applicationCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (job.status == JobStatus.OPEN) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onViewApplications)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Content.person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(IconSizes.small)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Review ${job.applicationCount} Application${if (job.applicationCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Job has a selected candidate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Actions.check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(IconSizes.small)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Candidate Selected",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format timestamp to relative date string
 */
private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}