package com.example.workapp.ui.screens.jobs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.workapp.ui.viewmodel.ApplicationViewModel
import com.example.workapp.ui.viewmodel.WithdrawApplicationState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen displaying all applications submitted by the craftsman
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    onNavigateBack: () -> Unit,
    onJobClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApplicationViewModel = hiltViewModel()
) {
    val applications by viewModel.myApplications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val withdrawApplicationState by viewModel.withdrawApplicationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedApplicationId by remember { mutableStateOf<String?>(null) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    // Load applications when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadMyApplications()
    }

    // Handle withdraw state
    LaunchedEffect(withdrawApplicationState) {
        when (withdrawApplicationState) {
            is WithdrawApplicationState.Success -> {
                snackbarHostState.showSnackbar("Application withdrawn")
                viewModel.resetWithdrawApplicationState()
                selectedApplicationId = null
            }
            is WithdrawApplicationState.Error -> {
                snackbarHostState.showSnackbar(
                    (withdrawApplicationState as WithdrawApplicationState.Error).message
                )
                viewModel.resetWithdrawApplicationState()
            }
            else -> {}
        }
    }

    // Clean up when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetWithdrawApplicationState()
        }
    }

    // Withdraw confirmation dialog
    if (showWithdrawDialog && selectedApplicationId != null) {
        val application = applications.find { it.id == selectedApplicationId }
        application?.let { app ->
            AlertDialog(
                onDismissRequest = { showWithdrawDialog = false },
                title = { Text("Withdraw Application") },
                text = {
                    Text("Are you sure you want to withdraw your application for '${app.jobTitle}'?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.withdrawApplication(selectedApplicationId!!)
                            showWithdrawDialog = false
                        }
                    ) {
                        Text("Withdraw", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWithdrawDialog = false }) {
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
                            text = "My Applications",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "${applications.size} total applications",
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    viewModel.refresh()
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

            if (isLoading) {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        com.example.workapp.ui.components.SkeletonApplicationCard()
                    }
                }
            } else if (applications.isEmpty()) {
                // Empty state
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Content.work,
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
                        text = "Apply to jobs to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    }
                }
            } else {
                // Applications list
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                items(applications) { application ->
                    MyApplicationCard(
                        application = application,
                        onClick = { onJobClick(application.jobId) },
                        onWithdraw = {
                            selectedApplicationId = application.id
                            showWithdrawDialog = true
                        }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card displaying application information for craftsman
 */
@Composable
private fun MyApplicationCard(
    application: JobApplication,
    onClick: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            // Header with job title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.jobTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "Posted by ${application.clientName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                // Status badge
                ApplicationStatusBadge(status = application.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Application details
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Job budget vs proposed price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
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
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
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
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Applied date
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Content.schedule,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(IconSizes.small),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Applied ${formatDate(application.appliedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Show response message if rejected
                if (application.status == ApplicationStatus.REJECTED && application.responseMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = application.responseMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Withdraw button for pending applications
                if (application.status == ApplicationStatus.PENDING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onWithdraw,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = AppIcons.Actions.close,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(IconSizes.small)
                        )
                        Text("Withdraw Application")
                    }
                }
            }
        }
    }
}

/**
 * Status badge component
 */
@Composable
private fun ApplicationStatusBadge(
    status: ApplicationStatus,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (status) {
        ApplicationStatus.PENDING -> MaterialTheme.colorScheme.tertiary to "Pending"
        ApplicationStatus.ACCEPTED -> MaterialTheme.colorScheme.secondary to "Accepted"
        ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error to "Rejected"
        ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) to "Withdrawn"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(statusColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = statusColor
        )
    }
}

/**
 * Format timestamp to readable date string
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