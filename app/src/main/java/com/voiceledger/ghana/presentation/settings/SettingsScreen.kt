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
                            trailing = {\n                                if (uiState.hasSellerProfile) {\n                                    Icon(\n                                        imageVector = Icons.Default.CheckCircle,\n                                        contentDescription = \"Enrolled\",\n                                        tint = MaterialTheme.colorScheme.primary\n                                    )\n                                }\n                            }\n                        )\n                        \n                        Spacer(modifier = Modifier.height(8.dp))\n                        \n                        // Voice Confirmation Toggle\n                        SettingsToggleItem(\n                            title = \"Voice Confirmation\",\n                            subtitle = \"Confirm transactions with voice\",\n                            icon = Icons.Default.VoiceChat,\n                            checked = uiState.settings.enableVoiceConfirmation,\n                            onCheckedChange = viewModel::updateVoiceConfirmation\n                        )\n                        \n                        Spacer(modifier = Modifier.height(8.dp))\n                        \n                        // Confidence Threshold\n                        ConfidenceThresholdSetting(\n                            threshold = uiState.settings.confidenceThreshold,\n                            onThresholdChange = viewModel::updateConfidenceThreshold\n                        )\n                    }\n                }\n                \n                // Notification Settings\n                item {\n                    SettingsSection(\n                        title = \"Notifications\",\n                        icon = Icons.Default.Notifications\n                    ) {\n                        SettingsToggleItem(\n                            title = \"Enable Notifications\",\n                            subtitle = \"Get notified about transactions and summaries\",\n                            icon = Icons.Default.NotificationsActive,\n                            checked = uiState.settings.enableNotifications,\n                            onCheckedChange = viewModel::updateNotifications\n                        )\n                    }\n                }\n                \n                // Data & Privacy Settings\n                item {\n                    SettingsSection(\n                        title = \"Data & Privacy\",\n                        icon = Icons.Default.Security\n                    ) {\n                        // Auto Backup\n                        SettingsToggleItem(\n                            title = \"Auto Backup\",\n                            subtitle = \"Automatically backup your data\",\n                            icon = Icons.Default.Backup,\n                            checked = uiState.settings.autoBackup,\n                            onCheckedChange = viewModel::updateAutoBackup\n                        )\n                        \n                        Spacer(modifier = Modifier.height(8.dp))\n                        \n                        // Data Retention\n                        DataRetentionSetting(\n                            days = uiState.settings.dataRetentionDays,\n                            onDaysChange = viewModel::updateDataRetention\n                        )\n                        \n                        Spacer(modifier = Modifier.height(8.dp))\n                        \n                        // Export Data\n                        SettingsItem(\n                            title = \"Export Data\",\n                            subtitle = \"Download your transaction data\",\n                            icon = Icons.Default.FileDownload,\n                            onClick = { showDataExportDialog = true },\n                            trailing = {\n                                if (uiState.isExportingData) {\n                                    CircularProgressIndicator(\n                                        modifier = Modifier.size(20.dp),\n                                        strokeWidth = 2.dp\n                                    )\n                                }\n                            }\n                        )\n                        \n                        Spacer(modifier = Modifier.height(8.dp))\n                        \n                        // Delete All Data\n                        SettingsItem(\n                            title = \"Delete All Data\",\n                            subtitle = \"Permanently delete all transactions and customers\",\n                            icon = Icons.Default.DeleteForever,\n                            onClick = { showDataDeleteDialog = true },\n                            titleColor = MaterialTheme.colorScheme.error,\n                            trailing = {\n                                if (uiState.isDeletingData) {\n                                    CircularProgressIndicator(\n                                        modifier = Modifier.size(20.dp),\n                                        strokeWidth = 2.dp,\n                                        color = MaterialTheme.colorScheme.error\n                                    )\n                                }\n                            }\n                        )\n                    }\n                }\n                \n                // Data Statistics\n                item {\n                    DataStatisticsCard(\n                        totalTransactions = uiState.totalTransactions,\n                        totalCustomers = uiState.totalCustomers\n                    )\n                }\n                \n                // App Information\n                item {\n                    AppInfoCard()\n                }\n            }\n        }\n    }\n    \n    // Dialogs\n    if (showLanguageDialog) {\n        LanguageSelectionDialog(\n            languages = viewModel.availableLanguages,\n            selectedLanguage = uiState.settings.selectedLanguage,\n            onLanguageSelected = { language ->\n                viewModel.updateLanguage(language)\n                showLanguageDialog = false\n            },\n            onDismiss = { showLanguageDialog = false }\n        )\n    }\n    \n    if (showMarketHoursDialog) {\n        MarketHoursDialog(\n            startHour = uiState.settings.marketStartHour,\n            endHour = uiState.settings.marketEndHour,\n            onHoursSelected = { start, end ->\n                viewModel.updateMarketHours(start, end)\n                showMarketHoursDialog = false\n            },\n            onDismiss = { showMarketHoursDialog = false }\n        )\n    }\n    \n    if (showVoiceEnrollmentDialog) {\n        VoiceEnrollmentDialog(\n            isEnrolling = uiState.isEnrollingVoice,\n            enrollmentStep = uiState.enrollmentStep,\n            hasExistingProfile = uiState.hasSellerProfile,\n            onStartEnrollment = viewModel::startVoiceEnrollment,\n            onCancelEnrollment = viewModel::cancelVoiceEnrollment,\n            onDismiss = { showVoiceEnrollmentDialog = false }\n        )\n    }\n    \n    if (showDataExportDialog) {\n        DataExportDialog(\n            isExporting = uiState.isExportingData,\n            onExport = {\n                viewModel.exportData()\n                showDataExportDialog = false\n            },\n            onDismiss = { showDataExportDialog = false }\n        )\n    }\n    \n    if (showDataDeleteDialog) {\n        DataDeleteDialog(\n            isDeleting = uiState.isDeletingData,\n            totalTransactions = uiState.totalTransactions,\n            totalCustomers = uiState.totalCustomers,\n            onConfirmDelete = {\n                viewModel.deleteAllData()\n                showDataDeleteDialog = false\n            },\n            onDismiss = { showDataDeleteDialog = false }\n        )\n    }\n}\n\n@Composable\nprivate fun SettingsSection(\n    title: String,\n    icon: ImageVector,\n    content: @Composable ColumnScope.() -> Unit\n) {\n    Card(\n        modifier = Modifier.fillMaxWidth()\n    ) {\n        Column(\n            modifier = Modifier.padding(16.dp)\n        ) {\n            Row(\n                verticalAlignment = Alignment.CenterVertically,\n                modifier = Modifier.padding(bottom = 12.dp)\n            ) {\n                Icon(\n                    imageVector = icon,\n                    contentDescription = null,\n                    tint = MaterialTheme.colorScheme.primary,\n                    modifier = Modifier.size(20.dp)\n                )\n                Spacer(modifier = Modifier.width(8.dp))\n                Text(\n                    text = title,\n                    style = MaterialTheme.typography.titleMedium,\n                    fontWeight = FontWeight.SemiBold,\n                    color = MaterialTheme.colorScheme.primary\n                )\n            }\n            \n            content()\n        }\n    }\n}\n\n@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun SettingsItem(\n    title: String,\n    subtitle: String,\n    icon: ImageVector,\n    onClick: () -> Unit,\n    titleColor: Color = MaterialTheme.colorScheme.onSurface,\n    trailing: @Composable (() -> Unit)? = null\n) {\n    Card(\n        onClick = onClick,\n        colors = CardDefaults.cardColors(\n            containerColor = MaterialTheme.colorScheme.surfaceVariant\n        )\n    ) {\n        Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .padding(16.dp),\n            verticalAlignment = Alignment.CenterVertically\n        ) {\n            Icon(\n                imageVector = icon,\n                contentDescription = null,\n                tint = MaterialTheme.colorScheme.onSurfaceVariant,\n                modifier = Modifier.size(24.dp)\n            )\n            \n            Spacer(modifier = Modifier.width(16.dp))\n            \n            Column(\n                modifier = Modifier.weight(1f)\n            ) {\n                Text(\n                    text = title,\n                    style = MaterialTheme.typography.bodyLarge,\n                    fontWeight = FontWeight.Medium,\n                    color = titleColor\n                )\n                Text(\n                    text = subtitle,\n                    style = MaterialTheme.typography.bodyMedium,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant\n                )\n            }\n            \n            trailing?.invoke() ?: Icon(\n                imageVector = Icons.Default.ChevronRight,\n                contentDescription = null,\n                tint = MaterialTheme.colorScheme.onSurfaceVariant\n            )\n        }\n    }\n}\n\n@Composable\nprivate fun SettingsToggleItem(\n    title: String,\n    subtitle: String,\n    icon: ImageVector,\n    checked: Boolean,\n    onCheckedChange: (Boolean) -> Unit\n) {\n    Card(\n        colors = CardDefaults.cardColors(\n            containerColor = MaterialTheme.colorScheme.surfaceVariant\n        )\n    ) {\n        Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .padding(16.dp),\n            verticalAlignment = Alignment.CenterVertically\n        ) {\n            Icon(\n                imageVector = icon,\n                contentDescription = null,\n                tint = MaterialTheme.colorScheme.onSurfaceVariant,\n                modifier = Modifier.size(24.dp)\n            )\n            \n            Spacer(modifier = Modifier.width(16.dp))\n            \n            Column(\n                modifier = Modifier.weight(1f)\n            ) {\n                Text(\n                    text = title,\n                    style = MaterialTheme.typography.bodyLarge,\n                    fontWeight = FontWeight.Medium\n                )\n                Text(\n                    text = subtitle,\n                    style = MaterialTheme.typography.bodyMedium,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant\n                )\n            }\n            \n            Switch(\n                checked = checked,\n                onCheckedChange = onCheckedChange\n            )\n        }\n    }\n}\n\n@Composable\nprivate fun ConfidenceThresholdSetting(\n    threshold: Float,\n    onThresholdChange: (Float) -> Unit\n) {\n    Card(\n        colors = CardDefaults.cardColors(\n            containerColor = MaterialTheme.colorScheme.surfaceVariant\n        )\n    ) {\n        Column(\n            modifier = Modifier\n                .fillMaxWidth()\n                .padding(16.dp)\n        ) {\n            Row(\n                verticalAlignment = Alignment.CenterVertically\n            ) {\n                Icon(\n                    imageVector = Icons.Default.Psychology,\n                    contentDescription = null,\n                    tint = MaterialTheme.colorScheme.onSurfaceVariant,\n                    modifier = Modifier.size(24.dp)\n                )\n                \n                Spacer(modifier = Modifier.width(16.dp))\n                \n                Column(\n                    modifier = Modifier.weight(1f)\n                ) {\n                    Text(\n                        text = \"Confidence Threshold\",\n                        style = MaterialTheme.typography.bodyLarge,\n                        fontWeight = FontWeight.Medium\n                    )\n                    Text(\n                        text = \"Minimum confidence for auto-saving: ${(threshold * 100).toInt()}%\",\n                        style = MaterialTheme.typography.bodyMedium,\n                        color = MaterialTheme.colorScheme.onSurfaceVariant\n                    )\n                }\n            }\n            \n            Spacer(modifier = Modifier.height(12.dp))\n            \n            Slider(\n                value = threshold,\n                onValueChange = onThresholdChange,\n                valueRange = 0.5f..1.0f,\n                steps = 9 // 50%, 55%, 60%, ..., 95%, 100%\n            )\n        }\n    }\n}\n\n@Composable\nprivate fun DataRetentionSetting(\n    days: Int,\n    onDaysChange: (Int) -> Unit\n) {\n    var showDialog by remember { mutableStateOf(false) }\n    \n    Card(\n        onClick = { showDialog = true },\n        colors = CardDefaults.cardColors(\n            containerColor = MaterialTheme.colorScheme.surfaceVariant\n        )\n    ) {\n        Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .padding(16.dp),\n            verticalAlignment = Alignment.CenterVertically\n        ) {\n            Icon(\n                imageVector = Icons.Default.Schedule,\n                contentDescription = null,\n                tint = MaterialTheme.colorScheme.onSurfaceVariant,\n                modifier = Modifier.size(24.dp)\n            )\n            \n            Spacer(modifier = Modifier.width(16.dp))\n            \n            Column(\n                modifier = Modifier.weight(1f)\n            ) {\n                Text(\n                    text = \"Data Retention\",\n                    style = MaterialTheme.typography.bodyLarge,\n                    fontWeight = FontWeight.Medium\n                )\n                Text(\n                    text = \"Keep data for $days days\",\n                    style = MaterialTheme.typography.bodyMedium,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant\n                )\n            }\n            \n            Icon(\n                imageVector = Icons.Default.ChevronRight,\n                contentDescription = null,\n                tint = MaterialTheme.colorScheme.onSurfaceVariant\n            )\n        }\n    }\n    \n    if (showDialog) {\n        DataRetentionDialog(\n            currentDays = days,\n            onDaysSelected = { newDays ->\n                onDaysChange(newDays)\n                showDialog = false\n            },\n            onDismiss = { showDialog = false }\n        )\n    }\n}\n\nprivate fun formatHour(hour: Int): String {\n    return when {\n        hour == 0 -> \"12:00 AM\"\n        hour < 12 -> \"$hour:00 AM\"\n        hour == 12 -> \"12:00 PM\"\n        else -> \"${hour - 12}:00 PM\"\n    }\n}\n\n@Preview(showBackground = true)\n@Composable\nprivate fun SettingsScreenPreview() {\n    VoiceLedgerTheme {\n        // Preview would need mock data\n    }\n}"