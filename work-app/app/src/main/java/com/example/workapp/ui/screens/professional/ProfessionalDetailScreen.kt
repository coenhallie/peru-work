package com.example.workapp.ui.screens.professional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.workapp.R
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.workapp.data.model.User
import com.example.workapp.data.repository.ProfessionalRepository
import com.example.workapp.ui.components.FullScreenImageViewer
import com.example.workapp.ui.components.WorkAppTopBar
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.theme.StarYellow
import com.example.workapp.ui.viewmodel.AuthState
import com.example.workapp.ui.viewmodel.AuthViewModel
import com.example.workapp.ui.viewmodel.ChatViewModel
import com.example.workapp.ui.viewmodel.StartChatResult
import kotlinx.coroutines.launch

/**
 * Professional detail screen showing profile and booking options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalDetailScreen(
    professionalId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    repository: ProfessionalRepository = hiltViewModel<ProfessionalDetailViewModel>().repository,
    authViewModel: AuthViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    var professional by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Bottom Sheet state
    var showBottomSheet by remember { mutableStateOf(false) }
    var initialMessage by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    // Auth state
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Authenticated)?.user
    
    // Chat state
    val startChatResult by chatViewModel.startChatResult.collectAsState()
    val isChatLoading by chatViewModel.isLoading.collectAsState()
    
    // Handle chat result
    LaunchedEffect(startChatResult) {
        when (val result = startChatResult) {
            is StartChatResult.Success -> {
                chatViewModel.clearStartChatResult()
                onNavigateToChat(result.chatRoomId)
            }
            is StartChatResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                chatViewModel.clearStartChatResult()
            }
            null -> { /* No result yet */ }
        }
    }

    LaunchedEffect(professionalId) {
        scope.launch {
            repository.getProfessionalById(professionalId)
                .onSuccess {
                    professional = it
                    isLoading = false
                }
                .onFailure {
                    error = it.message
                    isLoading = false
                }
        }
    }

    // Full Screen Image Viewer
    if (selectedImageUrl != null) {
        FullScreenImageViewer(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
    }

    Scaffold(
        topBar = {
            WorkAppTopBar(
                title = stringResource(R.string.professional_role),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = AppIcons.Navigation.back,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(IconSizes.medium)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        com.example.workapp.ui.components.FadeInLoadingContent(
            isLoading = isLoading,
            modifier = Modifier.padding(padding),
            skeletonContent = {
                com.example.workapp.ui.components.SkeletonProfessionalDetail(
                    modifier = Modifier.fillMaxSize()
                )
            }
        ) {
            if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: stringResource(R.string.error_loading_professional),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (professional != null) {
                ProfessionalDetailContent(
                    professional = professional!!,
                    currentUser = currentUser,
                    isRequestingService = isChatLoading,
                    onRequestService = {
                        showBottomSheet = true
                    },
                    onImageClick = { url -> selectedImageUrl = url },
                    modifier = Modifier.fillMaxSize()
                )
                
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = sheetState
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .navigationBarsPadding() // Add padding for navigation bar
                                .imePadding() // Add padding for keyboard
                        ) {
                            Text(
                                text = stringResource(R.string.request_service_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = stringResource(R.string.start_conversation_desc, professional?.name ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            OutlinedTextField(
                                value = initialMessage,
                                onValueChange = { initialMessage = it },
                                label = { Text(stringResource(R.string.message_optional)) },
                                placeholder = { Text(stringResource(R.string.message_placeholder)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showBottomSheet = false }
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Button(
                                    onClick = {
                                        if (currentUser != null && professional != null) {
                                            chatViewModel.startChatWithProfessional(currentUser, professional!!, initialMessage)
                                            showBottomSheet = false
                                            initialMessage = "" // Reset message
                                        }
                                    },
                                    enabled = !isChatLoading
                                ) {
                                    if (isChatLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(stringResource(R.string.send_request))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalDetailContent(
    professional: User,
    currentUser: User?,
    isRequestingService: Boolean,
    onRequestService: () -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image
                AsyncImage(
                    model = professional.profileImageUrl ?: "https://via.placeholder.com/200",
                    contentDescription = stringResource(R.string.profile_desc_template, professional.name),
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable {
                            (professional.profileImageUrl ?: "https://via.placeholder.com/200").let(onImageClick)
                        },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = professional.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = professional.currentProfession ?: stringResource(R.string.professional_role),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Content.star,
                        contentDescription = stringResource(R.string.rating_desc),
                        tint = StarYellow,
                        modifier = Modifier.size(IconSizes.medium)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${professional.rating ?: 0.0}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.reviews_count, professional.reviewCount ?: 0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoChip(
                        icon = AppIcons.Content.work,
                        label = stringResource(R.string.experience),
                        value = stringResource(R.string.years_experience, (professional.experience ?: 0).toString())
                    )
                    InfoChip(
                        icon = AppIcons.Content.work,
                        label = stringResource(R.string.projects),
                        value = "${professional.completedProjects ?: 0}"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bio Section
        if (!professional.bio.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = professional.bio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Specialties Section
        if (!professional.specialties.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.specialties),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    professional.specialties.forEach { specialty ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = specialty,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Previous Projects Section
        if (!professional.previousJobs.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.previous_projects),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    professional.previousJobs.forEach { job ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = job.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            if (job.photoUrls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(job.photoUrls) { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = stringResource(R.string.project_photo),
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable { onImageClick(url) },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Contact Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.contact_information),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                ContactRow(
                    icon = AppIcons.Content.phone,
                    label = stringResource(R.string.phone),
                    value = professional.phone
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContactRow(
                    icon = AppIcons.Content.email,
                    label = stringResource(R.string.email),
                    value = professional.email
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContactRow(
                    icon = AppIcons.Content.place,
                    label = stringResource(R.string.location),
                    value = professional.location
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Request Service Button - only show for clients (non-professionals)
        // and not when viewing own profile
        if (currentUser != null &&
            !currentUser.isProfessional() &&
            currentUser.id != professional.id
        ) {
            Button(
                onClick = onRequestService,
                enabled = !isRequestingService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            ) {
                if (isRequestingService) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = AppIcons.Navigation.chat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRequestingService) stringResource(R.string.starting_chat) else stringResource(R.string.request_service_button),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// Simple ViewModel for dependency injection
@dagger.hilt.android.lifecycle.HiltViewModel
class ProfessionalDetailViewModel @javax.inject.Inject constructor(
    val repository: ProfessionalRepository
) : androidx.lifecycle.ViewModel()
