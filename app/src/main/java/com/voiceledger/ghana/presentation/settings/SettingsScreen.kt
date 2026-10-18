package com.voiceledger.ghana.presentation.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme

/**
 * Settings screen with comprehensive configuration options
 * Includes language selection, market hours, voice enrollment, and privacy controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showMarketHoursDialog by remember { mutableStateOf(false) }
    var showVoiceEnrollmentDialog by remember { mutableStateOf(false) }
    var showDataExportDialog by remember { mutableStateOf(false) }
    var showDataDeleteDialog by remember { mutableStateOf(false) }
    
    // Handle messages and errors
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            viewModel.clearMessage()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            viewModel.clearError()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language Settings
                item {
                    SettingsSection(
                        title = "Language & Region",
                        icon = Icons.Default.Language
                    ) {
                        SettingsItem(
                            title = "Language",
                            subtitle = uiState.settings.selectedLanguage.name,
                            icon = Icons.Default.Translate,
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }
                
                // Market Hours Settings
                item {
                    SettingsSection(
                        title = "Market Configuration",
                        icon = Icons.Default.Schedule
                    ) {
                        SettingsItem(
                            title = "Market Hours",
                            subtitle = "${formatHour(uiState.settings.marketStartHour)} - ${formatHour(uiState.settings.marketEndHour)}",
                            icon = Icons.Default.AccessTime,
                            onClick = { showMarketHoursDialog = true }
                        )
                    }
                }
                
                // Voice Settings
                item {
                    SettingsSection(
                        title = "Voice Recognition",
                        icon = Icons.Default.Mic
                    ) {
                        // Voice Enrollment
                        SettingsItem(
                            title = if (uiState.hasSellerProfile) "Voice Profile" else "Set Up Voice Profile",
                            subtitle = if (uiState.hasSellerProfile) {
                                "Profile: ${uiState.sellerProfileName ?: \"Seller\"}"
                            } else {
                                "Enroll your voice for better accuracy"
                            },
                            icon = if (uiState.hasSellerProfile) Icons.Default.Person else Icons.Default.PersonAdd,
                            onClick = { showVoiceEnrollmentDialog = true },
                            trailing = {
                                if (uiState.hasSellerProfile) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Enrolled",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Voice Confirmation Toggle
                        SettingsToggleItem(
                            title = "Voice Confirmation",
                            subtitle = "Confirm transactions with voice",
                            icon = Icons.Default.VoiceChat,
                            checked = uiState.settings.enableVoiceConfirmation,
                            onCheckedChange = viewModel::updateVoiceConfirmation
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Confidence Threshold
                        ConfidenceThresholdSetting(
                            threshold = uiState.settings.confidenceThreshold,
                            onThresholdChange = viewModel::updateConfidenceThreshold
                        )
                    }
                }
                
                // Notification Settings
                item {
                    SettingsSection(
                        title = "Notifications",
                        icon = Icons.Default.Notifications
                    ) {
                        SettingsToggleItem(
                            title = "Enable Notifications",
                            subtitle = "Get notified about transactions and summaries",
                            icon = Icons.Default.NotificationsActive,
                            checked = uiState.settings.enableNotifications,
                            onCheckedChange = viewModel::updateNotifications
                        )
                    }
                }
                
                // Data & Privacy Settings
                item {
                    SettingsSection(
                        title = "Data & Privacy",
                        icon = Icons.Default.Security
                    ) {
                        // Auto Backup
                        SettingsToggleItem(
                            title = "Auto Backup",
                            subtitle = "Automatically backup your data",
                            icon = Icons.Default.Backup,
                            checked = uiState.settings.autoBackup,
                            onCheckedChange = viewModel::updateAutoBackup
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Data Retention
                        DataRetentionSetting(
                            days = uiState.settings.dataRetentionDays,
                            onDaysChange = viewModel::updateDataRetention
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Export Data
                        SettingsItem(
                            title = "Export Data",
                            subtitle = "Download your transaction data",
                            icon = Icons.Default.FileDownload,
                            onClick = { showDataExportDialog = true },
                            trailing = {
                                if (uiState.isExportingData) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Delete All Data
                        SettingsItem(
                            title = "Delete All Data",
                            subtitle = "Permanently delete all transactions and customers",
                            icon = Icons.Default.DeleteForever,
                            onClick = { showDataDeleteDialog = true },
                            titleColor = MaterialTheme.colorScheme.error,
                            trailing = {
                                if (uiState.isDeletingData) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                }
                
                // Data Statistics
                item {
                    DataStatisticsCard(
                        totalTransactions = uiState.totalTransactions,
                        totalCustomers = uiState.totalCustomers
                    )
                }
                
                // App Information
                item {
                    AppInfoCard()
                }
            }
        }
    }
    
    // Dialogs
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            languages = viewModel.availableLanguages,
            selectedLanguage = uiState.settings.selectedLanguage,
            onLanguageSelected = { language ->
                viewModel.updateLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    if (showMarketHoursDialog) {
        MarketHoursDialog(
            startHour = uiState.settings.marketStartHour,
            endHour = uiState.settings.marketEndHour,
            onHoursSelected = { start, end ->
                viewModel.updateMarketHours(start, end)
                showMarketHoursDialog = false
            },
            onDismiss = { showMarketHoursDialog = false }
        )
    }
    
    if (showVoiceEnrollmentDialog) {
        VoiceEnrollmentDialog(
            isEnrolling = uiState.isEnrollingVoice,
            enrollmentStep = uiState.enrollmentStep,
            hasExistingProfile = uiState.hasSellerProfile,
            onStartEnrollment = viewModel::startVoiceEnrollment,
            onCancelEnrollment = viewModel::cancelVoiceEnrollment,
            onDismiss = { showVoiceEnrollmentDialog = false }
        )
    }
    
    if (showDataExportDialog) {
        DataExportDialog(
            isExporting = uiState.isExportingData,
            onExport = {
                viewModel.exportData()
                showDataExportDialog = false
            },
            onDismiss = { showDataExportDialog = false }
        )
    }
    
    if (showDataDeleteDialog) {
        DataDeleteDialog(
            isDeleting = uiState.isDeletingData,
            totalTransactions = uiState.totalTransactions,
            totalCustomers = uiState.totalCustomers,
            onConfirmDelete = {
                viewModel.deleteAllData()
                showDataDeleteDialog = false
            },
            onDismiss = { showDataDeleteDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            trailing?.invoke() ?: Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun ConfidenceThresholdSetting(
    threshold: Float,
    onThresholdChange: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Confidence Threshold",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Minimum confidence for auto-saving: ${(threshold * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Slider(
                value = threshold,
                onValueChange = onThresholdChange,
                valueRange = 0.5f..1.0f,
                steps = 9 // 50%, 55%, 60%, ..., 95%, 100%
            )
        }
    }
}

@Composable
private fun DataRetentionSetting(
    days: Int,
    onDaysChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = { showDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Data Retention",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Keep data for $days days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    if (showDialog) {
        DataRetentionDialog(
            currentDays = days,
            onDaysSelected = { newDays ->
                onDaysChange(newDays)
                showDialog = false
            },
            onDismiss = { showDialog = false }
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    VoiceLedgerTheme {
        // Preview would need mock data
    }
}"