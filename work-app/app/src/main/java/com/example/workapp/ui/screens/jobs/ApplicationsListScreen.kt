package com.example.workapp.ui.screens.jobs

import androidx.compose.foundation.background
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
import com.example.workapp.ui.components.ApplicationActionType
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
    viewModel: ApplicationViewModel = hiltViewModel(),
    chatViewModel: com.example.workapp.ui.viewmodel.ChatViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit = {}
) {
    val applications by viewModel.jobApplications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val acceptApplicationState by viewModel.acceptApplicationState.collectAsState()
    val rejectApplicationState by viewModel.rejectApplicationState.collectAsState()
    val startChatResult by chatViewModel.startChatResult.collectAsState()
    val isChatLoading by chatViewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedApplicationId by remember { mutableStateOf<String?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }
    var selectedActionType by remember { mutableStateOf(ApplicationActionType.ACCEPT) }
    
    // Chat Bottom Sheet State
    var showChatSheet by remember { mutableStateOf(false) }
    var chatMessage by remember { mutableStateOf("") }
    var selectedProfessionalForChat by remember { mutableStateOf<JobApplication?>(null) }

    // Load applications when screen opens
    LaunchedEffect(jobId) {
        viewModel.loadApplicationsForJob(jobId)
    }

    // Handle accept state
    LaunchedEffect(acceptApplicationState) {
        when (acceptApplicationState) {
            is AcceptApplicationState.Success -> {
                snackbarHostState.showSnackbar("Application accepted! Job assigned to professional.")
                viewModel.resetAcceptApplicationState()
                // Reload to update the list with new statuses
                viewModel.loadApplicationsForJob(jobId)
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
                // Reload to update the list
                viewModel.loadApplicationsForJob(jobId)
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


    // Handle chat start result
    LaunchedEffect(startChatResult) {
        when (startChatResult) {
            is com.example.workapp.ui.viewmodel.StartChatResult.Success -> {
                val chatRoomId = (startChatResult as com.example.workapp.ui.viewmodel.StartChatResult.Success).chatRoomId
                chatViewModel.clearStartChatResult()
                showChatSheet = false
                chatMessage = ""
                selectedProfessionalForChat = null
                onNavigateToChat(chatRoomId)
            }
            is com.example.workapp.ui.viewmodel.StartChatResult.Error -> {
                snackbarHostState.showSnackbar(
                    (startChatResult as com.example.workapp.ui.viewmodel.StartChatResult.Error).message
                )
                chatViewModel.clearStartChatResult()
            }
            else -> {}
        }
    }

    // Clean up when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetAcceptApplicationState()
            viewModel.resetRejectApplicationState()
            chatViewModel.clearStartChatResult()
        }
    }

    // Action Bottom Sheet
    if (showActionSheet && selectedApplicationId != null) {
        val application = applications.find { it.id == selectedApplicationId }
        application?.let { app ->
            com.example.workapp.ui.components.ApplicationActionBottomSheet(
                actionType = selectedActionType,
                applicantName = app.applicantName,
                onDismiss = { showActionSheet = false },
                onConfirm = { message ->
                    if (selectedActionType == ApplicationActionType.ACCEPT) {
                        viewModel.acceptApplication(
                            applicationId = app.id,
                            jobId = app.jobId,
                            professionalId = app.applicantId,
                            professionalName = app.applicantName,
                            professionalProfileImage = app.applicantProfileImage,
                            introMessage = message
                        )
                    } else {
                        viewModel.rejectApplication(
                            applicationId = app.id,
                            message = message
                        )
                    }
                    showActionSheet = false
                }
            )
        }

    }

    // Chat Bottom Sheet
    if (showChatSheet && selectedProfessionalForChat != null) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp) // Add padding for keyboard/navigation
            ) {
                Text(
                    text = "Chat with ${selectedProfessionalForChat?.applicantName}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Send an introductory message to start the conversation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                androidx.compose.material3.OutlinedTextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    label = { Text("Message") },
                    placeholder = { Text("Hi, I'm interested in your application...") },
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        selectedProfessionalForChat?.let { app ->
                            // Call the ViewModel helper function to start the chat

                            
                            chatViewModel.startChatWithProfessionalFromApplication(
                                professionalId = app.applicantId,
                                professionalName = app.applicantName,
                                professionalImage = app.applicantProfileImage,
                                initialMessage = chatMessage
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = chatMessage.isNotBlank() && !isChatLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isChatLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Message")
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Job Applications",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "${applications.size} applications",
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
        com.example.workapp.ui.components.FadeInLoadingContent(
            isLoading = isLoading,
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            skeletonContent = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(3) {
                        com.example.workapp.ui.components.SkeletonApplicationReviewCard()
                    }
                }
            }
        ) {
            if (applications.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            text = "Check back later for professional applications",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Check if any application is accepted to determine if we should show action buttons
                val isJobFilled = applications.any { it.status == ApplicationStatus.ACCEPTED }

                // Applications list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sort: Accepted first, then Pending, then others
                    val sortedApplications = applications.sortedWith(
                        compareBy<JobApplication> { 
                            when(it.status) {
                                ApplicationStatus.ACCEPTED -> 0
                                ApplicationStatus.PENDING -> 1
                                else -> 2
                            }
                        }.thenByDescending { it.appliedAt }
                    )

                    items(sortedApplications) { application ->
                        ApplicationCard(
                            application = application,
                            isJobFilled = isJobFilled,
                            onAccept = {
                                selectedApplicationId = application.id
                                selectedActionType = com.example.workapp.ui.components.ApplicationActionType.ACCEPT
                                showActionSheet = true
                            },
                            onReject = {
                                selectedApplicationId = application.id
                                selectedActionType = com.example.workapp.ui.components.ApplicationActionType.REJECT
                                showActionSheet = true
                            },
                            isLoading = acceptApplicationState is AcceptApplicationState.Loading ||
                                       (rejectApplicationState is RejectApplicationState.Loading &&
                                        selectedApplicationId == application.id),
                            onChat = {
                                // Open bottom sheet instead of direct navigation
                                selectedProfessionalForChat = application
                                chatMessage = "" // Reset message
                                showChatSheet = true
                            }
                        )
                    }
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
    isJobFilled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onChat: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val isAccepted = application.status == ApplicationStatus.ACCEPTED
    val isRejected = application.status == ApplicationStatus.REJECTED
    
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
            // Header: Professional Info + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Professional Info (Left)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    // Avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
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

                    Column {
                        Text(
                            text = application.applicantName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            application.applicantProfession?.let { craft ->
                                Text(
                                    text = craft,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            application.applicantRating?.let { rating ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AppIcons.Content.star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format("%.1f", rating),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        application.applicantExperience?.let { experience ->
                            Text(
                                text = "$experience years experience",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Status Badge (Right) - Only if not pending
                if (application.status != ApplicationStatus.PENDING) {
                    val (statusText, statusColor, containerColor) = when(application.status) {
                        ApplicationStatus.ACCEPTED -> Triple(
                            "Hired", 
                            MaterialTheme.colorScheme.onSecondaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                        ApplicationStatus.REJECTED -> Triple(
                            "Rejected", 
                            MaterialTheme.colorScheme.onErrorContainer,
                            MaterialTheme.colorScheme.errorContainer
                        )
                        ApplicationStatus.WITHDRAWN -> Triple(
                            "Withdrawn", 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        else -> Triple("", MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface)
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(containerColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                }
                }

            // Chat Button
            OutlinedButton(
                onClick = onChat,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = AppIcons.Navigation.chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val firstName = application.applicantName.split(" ").firstOrNull() ?: "Professional"
                Text("Chat with $firstName")
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

            // Action buttons - Only show if application is PENDING and job is NOT filled
            if (application.status == ApplicationStatus.PENDING && !isJobFilled) {
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