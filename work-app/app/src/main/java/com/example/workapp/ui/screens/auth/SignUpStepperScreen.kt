package com.example.workapp.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.workapp.data.model.PreviousJob
import com.example.workapp.data.model.UserRole
import com.example.workapp.ui.components.AddressAutofillTextField
import com.example.workapp.ui.components.JobCategorySelector
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.viewmodel.AuthViewModel
import com.example.workapp.ui.viewmodel.EmailValidationState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import kotlin.math.roundToInt

@Composable
fun SignUpStepperScreen(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onSwitchToSignIn: () -> Unit,
    initialName: String = "",
    initialEmail: String = "",
    initialImageUri: Uri? = null,
    isGoogleSignIn: Boolean = false
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 9 // Max possible steps (added previous jobs step)

    // Form State
    var selectedRole by remember { mutableStateOf(UserRole.CLIENT) }
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var craft by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(initialImageUri) }
    var availability by remember { mutableStateOf("") }
    var workDistance by remember { mutableStateOf(10f) } // Default 10km
    
    // Previous jobs state for professionals
    var previousJobsList by remember { mutableStateOf<List<PreviousJobItem>>(emptyList()) }

    // Email validation state
    val emailValidationState by viewModel.emailValidationState.collectAsState()
    
    LaunchedEffect(emailValidationState) {
        if (emailValidationState is EmailValidationState.Valid) {
            currentStep++
            viewModel.resetEmailValidation()
        }
    }

    // Determine if current step is valid
    val isStepValid = when (currentStep) {
        0 -> true // Role selection is always valid (default selected)
        1 -> name.isNotBlank() && email.isNotBlank() && (isGoogleSignIn || password.isNotBlank())
        2 -> phone.isNotBlank()
        3 -> location.isNotBlank()
        4 -> if (selectedRole == UserRole.PROFESSIONAL) craft.isNotBlank() && bio.isNotBlank() else true
        5 -> true // Previous jobs are optional
        6 -> true // Profile photo is optional
        7 -> if (selectedRole == UserRole.PROFESSIONAL) availability.isNotBlank() else true
        8 -> true // Distance has default
        else -> false
    }

    // Calculate progress
    val progress = (currentStep + 1).toFloat() / if (selectedRole == UserRole.CLIENT) 5 else 9

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar with Back Button and Progress
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                IconButton(onClick = { currentStep-- }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                IconButton(onClick = onSwitchToSignIn) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Sign In")
                }
            }
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "${currentStep + 1}/${if (selectedRole == UserRole.CLIENT) 5 else 9}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Content Area with Animation
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(
                        // Disable clipping since the content may be larger than the container
                        // during the transition
                        sizeTransform = null
                    )
                },
                label = "Stepper Animation"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp), // minimal padding as parent has padding
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    when (step) {
                        0 -> RoleSelectionStep(
                            selectedRole = selectedRole,
                            onRoleSelected = { selectedRole = it }
                        )
                        1 -> BasicInfoStep(
                            name = name,
                            onNameChange = { name = it },
                            email = email,
                            onEmailChange = { 
                                email = it 
                                if (emailValidationState is EmailValidationState.Invalid) {
                                    viewModel.resetEmailValidation()
                                }
                            },
                            password = password,
                            onPasswordChange = { password = it },
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityChange = { passwordVisible = it },
                            showPassword = !isGoogleSignIn,
                            emailError = (emailValidationState as? EmailValidationState.Invalid)?.reason
                        )
                        2 -> ContactStep(
                            phone = phone,
                            onPhoneChange = { phone = it }
                        )
                        3 -> LocationStep(
                            location = location,
                            onLocationChange = { location = it }
                        )
                        4 -> if (selectedRole == UserRole.PROFESSIONAL) {
                            ProfessionalDetailsStep(
                                craft = craft,
                                onCraftChange = { craft = it },
                                bio = bio,
                                onBioChange = { bio = it }
                            )
                        } else {
                            // Skip for client - go to profile photo
                            ProfilePhotoUploadStep(imageUri, { imageUri = it })
                        }
                        5 -> if (selectedRole == UserRole.PROFESSIONAL) {
                            PreviousJobsStep(
                                previousJobs = previousJobsList,
                                onPreviousJobsChange = { previousJobsList = it }
                            )
                        } else {
                            // End for client (should not reach here via logic but for safety)
                            Text("Ready to finish!")
                        }
                        6 -> if (selectedRole == UserRole.PROFESSIONAL) {
                            ProfilePhotoUploadStep(imageUri, { imageUri = it })
                        } else {
                            Text("Ready to finish!")
                        }
                        7 -> AvailabilityStep(
                            availability = availability,
                            onAvailabilityChange = { availability = it }
                        )
                        8 -> WorkDistanceStep(
                            distance = workDistance,
                            onDistanceChange = { workDistance = it }
                        )
                    }
                }
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Button(
                onClick = {
                    if (currentStep == 1) {
                        viewModel.validateEmail(email)
                    } else {
                        val maxSteps = if (selectedRole == UserRole.CLIENT) 4 else 8
                        if (currentStep < maxSteps) {
                            currentStep++
                        } else {
                            // Submit - convert previousJobsList to actual data
                            val previousJobs = if (selectedRole == UserRole.PROFESSIONAL && previousJobsList.isNotEmpty()) {
                                previousJobsList
                            } else {
                                null
                            }
                            
                            if (isGoogleSignIn) {
                                viewModel.completeProfile(
                                    email = email.trim(),
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    location = location.trim(),
                                    role = selectedRole,
                                    craft = if (selectedRole == UserRole.PROFESSIONAL) craft.trim() else null,
                                    bio = if (selectedRole == UserRole.PROFESSIONAL) bio.trim() else null,
                                    workDistance = if (selectedRole == UserRole.PROFESSIONAL) workDistance.roundToInt() else null,
                                    imageUri = imageUri,
                                    previousJobs = previousJobs
                                )
                            } else {
                                viewModel.signUp(
                                    email = email.trim(),
                                    password = password,
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    location = location.trim(),
                                    role = selectedRole,
                                    craft = if (selectedRole == UserRole.PROFESSIONAL) craft.trim() else null,
                                    bio = if (selectedRole == UserRole.PROFESSIONAL) bio.trim() else null,
                                    workDistance = if (selectedRole == UserRole.PROFESSIONAL) workDistance.roundToInt() else null,
                                    imageUri = imageUri,
                                    previousJobs = previousJobs
                                )
                            }
                        }
                    }
                },
                enabled = isStepValid && !isLoading && emailValidationState !is EmailValidationState.Validating,
                modifier = Modifier.width(120.dp)
            ) {
                if (isLoading || emailValidationState is EmailValidationState.Validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    val isLastStep = if (selectedRole == UserRole.CLIENT) currentStep == 4 else currentStep == 8
                    Text(if (isLastStep) "Finish" else "Next")
                }
            }
        }
    }
}

// --- Step Composables ---

@Composable
fun RoleSelectionStep(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Who are you?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose how you want to use the app",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RoleCard(
                role = UserRole.CLIENT,
                isSelected = selectedRole == UserRole.CLIENT,
                onClick = { onRoleSelected(UserRole.CLIENT) },
                modifier = Modifier.weight(1f)
            )
            RoleCard(
                role = UserRole.PROFESSIONAL,
                isSelected = selectedRole == UserRole.PROFESSIONAL,
                onClick = { onRoleSelected(UserRole.PROFESSIONAL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RoleCard(
    role: UserRole,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (role == UserRole.CLIENT) Icons.Default.Person else AppIcons.Form.work,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (role == UserRole.CLIENT) "Client" else "Professional",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BasicInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    showPassword: Boolean = true,
    emailError: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            leadingIcon = { Icon(AppIcons.Form.person, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(AppIcons.Form.email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError) }
        )

        if (showPassword) {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                leadingIcon = { Icon(AppIcons.Form.lock, null) },
                trailingIcon = {
                    IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                        Icon(
                            imageVector = if (passwordVisible) AppIcons.Form.visibility else AppIcons.Form.visibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun ContactStep(
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Contact Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text("How can people reach you?")

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            leadingIcon = { Icon(AppIcons.Form.phone, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun LocationStep(
    location: String,
    onLocationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Where are you based?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        AddressAutofillTextField(
            value = location,
            onValueChange = onLocationChange,
            label = "Address",
            placeholder = "Start typing your address...",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProfessionalDetailsStep(
    craft: String,
    onCraftChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Professional Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Use JobCategorySelector for craft selection
        JobCategorySelector(
            selectedCategory = craft,
            onCategorySelected = onCraftChange,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            placeholder = { Text("Tell us about your experience...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
    }
}

@Composable
fun ProfilePhotoUploadStep(
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Add a Profile Photo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "This will be your main profile picture that clients see",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to upload")
                }
            }
        }
        
        Text(
            text = "Optional - You can skip this step",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Data class to hold previous job item before upload
data class PreviousJobItem(
    val description: String = "",
    val photoUris: List<Uri> = emptyList()
)

@Composable
fun PreviousJobsStep(
    previousJobs: List<PreviousJobItem>,
    onPreviousJobsChange: (List<PreviousJobItem>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Previous Work Portfolio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Showcase your previous projects to help clients see your work quality",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        // List of previous jobs
        if (previousJobs.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                previousJobs.forEachIndexed { index, job ->
                    PreviousJobCard(
                        job = job,
                        onDelete = {
                            onPreviousJobsChange(previousJobs.toMutableList().apply { removeAt(index) })
                        }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = AppIcons.Form.work,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No previous projects added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Add button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = previousJobs.size < 10 // Limit to 10 projects
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Previous Project")
        }
        
        Text(
            text = "Optional - You can skip this step or add up to 10 projects",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    // Add job dialog
    if (showAddDialog) {
        AddPreviousJobDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newJob ->
                onPreviousJobsChange(previousJobs + newJob)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PreviousJobCard(
    job: PreviousJobItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = job.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )
                    
                    if (job.photoUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            job.photoUris.forEach { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Project photo",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPreviousJobDialog(
    onDismiss: () -> Unit,
    onAdd: (PreviousJobItem) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // Take maximum 2 photos
        selectedPhotoUris = (selectedPhotoUris + uris).take(2)
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Text(
                text = "Add Previous Project",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Share details about a project you've completed to showcase your work",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Project Description") },
                placeholder = { Text("e.g., Kitchen renovation in Amsterdam - replaced cabinets, countertops, and installed new appliances") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 6,
                supportingText = {
                    Text("${description.length}/500 characters")
                }
            )
            
            // Photos section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Project Photos (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Add up to 2 photos showing your work (${selectedPhotoUris.size}/2)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Photo grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    selectedPhotoUris.forEach { uri ->
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Project photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    selectedPhotoUris = selectedPhotoUris.filter { it != uri }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Add photo button
                    if (selectedPhotoUris.size < 2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { photoLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = "Add photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Add Photo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        if (description.isNotBlank()) {
                            onAdd(PreviousJobItem(description.take(500), selectedPhotoUris))
                        }
                    },
                    enabled = description.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Project")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityStep(
    availability: String,
    onAvailabilityChange: (String) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val availabilityOptions = listOf(
        "Weekdays (9 AM - 5 PM)",
        "Weekends Only",
        "Evenings (After 5 PM)",
        "Flexible / On Request",
        "Full Time (24/7 Emergency)"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Availability",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text("When are you usually available to work?")

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = availability,
                onValueChange = {},
                readOnly = true,
                label = { Text("Availability") },
                placeholder = { Text("Select your availability") },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showBottomSheet = true }
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Select Availability",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn {
                        items(availabilityOptions) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAvailabilityChange(option)
                                        showBottomSheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (option == availability) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (option == availability) FontWeight.Bold else FontWeight.Normal
                                )
                                if (option == availability) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkDistanceStep(
    distance: Float,
    onDistanceChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = "Work Distance",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text("How far are you willing to travel for work?")

        Column {
            Text(
                text = "${distance.roundToInt()} km",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = distance,
                onValueChange = onDistanceChange,
                valueRange = 1f..100f,
                steps = 99
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 km")
                Text("100 km")
            }
        }
    }
}
