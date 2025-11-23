package com.example.workapp.ui.screens.jobs

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workapp.data.model.ApplicationStatus
import com.example.workapp.data.model.JobApplication
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.viewmodel.AcceptApplicationState
import com.example.workapp.ui.viewmodel.ApplicationViewModel
import com.example.workapp.ui.viewmodel.RejectApplicationState
import com.example.workapp.ui.components.SkeletonApplicationReviewCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying all applications for a job (for clients to review)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsListScreen(
    jobId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApplicationViewModel = hiltViewModel()
) {
    val applications by viewModel.jobApplications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val acceptApplicationState by viewModel.acceptApplicationState.collectAsState()
    val rejectApplicationState by viewModel.rejectApplicationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedApplicationId by remember { mutableStateOf<String?>(null) }
    var showAcceptDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }

    // Load applications when screen opens
    LaunchedEffect(jobId) {
        viewModel.loadPendingApplicationsForJob(jobId)
    }

    // Handle accept state
    LaunchedEffect(acceptApplicationState) {
        when (acceptApplicationState) {
            is AcceptApplicationState.Success -> {
                snackbarHostState.showSnackbar("Application accepted! Job assigned to craftsman.")
                viewModel.resetAcceptApplicationState()
                onNavigateBack() // Go back after successful acceptance
            }
            is AcceptApplicationState.Error -> {
                snackbarHostState.showSnackbar(
                    (acceptApplicationState as AcceptApplicationState.Error).message
                )
                viewModel.resetAcceptApplicationState()
            }
            else -> {}
        }
    }

    // Handle reject state
    LaunchedEffect(rejectApplicationState) {
        when (rejectApplicationState) {
            is RejectApplicationState.Success -> {
                snackbarHostState.showSnackbar("Application rejected")
                viewModel.resetRejectApplicationState()
                selectedApplicationId = null
            }
            is RejectApplicationState.Error -> {
                snackbarHostState.showSnackbar(
                    (rejectApplicationState as RejectApplicationState.Error).message
                )
                viewModel.resetRejectApplicationState()
            }
            else -> {}
        }
    }

    // Clean up when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetAcceptApplicationState()
            viewModel.resetRejectApplicationState()
        }
    }

    // Accept confirmation dialog
    if (showAcceptDialog && selectedApplicationId != null) {
        val application = applications.find { it.id == selectedApplicationId }
        application?.let { app ->
            AlertDialog(
                onDismissRequest = { showAcceptDialog = false },
                title = { Text("Accept Application") },
                text = {
                    Text("Accept ${app.craftsmanName}'s application? This will assign the job to them and notify other applicants.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.acceptApplication(
                                applicationId = app.id,
                                jobId = app.jobId,
                                craftsmanId = app.craftsmanId,
                                craftsmanName = app.craftsmanName
                            )
                            showAcceptDialog = false
                        }
                    ) {
                        Text("Accept")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAcceptDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Reject confirmation dialog
    if (showRejectDialog && selectedApplicationId != null) {
        val application = applications.find { it.id == selectedApplicationId }
        application?.let { app ->
            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text("Reject Application") },
                text = { Text("Reject ${app.craftsmanName}'s application?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.rejectApplication(selectedApplicationId!!)
                            showRejectDialog = false
                        }
                    ) {
                        Text("Reject", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Job Applications",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "${applications.size} pending applications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = AppIcons.Navigation.back,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) {
                    com.example.workapp.ui.components.SkeletonApplicationReviewCard()
                }
            }
        } else if (applications.isEmpty()) {
            // Empty state
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Content.person,
                        contentDescription = null,
                        modifier = Modifier.size(IconSizes.large),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No applications yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Check back later for craftsmen applications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            // Applications list
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(applications) { application ->
                    ApplicationCard(
                        application = application,
                        onAccept = {
                            selectedApplicationId = application.id
                            showAcceptDialog = true
                        },
                        onReject = {
                            selectedApplicationId = application.id
                            showRejectDialog = true
                        },
                        isLoading = acceptApplicationState is AcceptApplicationState.Loading ||
                                   (rejectApplicationState is RejectApplicationState.Loading &&
                                    selectedApplicationId == application.id)
                    )
                }
            }
        }
    }
}

/**
 * Card displaying application information
 */
@Composable
private fun ApplicationCard(
    application: JobApplication,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Craftsman header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Content.person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.craftsmanName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        application.craftsmanCraft?.let { craft ->
                            Text(
                                text = craft,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        application.craftsmanRating?.let { rating ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AppIcons.Content.star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", rating),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    application.craftsmanExperience?.let { experience ->
                        Text(
                            text = "$experience years experience",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Application details
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Proposed price
                application.proposedPrice?.let { price ->
                    DetailRow(
                        label = "Proposed Price",
                        value = "PEN ${String.format("%.0f", price)}",
                        valueColor = MaterialTheme.colorScheme.primary,
                        icon = AppIcons.Content.payment
                    )
                }

                // Estimated duration
                application.estimatedDuration?.let { duration ->
                    DetailRow(
                        label = "Estimated Duration",
                        value = duration,
                        icon = AppIcons.Content.schedule
                    )
                }

                // Availability
                application.availability?.let { availability ->
                    DetailRow(
                        label = "Availability",
                        value = availability,
                        icon = AppIcons.Content.schedule
                    )
                }

                // Applied date
                DetailRow(
                    label = "Applied",
                    value = formatDate(application.appliedAt),
                    icon = AppIcons.Content.schedule
                )
            }

            // Cover letter if provided
            application.coverLetter?.let { coverLetter ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cover Letter",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = coverLetter,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Action buttons
            if (application.status == ApplicationStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Reject")
                        }
                    }
                    
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Accept")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detail row component
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(IconSizes.medium),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor
        )
    }
}

/**
 * Format timestamp to readable date string
 */
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}