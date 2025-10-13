package com.voiceledger.ghana.presentation.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Language selection dialog
 */
@Composable
fun LanguageSelectionDialog(
    languages: List<Language>,
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Language",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(languages) { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language.code == selectedLanguage.code,
                                onClick = { onLanguageSelected(language) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language.code == selectedLanguage.code,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = language.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Market hours configuration dialog
 */
@Composable
fun MarketHoursDialog(
    startHour: Int,
    endHour: Int,
    onHoursSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStartHour by remember { mutableStateOf(startHour) }
    var selectedEndHour by remember { mutableStateOf(endHour) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Market Hours",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Set your market operating hours for automatic power management",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Start Hour Selector
                Text(
                    text = "Start Hour",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                HourSelector(
                    selectedHour = selectedStartHour,
                    onHourSelected = { selectedStartHour = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // End Hour Selector
                Text(
                    text = "End Hour",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                HourSelector(
                    selectedHour = selectedEndHour,
                    onHourSelected = { selectedEndHour = it }
                )
                
                if (selectedStartHour >= selectedEndHour) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start hour must be before end hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onHoursSelected(selectedStartHour, selectedEndHour) },
                enabled = selectedStartHour < selectedEndHour
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Hour selector component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourSelector(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = formatHour(selectedHour),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (0..23).forEach { hour ->
                DropdownMenuItem(
                    text = { Text(formatHour(hour)) },
                    onClick = {
                        onHourSelected(hour)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Voice enrollment dialog
 */
@Composable
fun VoiceEnrollmentDialog(
    isEnrolling: Boolean,
    enrollmentStep: VoiceEnrollmentStep,
    hasExistingProfile: Boolean,
    onStartEnrollment: () -> Unit,
    onCancelEnrollment: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = if (isEnrolling) onCancelEnrollment else onDismiss,
        title = {
            Text(
                text = if (hasExistingProfile) "Voice Profile" else "Voice Enrollment",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isEnrolling -> {
                        VoiceEnrollmentProgress(enrollmentStep)
                    }
                    hasExistingProfile -> {
                        VoiceProfileInfo()
                    }
                    else -> {
                        VoiceEnrollmentInstructions()
                    }
                }
            }
        },
        confirmButton = {
            when {
                isEnrolling -> {
                    TextButton(onClick = onCancelEnrollment) {
                        Text("Cancel")
                    }
                }
                hasExistingProfile -> {
                    Button(onClick = onStartEnrollment) {
                        Text("Re-enroll")
                    }
                }
                else -> {
                    Button(onClick = onStartEnrollment) {
                        Text("Start Enrollment")
                    }
                }
            }
        },
        dismissButton = {
            if (!isEnrolling) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun VoiceEnrollmentInstructions() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Voice enrollment helps improve transaction accuracy by learning your voice patterns.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You'll be asked to read a few sample phrases. This takes about 2 minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VoiceEnrollmentProgress(step: VoiceEnrollmentStep) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (step) {
            VoiceEnrollmentStep.RECORDING -> {
                Icon(
                    imageVector = Icons.Default.MicNone,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CircularProgressIndicator()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Recording your voice...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Please speak clearly into the microphone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            VoiceEnrollmentStep.PROCESSING -> {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CircularProgressIndicator()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Processing voice data...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            VoiceEnrollmentStep.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Green
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Enrollment Complete!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Green
                )
                
                Text(
                    text = "Your voice profile has been created successfully",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            VoiceEnrollmentStep.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Enrollment Failed",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = "Please try again in a quiet environment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            else -> {}
        }
    }
}

@Composable
private fun VoiceProfileInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Voice profile is active",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your voice is being used to improve transaction accuracy. You can re-enroll to update your profile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Data export confirmation dialog
 */
@Composable
fun DataExportDialog(
    isExporting: Boolean,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            if (isExporting) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preparing your data for export...")
                }
            } else {
                Text("Export all your transaction data and customer information to a CSV file?")
            }
        },
        confirmButton = {
            if (!isExporting) {
                Button(onClick = onExport) {
                    Text("Export")
                }
            }
        },
        dismissButton = {
            if (!isExporting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Data deletion confirmation dialog
 */
@Composable
fun DataDeleteDialog(
    isDeleting: Boolean,
    totalTransactions: Int,
    totalCustomers: Int,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete All Data",
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            if (isDeleting) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Deleting all data...")
                }
            } else {
                Column {
                    Text(
                        text = "This will permanently delete:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("• $totalTransactions transactions")
                    Text("• $totalCustomers customer profiles")
                    Text("• All transaction history")
                    Text("• All customer voice data")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "This action cannot be undone!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (!isDeleting) {
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            }
        },
        dismissButton = {
            if (!isDeleting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Data retention selection dialog
 */
@Composable
fun DataRetentionDialog(
    currentDays: Int,
    onDaysSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val retentionOptions = listOf(30, 60, 90, 180, 365)
    var selectedDays by remember { mutableStateOf(currentDays) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Data Retention Period") },
        text = {
            Column {
                Text(
                    text = "How long should transaction data be kept?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                retentionOptions.forEach { days ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = days == selectedDays,
                                onClick = { selectedDays = days }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = days == selectedDays,
                            onClick = { selectedDays = days }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$days days",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDaysSelected(selectedDays) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Data statistics card
 */
@Composable
fun DataStatisticsCard(
    totalTransactions: Int,
    totalCustomers: Int
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Data Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Transactions",
                    value = totalTransactions.toString(),
                    icon = Icons.Default.Receipt
                )
                
                StatItem(
                    label = "Customers",
                    value = totalCustomers.toString(),
                    icon = Icons.Default.People
                )
            }
        }
    }
}

/**
 * App information card
 */
@Composable
fun AppInfoCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            InfoRow("Version", "1.0.0")
            InfoRow("Build", "2024.01.01")
            InfoRow("Developer", "Ghana Voice Ledger Team")
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12:00 AM"
        hour < 12 -> "$hour:00 AM"
        hour == 12 -> "12:00 PM"
        else -> "${hour - 12}:00 PM"
    }
}