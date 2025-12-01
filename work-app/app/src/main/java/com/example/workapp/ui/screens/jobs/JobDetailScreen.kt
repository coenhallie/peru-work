package com.example.workapp.ui.screens.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.workapp.data.model.Job
import com.example.workapp.data.model.JobStatus
import com.example.workapp.ui.components.ApplicationSubmissionBottomSheet
import com.example.workapp.ui.components.FullScreenImageViewer
import com.example.workapp.ui.components.JobImage
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.viewmodel.ApplicationViewModel
import com.example.workapp.ui.viewmodel.JobViewModel
import com.example.workapp.ui.viewmodel.SubmitApplicationState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.example.workapp.BuildConfig
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.width
/**
 * Screen displaying detailed job information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    jobId: String,
    currentUserId: String?,
    isProfessional: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToApplications: (String) -> Unit,
    chatRoomId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: JobViewModel = hiltViewModel(),
    applicationViewModel: ApplicationViewModel = hiltViewModel()
) {
    val job by viewModel.currentJob.collectAsState()
    val jobCoordinates by viewModel.jobCoordinates.collectAsState()
    val hasApplied by applicationViewModel.hasApplied.collectAsState()
    val submitApplicationState by applicationViewModel.submitApplicationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var showApplicationBottomSheet by remember { mutableStateOf(false) }
    var isMapVisible by remember { mutableStateOf(true) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    // Check application status when screen is opened
    LaunchedEffect(jobId, isProfessional) {
        if (isProfessional) {
            applicationViewModel.checkIfApplied(jobId)
        }
    }
    
    // Handle application submission state
    LaunchedEffect(submitApplicationState) {
        when (submitApplicationState) {
            is SubmitApplicationState.Success -> {
                showApplicationBottomSheet = false
                applicationViewModel.resetSubmitApplicationState()
                applicationViewModel.checkIfApplied(jobId)
                snackbarHostState.showSnackbar("Application submitted successfully!")
            }
            is SubmitApplicationState.Error -> {
                // If dialog is not shown, show snackbar (fallback)
                if (!showApplicationBottomSheet) {
                    snackbarHostState.showSnackbar(
                        (submitApplicationState as SubmitApplicationState.Error).message
                    )
                    applicationViewModel.resetSubmitApplicationState()
                }
                // If dialog IS shown, we don't reset state yet so dialog can show error
            }
            else -> {}
        }
    }

    // Clean up when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            // We don't clear current job here as it's handled by ViewModel's SavedStateHandle
            // But we might want to clear it if we want fresh data on next visit
            // viewModel.clearCurrentJob()
            applicationViewModel.resetHasAppliedState()
            applicationViewModel.resetSubmitApplicationState()
        }
    }

    // Handle navigation back with map visibility cleanup
    val handleNavigateBack = {
        isMapVisible = false
        onNavigateBack()
    }
    
    // Show application submission bottom sheet
    if (showApplicationBottomSheet && job != null) {
        val errorMessage = (submitApplicationState as? SubmitApplicationState.Error)?.message
        
        ApplicationSubmissionBottomSheet(
            jobTitle = job!!.title,
            jobBudget = job!!.budget,
            onDismiss = {
                showApplicationBottomSheet = false
                applicationViewModel.resetSubmitApplicationState()
            },
            onSubmit = { price, duration, coverLetter, availability ->
                applicationViewModel.submitApplication(
                    jobId = job!!.id,
                    jobTitle = job!!.title,
                    jobBudget = job!!.budget,
                    clientId = job!!.clientId,
                    clientName = job!!.clientName,
                    proposedPrice = price,
                    estimatedDuration = duration,
                    coverLetter = coverLetter,
                    availability = availability,
                    chatRoomId = chatRoomId
                )
                showApplicationBottomSheet = false
            },
            isLoading = submitApplicationState is SubmitApplicationState.Loading,
            error = errorMessage
        )
    }

    // Full Screen Image Viewer
    if (selectedImageUrl != null) {
        FullScreenImageViewer(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Debug logging
            LaunchedEffect(job, currentUserId) {
                if (job != null) {
                    println("DEBUG_FAB: JobId=${job!!.id}")
                    println("DEBUG_FAB: AppCount=${job!!.applicationCount}")
                    println("DEBUG_FAB: ClientId=${job!!.clientId}")
                    println("DEBUG_FAB: CurrentUserId=$currentUserId")
                    println("DEBUG_FAB: IsOwner=${job!!.clientId == currentUserId}")
                    println("DEBUG_FAB: IsProfessional=$isProfessional")
                } else {
                    println("DEBUG_FAB: Job is null")
                }
            }

            // Show FAB for CLIENT viewing their own job with applications
            if (job != null && job!!.applicationCount > 0) {
                val isJobOwner = currentUserId != null &&
                                currentUserId.isNotEmpty() &&
                                job!!.clientId == currentUserId
                
                if (isJobOwner) {
                    val isAccepted = job!!.status == JobStatus.ACCEPTED
                    val fabText = if (isAccepted) "View Applications" else "Review (${job!!.applicationCount})"
                    
                    ExtendedFloatingActionButton(
                        onClick = { onNavigateToApplications(jobId) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            imageVector = AppIcons.Content.person,
                            contentDescription = if (isAccepted) "View Applications" else "Review Applications",
                            modifier = Modifier.size(IconSizes.medium)
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = fabText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Job Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleNavigateBack) {
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
            isLoading = job == null,
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            skeletonContent = {
                com.example.workapp.ui.components.SkeletonJobDetail(
                    modifier = Modifier.fillMaxSize()
                )
            }
        ) {
            // Job details content
            JobDetailContent(
                job = job!!,
                jobCoordinates = jobCoordinates,
                currentUserId = currentUserId,
                isProfessional = isProfessional,
                hasApplied = hasApplied,
                applicationCount = job!!.applicationCount,
                onApply = { showApplicationBottomSheet = true },
                onViewApplications = { onNavigateToApplications(jobId) },
                chatRoomId = chatRoomId,
                isMapVisible = isMapVisible,
                onImageClick = { url -> selectedImageUrl = url },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Content displaying all job details
 */
@Composable
private fun JobDetailContent(
    job: Job,
    jobCoordinates: Point?,
    currentUserId: String?,
    isProfessional: Boolean,
    hasApplied: Boolean,
    applicationCount: Int,
    onApply: () -> Unit,
    onViewApplications: () -> Unit,
    isMapVisible: Boolean,
    onImageClick: (String) -> Unit,
    chatRoomId: String? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {


        // Job Images Carousel
        val images = remember(job) {
            when {
                !job.images.isNullOrEmpty() -> job.images
                !job.imageUrl.isNullOrEmpty() -> listOf(job.imageUrl)
                else -> emptyList()
            }
        }

        if (images.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                val pagerState = rememberPagerState(pageCount = { images.size })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Calculate the absolute offset for the current page from the
                                // scroll position. We use the absolute value which allows us to mirror
                                // any effects for both directions
                                val pageOffset = (
                                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                )

                                // Scale effect: shrink the image slightly as it moves away from center
                                val scale = 1f - (0.15f * kotlin.math.abs(pageOffset))
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        JobImage(
                            imageUrl = images[page],
                            category = job.category,
                            modifier = Modifier.fillMaxSize(),
                            onClick = onImageClick
                        )
                    }
                }

                // Page Indicator
                if (images.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(images.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 16.dp else 6.dp,
                                label = "indicatorWidth"
                            )
                            val color by animateColorAsState(
                                targetValue = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                label = "indicatorColor"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(color)
                                    .height(6.dp)
                                    .width(width)
                            )
                        }
                    }
                }
            }
        } else {
            // Fallback for no images
            JobImage(
                imageUrl = null,
                category = job.category,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                onClick = onImageClick
            )
        }

        // Job information
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title and Category Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Content.work,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(IconSizes.medium),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = job.category,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Status badge
                    StatusBadge(status = job.status)
                }
            }

            // Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Content.description,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(IconSizes.medium),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = job.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Budget and Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Job Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Budget
                    job.budget?.let { budget ->
                        DetailRow(
                            icon = AppIcons.Content.payment,
                            label = "Budget",
                            value = "PEN ${String.format("%.0f", budget)}",
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Location
                    DetailRow(
                        icon = AppIcons.Content.location,
                        label = "Location",
                        value = job.location
                    )
                    
                    // Deadline (if available)
                    job.deadline?.let { deadline ->
                        DetailRow(
                            icon = AppIcons.Content.schedule,
                            label = "Deadline",
                            value = deadline
                        )
                    }
                    
                    // Posted date
                    DetailRow(
                        icon = AppIcons.Content.schedule,
                        label = "Posted",
                        value = formatDate(job.createdAt)
                    )
                }
            }

            // Map section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    if (isMapVisible) {
                        LocationMapSection(
                            location = job.location,
                            coordinates = jobCoordinates,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Client Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Client Information",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    DetailRow(
                        icon = AppIcons.Content.person,
                        label = "Posted by",
                        value = job.clientName
                    )
                    
                    // Show professional info if assigned
                    job.professionalName?.let { professionalName ->
                        DetailRow(
                            icon = AppIcons.Content.work,
                            label = "Assigned to",
                            value = professionalName
                        )
                    }
                }
            }

            // Additional notes if available
            job.notes?.let { notes ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Content.description,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(IconSizes.medium),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Additional Notes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action buttons based on user role and job status
            if (isProfessional && job.status == JobStatus.OPEN && job.clientId != currentUserId) {
                // Professional viewing open job
                if (hasApplied) {
                    // Already applied - show status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Content.schedule,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(IconSizes.medium),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Application Submitted - Pending Review",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else {
                    // Can apply
                    Button(
                        onClick = onApply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = AppIcons.Actions.send,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(IconSizes.medium)
                        )
                        Text(
                            text = "Apply for this Job",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            } else if (!isProfessional && job.clientId == currentUserId && applicationCount > 0) {
                // Client viewing their job with applications - Button moved to sticky bottom bar
            }

            // Spacing at bottom
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Map section showing job location using Mapbox
 * Only displays if Mapbox token is configured
 */
@Composable
private fun LocationMapSection(
    location: String,
    coordinates: Point?,
    modifier: Modifier = Modifier
) {
    // Check if Mapbox token is available
    val hasMapboxToken = BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotEmpty()
    
    if (hasMapboxToken) {
        // Use fetched coordinates or default to Lima, Peru
        val mapLocation = coordinates ?: Point.fromLngLat(-77.0428, -12.0464) // Lima, Peru (lng, lat)
        
        // Capture colors in Composable context
        val markerColor = MaterialTheme.colorScheme.primary
        val strokeColor = MaterialTheme.colorScheme.onPrimary
        
        val mapViewportState = rememberMapViewportState {
            setCameraOptions {
                center(mapLocation)
                zoom(12.0)
                pitch(0.0)
            }
        }
        
        // Update camera when coordinates change
        LaunchedEffect(coordinates) {
            if (coordinates != null) {
                mapViewportState.flyTo(
                    CameraOptions.Builder()
                        .center(coordinates)
                        .zoom(12.0)
                        .build()
                )
            }
        }

        Box(
            modifier = modifier
        ) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                style = {
                    MapStyle(style = "mapbox://styles/mapbox/streets-v12")
                }
            ) {
                // Add a circle annotation to mark the job location
                CircleAnnotation(
                    point = mapLocation
                ) {
                    circleRadius = 10.0
                    circleColor = markerColor
                    circleStrokeWidth = 2.0
                    circleStrokeColor = strokeColor
                }
            }
            
            // Location label overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Content.location,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(IconSizes.small),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    } else {
        // Fallback UI when Mapbox is not configured - show location as text only
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Content.location,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(IconSizes.large),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Status badge component
 */
@Composable
private fun StatusBadge(
    status: JobStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        JobStatus.OPEN -> MaterialTheme.colorScheme.primary
        JobStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        JobStatus.ACCEPTED -> MaterialTheme.colorScheme.secondary
        JobStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        JobStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        JobStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
    
    val statusText = when (status) {
        JobStatus.OPEN -> "Open"
        JobStatus.PENDING -> "Pending"
        JobStatus.ACCEPTED -> "Accepted"
        JobStatus.IN_PROGRESS -> "In Progress"
        JobStatus.COMPLETED -> "Completed"
        JobStatus.CANCELLED -> "Cancelled"
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
 * Detail row component
 */
@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
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
            color = valueColor,
            modifier = Modifier.padding(start = 32.dp)
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