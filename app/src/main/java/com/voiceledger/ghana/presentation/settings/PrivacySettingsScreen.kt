package com.voiceledger.ghana.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.R
import com.voiceledger.ghana.security.*

/**
 * Privacy settings screen for managing data protection preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = stringResource(R.string.privacy_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Consent Status Card
            item {
                ConsentStatusCard(
                    consentState = uiState.consentState,
                    onUpdateConsent = { viewModel.updateConsent() },
                    onRevokeConsent = { viewModel.revokeConsent() }
                )
            }
            
            // Data Processing Settings
            item {
                DataProcessingSettingsCard(
                    settings = uiState.privacySettings,
                    onSettingsChange = { viewModel.updatePrivacySettings(it) }
                )
            }
            
            // Data Retention Settings
            item {
                DataRetentionSettingsCard(
                    settings = uiState.privacySettings,
                    onSettingsChange = { viewModel.updatePrivacySettings(it) }
                )
            }
            
            // Privacy Compliance Report
            item {
                PrivacyComplianceCard(
                    complianceReport = uiState.complianceReport,
                    onRefreshReport = { viewModel.refreshComplianceReport() }
                )
            }
            
            // Data Export/Delete Actions
            item {
                DataManagementCard(
                    onExportData = { viewModel.exportUserData() },
                    onDeleteAllData = { viewModel.showDeleteConfirmation() }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmation() },
            title = { Text("Delete All Data") },
            text = { 
                Text("This will permanently delete all your data including transactions, voice profiles, and settings. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAllUserData() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Loading indicator
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    
    // Error snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar with error message
            viewModel.clearError()
        }
    }
}

@Composable
private fun ConsentStatusCard(
    consentState: ConsentState,
    onUpdateConsent: () -> Unit,
    onRevokeConsent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Consent Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = if (consentState.hasValidConsent) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (consentState.hasValidConsent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    consentState.hasValidConsent -> "You have provided valid consent for data processing"
                    consentState.needsUpdate -> "Your consent needs to be updated"
                    consentState.isExpired -> "Your consent has expired"
                    else -> "No consent provided"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!consentState.hasValidConsent) {
                    Button(
                        onClick = onUpdateConsent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update Consent")
                    }
                } else {
                    OutlinedButton(
                        onClick = onRevokeConsent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Revoke Consent")
                    }
                }
            }
        }
    }
}

@Composable
private fun DataProcessingSettingsCard(
    settings: PrivacySettings,
    onSettingsChange: (PrivacySettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Data Processing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsSwitch(
                title = "Voice Processing",
                description = "Allow processing of voice recordings for transaction recognition",
                checked = settings.allowVoiceProcessing,
                onCheckedChange = { onSettingsChange(settings.copy(allowVoiceProcessing = it)) }
            )
            
            SettingsSwitch(
                title = "Data Storage",
                description = "Store transaction data locally on device",
                checked = settings.allowDataStorage,
                onCheckedChange = { onSettingsChange(settings.copy(allowDataStorage = it)) }
            )
            
            SettingsSwitch(
                title = "Analytics",
                description = "Collect anonymous usage analytics to improve the app",
                checked = settings.allowAnalytics,
                onCheckedChange = { onSettingsChange(settings.copy(allowAnalytics = it)) }
            )
            
            SettingsSwitch(
                title = "Cloud Sync",
                description = "Sync data with cloud services for backup",
                checked = settings.allowCloudSync,
                onCheckedChange = { onSettingsChange(settings.copy(allowCloudSync = it)) }
            )
            
            SettingsSwitch(
                title = "Speaker Identification",
                description = "Identify speakers to personalize experience",
                checked = settings.allowSpeakerIdentification,
                onCheckedChange = { onSettingsChange(settings.copy(allowSpeakerIdentification = it)) }
            )
            
            SettingsSwitch(
                title = "Encrypt Sensitive Data",
                description = "Encrypt sensitive data for additional security",
                checked = settings.encryptSensitiveData,
                onCheckedChange = { onSettingsChange(settings.copy(encryptSensitiveData = it)) }
            )
        }
    }
}

@Composable
private fun DataRetentionSettingsCard(
    settings: PrivacySettings,
    onSettingsChange: (PrivacySettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Data Retention",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            RetentionSlider(
                title = "Voice Recordings",
                value = settings.voiceRecordingRetentionDays,
                onValueChange = { onSettingsChange(settings.copy(voiceRecordingRetentionDays = it)) },
                maxValue = 365
            )
            
            RetentionSlider(
                title = "Transaction Data",
                value = settings.transactionRetentionDays,
                onValueChange = { onSettingsChange(settings.copy(transactionRetentionDays = it)) },
                maxValue = 1095 // 3 years
            )
            
            RetentionSlider(
                title = "Analytics Data",
                value = settings.analyticsRetentionDays,
                onValueChange = { onSettingsChange(settings.copy(analyticsRetentionDays = it)) },
                maxValue = 365
            )
            
            RetentionSlider(
                title = "Speaker Profiles",
                value = settings.speakerProfileRetentionDays,
                onValueChange = { onSettingsChange(settings.copy(speakerProfileRetentionDays = it)) },
                maxValue = 730 // 2 years
            )
            
            SettingsSwitch(
                title = "Automatic Data Deletion",
                description = "Automatically delete data when retention period expires",
                checked = settings.automaticDataDeletion,
                onCheckedChange = { onSettingsChange(settings.copy(automaticDataDeletion = it)) }
            )
        }
    }
}

@Composable
private fun PrivacyComplianceCard(
    complianceReport: PrivacyComplianceReport?,
    onRefreshReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy Compliance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onRefreshReport) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
            
            if (complianceReport != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Compliance Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Compliance Score")
                    Text(
                        text = "${complianceReport.complianceScore}/100",
                        fontWeight = FontWeight.Bold,
                        color = when {
                            complianceReport.complianceScore >= 80 -> MaterialTheme.colorScheme.primary
                            complianceReport.complianceScore >= 60 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                // Issues
                if (complianceReport.issues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Issues Found:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    complianceReport.issues.forEach { issue ->
                        Text(
                            text = "• ${issue.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Recommendations
                if (complianceReport.recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recommendations:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    complianceReport.recommendations.forEach { recommendation ->
                        Text(
                            text = "• $recommendation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataManagementCard(
    onExportData: () -> Unit,
    onDeleteAllData: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Export or delete your personal data in compliance with privacy regulations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportData,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Data")
                }
                
                Button(
                    onClick = onDeleteAllData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete All")
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun RetentionSlider(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    maxValue: Int
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$value days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..maxValue.toFloat(),
            steps = when {
                maxValue <= 100 -> maxValue - 2
                maxValue <= 365 -> 36
                else -> 50
            }
        )
    }
}