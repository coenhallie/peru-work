package com.example.workapp.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.workapp.data.model.Message
import com.example.workapp.data.model.MessageType
import com.example.workapp.data.model.Job
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.viewmodel.AuthViewModel
import com.example.workapp.ui.viewmodel.ChatViewModel
import com.example.workapp.ui.components.WorkAppTopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatRoomId: String,
    onNavigateBack: () -> Unit,
    onNavigateToJob: (String, String?) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val chatRooms by viewModel.chatRooms.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val availableJobs by viewModel.availableJobs.collectAsState()
    
    // Find current chat room to display title
    val currentRoom = chatRooms.find { it.id == chatRoomId }
    val isClient = currentUser?.id == currentRoom?.clientId
    val otherUserName = if (isClient) currentRoom?.professionalName else currentRoom?.clientName
    
    var messageText by remember { mutableStateOf("") }
    var showJobSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            viewModel.sendImageMessage(chatRoomId, selectedUri)
        }
    }

    LaunchedEffect(chatRoomId) {
        viewModel.loadMessages(chatRoomId)
        if (isClient) {
            viewModel.loadClientJobs()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            WorkAppTopBar(
                title = otherUserName ?: "Chat",
                subtitle = if (currentRoom != null) currentRoom.jobTitle else null,
                centered = false,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(AppIcons.Navigation.back, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentRoom != null && currentRoom.jobId.isNotEmpty()) {
                        TextButton(onClick = { onNavigateToJob(currentRoom.jobId, chatRoomId) }) {
                            Text("View Job")
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    viewModel.sendMessage(chatRoomId, messageText)
                    messageText = ""
                },
                onAttachImage = {
                    imagePickerLauncher.launch("image/*")
                },
                isUploading = isUploading,
                onAttachJob = if (isClient) { { showJobSheet = true } } else null
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            reverseLayout = true, // Show newest at bottom (which is top of list in reverse)
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isCurrentUser = message.senderId == currentUser?.id,
                    onAcceptJob = { jobId -> viewModel.acceptJobOffer(jobId, chatRoomId) },
                    onRejectJob = { jobId -> viewModel.rejectJobOffer(jobId, chatRoomId) },
                    isProfessional = !isClient,
                    onViewJob = { jobId -> onNavigateToJob(jobId, chatRoomId) }
                )
            }
        }
        
        if (showJobSheet) {
            JobSelectionBottomSheet(
                jobs = availableJobs,
                onJobSelected = { job ->
                    viewModel.sendJobOffer(chatRoomId, job)
                    showJobSheet = false
                },
                onDismiss = { showJobSheet = false }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onAcceptJob: (String) -> Unit = {},
    onRejectJob: (String) -> Unit = {},
    isProfessional: Boolean = false,
    onViewJob: (String) -> Unit = {}
) {
    if (message.type == MessageType.SYSTEM.name) {
        SystemMessage(message.message)
        return
    }

    if (message.type == MessageType.JOB_OFFER.name) {
        JobOfferBubble(
            message = message,
            isCurrentUser = isCurrentUser,
            onAccept = onAcceptJob,
            onReject = onRejectJob,
            isProfessional = isProfessional,
            onViewJob = onViewJob
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Display image if it's an IMAGE type message
                if (message.type == MessageType.IMAGE.name && message.attachmentUrl != null) {
                    AsyncImage(
                        model = message.attachmentUrl,
                        contentDescription = "Shared image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Add spacing if there's also text
                    if (message.message.isNotBlank() && message.message != "Image") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Display text if not just "Image" placeholder
                if (message.message.isNotBlank() &&
                    (message.type != MessageType.IMAGE.name || message.message != "Image")) {
                    Text(
                        text = message.message,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = formatMessageTime(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun JobOfferBubble(
    message: Message,
    isCurrentUser: Boolean,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    isProfessional: Boolean,
    onViewJob: (String) -> Unit = {}
) {
    val jobId = message.metadata?.get("jobId") ?: ""
    val jobTitle = message.metadata?.get("jobTitle") ?: "Unknown Job"
    // Budget removed as requested

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Job Offer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small photo placeholder or job icon
                    val jobImage = message.metadata?.get("jobImage")
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (!jobImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = jobImage,
                                contentDescription = "Job Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = jobTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { onViewJob(jobId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View Job")
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatMessageTime(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSelectionBottomSheet(
    jobs: List<Job>,
    onJobSelected: (Job) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select a Job to Offer",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (jobs.isEmpty()) {
                Text(
                    text = "No open jobs available.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.5f)
                ) {
                    items(jobs) { job ->
                        Card(
                            onClick = { onJobSelected(job) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Job Image
                                    val jobImage = job.imageUrl ?: job.images?.firstOrNull()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        if (!jobImage.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = jobImage,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Work,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Text(
                                        text = job.title,
                                        style = MaterialTheme.typography.titleMedium
                                    )
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
fun SystemMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachJob: (() -> Unit)? = null,
    isUploading: Boolean = false
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image attachment button
            IconButton(
                onClick = onAttachImage,
                enabled = !isUploading
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Attach image",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (onAttachJob != null) {
                IconButton(
                    onClick = onAttachJob,
                    enabled = !isUploading
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = "Offer Job",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        if (isUploading) "Uploading image..." else "Type a message..."
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = !isUploading
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = AppIcons.Actions.send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
