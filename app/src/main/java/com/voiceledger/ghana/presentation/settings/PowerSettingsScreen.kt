package com.voiceledger.ghana.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme
import com.voiceledger.ghana.service.MarketStatus
import com.voiceledger.ghana.service.PowerMode

/**
 * Screen for configuring power management settings
 * Allows users to customize battery optimization behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PowerSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Power Management",
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
        
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Status Card
            CurrentStatusCard(
                powerMode = uiState.currentPowerMode,
                batteryLevel = uiState.batteryLevel,
                isCharging = uiState.isCharging,
                marketStatus = uiState.marketStatus,
                estimatedBatteryLife = uiState.estimatedBatteryLife
            )
            
            // Market Hours Settings
            MarketHoursCard(
                startHour = uiState.marketStartHour,
                endHour = uiState.marketEndHour,
                onStartHourChange = viewModel::setMarketStartHour,
                onEndHourChange = viewModel::setMarketEndHour
            )
            
            // Battery Thresholds
            BatteryThresholdsCard(
                lowBatteryThreshold = uiState.lowBatteryThreshold,
                criticalBatteryThreshold = uiState.criticalBatteryThreshold,
                onLowThresholdChange = viewModel::setLowBatteryThreshold,
                onCriticalThresholdChange = viewModel::setCriticalBatteryThreshold
            )
            
            // Power Saving Options
            PowerSavingOptionsCard(
                powerSavingEnabled = uiState.powerSavingEnabled,
                onPowerSavingToggle = viewModel::setPowerSavingEnabled
            )
            
            // Manual Override
            ManualOverrideCard(
                currentMode = uiState.currentPowerMode,
                onModeChange = viewModel::setManualPowerMode
            )
        }
    }
}

@Composable
private fun CurrentStatusCard(
    powerMode: PowerMode,
    batteryLevel: Int,
    isCharging: Boolean,
    marketStatus: MarketStatus,
    estimatedBatteryLife: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (powerMode) {
                PowerMode.NORMAL -> MaterialTheme.colorScheme.primaryContainer
                PowerMode.POWER_SAVE -> MaterialTheme.colorScheme.secondaryContainer
                PowerMode.CRITICAL_SAVE -> MaterialTheme.colorScheme.errorContainer
                PowerMode.SLEEP -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
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
                    text = "Current Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Icon(
                    imageVector = when (powerMode) {
                        PowerMode.NORMAL -> Icons.Default.Battery6Bar
                        PowerMode.POWER_SAVE -> Icons.Default.Battery3Bar
                        PowerMode.CRITICAL_SAVE -> Icons.Default.Battery1Bar
                        PowerMode.SLEEP -> Icons.Default.BatteryAlert
                    },
                    contentDescription = null,
                    tint = when (powerMode) {
                        PowerMode.NORMAL -> Color.Green
                        PowerMode.POWER_SAVE -> Color(0xFFFF9800) // Orange
                        PowerMode.CRITICAL_SAVE -> Color.Red
                        PowerMode.SLEEP -> Color.Gray
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Power Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (powerMode) {
                            PowerMode.NORMAL -> "Normal"
                            PowerMode.POWER_SAVE -> "Power Save"
                            PowerMode.CRITICAL_SAVE -> "Critical Save"
                            PowerMode.SLEEP -> "Sleep"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "Battery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$batteryLevel%${if (isCharging) " (Charging)" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Market Status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (marketStatus) {
                            MarketStatus.BEFORE_HOURS -> "Before Hours"
                            MarketStatus.OPEN -> "Open"
                            MarketStatus.AFTER_HOURS -> "After Hours"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "Est. Battery Life",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (estimatedBatteryLife < 24) {
                            "${String.format("%.1f", estimatedBatteryLife)}h"
                        } else {
                            "${String.format("%.1f", estimatedBatteryLife / 24)}d"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketHoursCard(
    startHour: Int,
    endHour: Int,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Market Hours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Set when the market is open for automatic power management",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Time",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = "${String.format("%02d", startHour)}:00",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            (0..23).forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text("${String.format("%02d", hour)}:00") },
                                    onClick = {
                                        onStartHourChange(hour)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "End Time",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = "${String.format("%02d", endHour)}:00",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            (0..23).forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text("${String.format("%02d", hour)}:00") },
                                    onClick = {
                                        onEndHourChange(hour)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryThresholdsCard(
    lowBatteryThreshold: Int,
    criticalBatteryThreshold: Int,
    onLowThresholdChange: (Int) -> Unit,
    onCriticalThresholdChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Battery Thresholds",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Configure when power saving modes activate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Low Battery: ${lowBatteryThreshold}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = lowBatteryThreshold.toFloat(),
                        onValueChange = { onLowThresholdChange(it.toInt()) },
                        valueRange = 10f..50f,
                        steps = 8
                    )
                }
                
                Column {
                    Text(
                        text = "Critical Battery: ${criticalBatteryThreshold}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = criticalBatteryThreshold.toFloat(),
                        onValueChange = { onCriticalThresholdChange(it.toInt()) },
                        valueRange = 1f..(lowBatteryThreshold - 1).toFloat(),
                        steps = (lowBatteryThreshold - 2).coerceAtLeast(1)
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerSavingOptionsCard(
    powerSavingEnabled: Boolean,
    onPowerSavingToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Power Saving Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic Power Saving",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Automatically optimize power usage based on battery level and market hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = powerSavingEnabled,
                    onCheckedChange = onPowerSavingToggle
                )
            }
        }
    }
}

@Composable
private fun ManualOverrideCard(
    currentMode: PowerMode,
    onModeChange: (PowerMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Manual Override",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Temporarily override automatic power management",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PowerMode.values().forEach { mode ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { onModeChange(mode) },
                        label = {
                            Text(
                                text = when (mode) {
                                    PowerMode.NORMAL -> "Normal"
                                    PowerMode.POWER_SAVE -> "Save"
                                    PowerMode.CRITICAL_SAVE -> "Critical"
                                    PowerMode.SLEEP -> "Sleep"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PowerSettingsScreenPreview() {
    VoiceLedgerTheme {
        PowerSettingsScreen()
    }
}