package com.example.workapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog for submitting a job application
 */
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

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column {
                Text(
                    text = "Apply for Job",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = jobTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show job budget if available
                jobBudget?.let { budget ->
                    Text(
                        text = "Client Budget: PEN ${String.format("%.0f", budget)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Estimated Duration (Optional)
                OutlinedTextField(
                    value = estimatedDuration,
                    onValueChange = { estimatedDuration = it },
                    label = { Text("Estimated Duration (Optional)") },
                    placeholder = { Text("e.g., 2 days, 1 week") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Cover Letter (Optional)
                OutlinedTextField(
                    value = coverLetter,
                    onValueChange = { coverLetter = it },
                    label = { Text("Cover Letter (Optional)") },
                    placeholder = { Text("Introduce yourself and explain why you're a great fit...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Availability (Optional)
                OutlinedTextField(
                    value = availability,
                    onValueChange = { availability = it },
                    label = { Text("Your Availability (Optional)") },
                    placeholder = { Text("e.g., Available immediately, Starting next week") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Text(
                    text = "All fields are optional. You can apply with just your profile or add details to stand out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text("Submit Application")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}