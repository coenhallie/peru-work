package com.example.workapp.ui.screens.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.components.WorkAppTopBar
import com.example.workapp.ui.viewmodel.JobViewModel
import com.example.workapp.ui.viewmodel.UpdateJobState

/**
 * Screen for editing an existing job listing
 * Follows Material 3 design principles with proper form layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJobScreen(
    jobId: String,
    modifier: Modifier = Modifier,
    viewModel: JobViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val updateJobState by viewModel.updateJobState.collectAsState()
    val currentJob by viewModel.currentJob.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var jobCategory by remember { mutableStateOf("") }
    var jobLocation by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }

    // Load the job when screen opens
    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    // Initialize form fields when job is loaded
    LaunchedEffect(currentJob) {
        currentJob?.let { job ->
            if (!isInitialized) {
                jobTitle = job.title
                jobDescription = job.description
                jobCategory = job.category
                jobLocation = job.location
                isInitialized = true
            }
        }
    }

    // Handle job update state
    LaunchedEffect(updateJobState) {
        when (updateJobState) {
            is UpdateJobState.Success -> {
                snackbarHostState.showSnackbar("Job updated successfully!")
                viewModel.resetUpdateJobState()
                viewModel.clearCurrentJob()
                onNavigateBack()
            }
            is UpdateJobState.Error -> {
                snackbarHostState.showSnackbar(
                    (updateJobState as UpdateJobState.Error).message
                )
                viewModel.resetUpdateJobState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            WorkAppTopBar(
                title = "Edit Job",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = AppIcons.Navigation.back,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (currentJob == null && !isInitialized) {
            // Loading state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading job details...")
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                OutlinedTextField(
                    value = jobCategory,
                    onValueChange = { jobCategory = it },
                    label = { Text("Category*") },
                    placeholder = { Text("e.g., Plumbing, Electrical, Carpentry") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                // Job Location
                OutlinedTextField(
                    value = jobLocation,
                    onValueChange = { jobLocation = it },
                    label = { Text("Location*") },
                    placeholder = { Text("e.g., Lima, Cusco, Arequipa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )



                // Job Description
                OutlinedTextField(
                    value = jobDescription,
                    onValueChange = { jobDescription = it },
                    label = { Text("Job Description*") },
                    placeholder = { Text("Describe the work needed in detail...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Update Button
                Button(
                    onClick = {
                        viewModel.updateJob(
                            jobId = jobId,
                            title = jobTitle,
                            description = jobDescription,
                            category = jobCategory,
                            location = jobLocation
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    enabled = jobTitle.isNotBlank() &&
                             jobDescription.isNotBlank() &&
                             jobCategory.isNotBlank() &&
                             jobLocation.isNotBlank() &&
                             updateJobState !is UpdateJobState.Loading
                ) {
                    if (updateJobState is UpdateJobState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Update Job",
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
            }
        }
    }
}