package com.example.workapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes

enum class ApplicationActionType {
    ACCEPT,
    REJECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationActionBottomSheet(
    actionType: ApplicationActionType,
    applicantName: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var message by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = if (actionType == ApplicationActionType.ACCEPT) 
                        AppIcons.Actions.check else AppIcons.Actions.close,
                    contentDescription = null,
                    tint = if (actionType == ApplicationActionType.ACCEPT) 
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(IconSizes.large)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (actionType == ApplicationActionType.ACCEPT) 
                        "Accept Proposal" else "Reject Proposal",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Description
            Text(
                text = if (actionType == ApplicationActionType.ACCEPT) 
                    "You are about to accept $applicantName's proposal. This will assign the job to them." 
                else 
                    "You are about to reject $applicantName's proposal.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Input Field
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { 
                    Text(
                        if (actionType == ApplicationActionType.ACCEPT) 
                            "Introductory Message (Optional)" 
                        else 
                            "Reason for rejection (Optional)"
                    ) 
                },
                placeholder = { 
                    Text(
                        if (actionType == ApplicationActionType.ACCEPT) 
                            "Hi $applicantName, I'd like to hire you..." 
                        else 
                            "e.g., Price is outside my budget..."
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = { onConfirm(message.takeIf { it.isNotBlank() }) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == ApplicationActionType.ACCEPT) 
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        if (actionType == ApplicationActionType.ACCEPT) "Accept" else "Reject"
                    )
                }
            }
        }
    }
}
