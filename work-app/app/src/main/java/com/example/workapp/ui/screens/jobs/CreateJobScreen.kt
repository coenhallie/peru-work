package com.example.workapp.ui.screens.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.workapp.ui.components.AddressAutofillTextField
import com.example.workapp.ui.components.JobCategorySelector
import com.example.workapp.ui.components.WorkAppTopBar
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.viewmodel.CreateJobState
import com.example.workapp.ui.viewmodel.JobViewModel

/**
 * Screen for creating a new job listing
 * Follows Material 3 design principles with proper form layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobScreen(
    modifier: Modifier = Modifier,
    viewModel: JobViewModel = hiltViewModel(),
    onJobCreated: () -> Unit = {}
) {
    val createJobState by viewModel.createJobState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var jobCategory by remember { mutableStateOf("") }
    var jobLocation by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris.map { it.toString() }
        }
    }

    // Handle job creation state
    LaunchedEffect(createJobState) {
        when (createJobState) {
            is CreateJobState.Success -> {
                snackbarHostState.showSnackbar("Job posted successfully!")
                viewModel.resetCreateJobState()
                onJobCreated()
            }
            is CreateJobState.Error -> {
                snackbarHostState.showSnackbar(
                    (createJobState as CreateJobState.Error).message
                )
                viewModel.resetCreateJobState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            WorkAppTopBar(
                title = "Post a Job"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Upload Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedImageUris.isNotEmpty()) {
                    // Display selected images in a horizontal row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedImageUris.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            ) {
                                AsyncImage(
                                    model = selectedImageUris[index],
                                    contentDescription = "Selected Image ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Remove button
                                IconButton(
                                    onClick = {
                                        selectedImageUris = selectedImageUris.toMutableList().apply { removeAt(index) }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Actions.close,
                                        contentDescription = "Remove Image",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        // Add button at the end if less than 5 images
                        if (selectedImageUris.size < 5) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = AppIcons.Actions.add,
                                            contentDescription = "Add more",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Add Photo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Button to change all images (reset)
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Select Different Photos")
                    }
                } else {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 32.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Content.work, // Placeholder icon
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Add Job Photos (Max 5)*")
                        }
                    }
                }
            }

            // Job Title
            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = { Text("Job Title*") },
                placeholder = { Text("e.g., Kitchen Renovation") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Job Category
            JobCategorySelector(
                selectedCategory = jobCategory,
                onCategorySelected = { jobCategory = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Job Location with Address Autofill
            AddressAutofillTextField(
                value = jobLocation,
                onValueChange = { jobLocation = it },
                label = "Location*",
                placeholder = "Start typing the job location...",
                modifier = Modifier.fillMaxWidth()
            )



            // Job Description
            OutlinedTextField(
                value = jobDescription,
                onValueChange = { jobDescription = it },
                label = { Text("Job Description*") },
                placeholder = { Text("Describe your project in detail:\n\n• What needs to be done? (e.g., renovate 15m² kitchen)\n• What is the purpose/goal? (e.g., modernize for better functionality)\n• Specific requirements? (e.g., materials, timeline, budget expectations)\n• Current condition and any special considerations?\n\nMore details help professionals provide accurate proposals.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.createJob(
                        title = jobTitle,
                        description = jobDescription,
                        category = jobCategory,
                        location = jobLocation,
                        imageUris = selectedImageUris
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(vertical = 16.dp),
                enabled = jobTitle.isNotBlank() &&
                         jobDescription.isNotBlank() &&
                         jobCategory.isNotBlank() &&
                         jobLocation.isNotBlank() &&
                         selectedImageUris.isNotEmpty() &&
                         createJobState !is CreateJobState.Loading &&
                         createJobState !is CreateJobState.Success
            ) {
                if (createJobState is CreateJobState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Post Job",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Help text
            Text(
                text = "* Required fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            // Extra bottom padding to ensure button is fully visible above bottom navigation
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}