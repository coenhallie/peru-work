package com.example.workapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet for submitting a job application
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationSubmissionDialog(
    jobTitle: String,
    jobBudget: Double?,
    onDismiss: () -> Unit,
    onSubmit: (
        proposedPrice: String?,
        estimatedDuration: String?,
        coverLetter: String?,
        availability: String?
    ) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var proposedPrice by remember { mutableStateOf("") }
    var estimatedDuration by remember { mutableStateOf("") }
    var coverLetter by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf("") }
    
    var durationExpanded by remember { mutableStateOf(false) }
    var availabilityExpanded by remember { mutableStateOf(false) }
    
    val durationOptions = listOf(
        "1-2 days",
        "3-5 days",
        "1 week",
        "2 weeks",
        "3 weeks",
        "1 month",
        "2 months",
        "3+ months"
    )
    
    val availabilityOptions = listOf(
        "Available immediately",
        "Available in 1-2 days",
        "Available next week",
        "Available in 2 weeks",
        "Available in 1 month",
        "Not available soon"
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = sheetState,
        dragHandle = { if (!isLoading) BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Apply for Job",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = jobTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Show job budget if available
            jobBudget?.let { budget ->
                Text(
                    text = "Client Budget: PEN ${String.format("%.0f", budget)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Proposed Price (Optional)
            OutlinedTextField(
                value = proposedPrice,
                onValueChange = { proposedPrice = it },
                label = { Text("Your Proposed Price (Optional)") },
                placeholder = { Text("e.g., 500") },
                prefix = { Text("PEN ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isLoading
            )

            // Estimated Duration (Optional) - Dropdown
            ExposedDropdownMenuBox(
                expanded = durationExpanded && !isLoading,
                onExpandedChange = { if (!isLoading) durationExpanded = !durationExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = estimatedDuration,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Estimated Duration (Optional)") },
                    placeholder = { Text("Select duration") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = !isLoading),
                    enabled = !isLoading,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = durationExpanded && !isLoading,
                    onDismissRequest = { durationExpanded = false }
                ) {
                    durationOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                estimatedDuration = option
                                durationExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Cover Letter (Optional)
            OutlinedTextField(
                value = coverLetter,
                onValueChange = { coverLetter = it },
                label = { Text("Cover Letter (Optional)") },
                placeholder = { Text("Introduce yourself and explain why you're a great fit...") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isLoading
            )

            // Availability (Optional) - Dropdown
            ExposedDropdownMenuBox(
                expanded = availabilityExpanded && !isLoading,
                onExpandedChange = { if (!isLoading) availabilityExpanded = !availabilityExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = availability,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Your Availability (Optional)") },
                    placeholder = { Text("Select availability") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = availabilityExpanded)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = !isLoading),
                    enabled = !isLoading,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = availabilityExpanded && !isLoading,
                    onDismissRequest = { availabilityExpanded = false }
                ) {
                    availabilityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                availability = option
                                availabilityExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Text(
                text = "All fields are optional. You can apply with just your profile or add details to stand out.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Action buttons at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        onSubmit(
                            proposedPrice.takeIf { it.isNotBlank() },
                            estimatedDuration.takeIf { it.isNotBlank() },
                            coverLetter.takeIf { it.isNotBlank() },
                            availability.takeIf { it.isNotBlank() }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text("Submit")
                }
            }
        }
    }
}