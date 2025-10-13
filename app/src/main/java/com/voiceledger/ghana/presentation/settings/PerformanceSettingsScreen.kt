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
import com.voiceledger.ghana.performance.IssueSeverity
import com.voiceledger.ghana.performance.PerformanceIssue
import com.voiceledger.ghana.performance.PerformanceIssueType
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme

/**
 * Screen for monitoring and configuring performance settings
 * Shows real-time performance metrics and optimization options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PerformanceSettingsViewModel = hiltViewModel()
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
                    text = "Performance Monitor",
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
            },
            actions = {
                IconButton(onClick = viewModel::refreshMetrics) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        )
        
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Performance Overview Card
            PerformanceOverviewCard(
                overallScore = uiState.overallScore,
                fps = uiState.fps,
                droppedFramePercent = uiState.droppedFramePercent,
                memoryUsagePercent = uiState.memoryUsagePercent
            )
            
            // Memory Statistics Card
            MemoryStatisticsCard(
                usedMemoryMB = uiState.usedMemoryMB,
                maxMemoryMB = uiState.maxMemoryMB,
                poolStatistics = uiState.poolStatistics,
                onClearPools = viewModel::clearMemoryPools,
                onForceGC = viewModel::forceGarbageCollection
            )
            
            // Database Performance Card
            DatabasePerformanceCard(
                cacheHitRate = uiState.cacheHitRate,
                slowQueryCount = uiState.slowQueryCount,
                averageQueryTime = uiState.averageQueryTime,
                onOptimizeDatabase = viewModel::optimizeDatabase
            )
            
            // Performance Issues Card
            if (uiState.performanceIssues.isNotEmpty()) {
                PerformanceIssuesCard(
                    issues = uiState.performanceIssues
                )
            }
            
            // Method Performance Card
            if (uiState.slowMethods.isNotEmpty()) {
                MethodPerformanceCard(
                    slowMethods = uiState.slowMethods
                )
            }
            
            // Performance Settings Card
            PerformanceSettingsCard(
                monitoringEnabled = uiState.monitoringEnabled,
                frameRateMonitoringEnabled = uiState.frameRateMonitoringEnabled,
                methodTrackingEnabled = uiState.methodTrackingEnabled,
                onMonitoringToggle = viewModel::setMonitoringEnabled,
                onFrameRateToggle = viewModel::setFrameRateMonitoringEnabled,
                onMethodTrackingToggle = viewModel::setMethodTrackingEnabled
            )
            
            // Recommendations Card
            if (uiState.recommendations.isNotEmpty()) {
                RecommendationsCard(
                    recommendations = uiState.recommendations
                )
            }
        }
    }
}

@Composable
private fun PerformanceOverviewCard(
    overallScore: Int,
    fps: Int,
    droppedFramePercent: Int,
    memoryUsagePercent: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                overallScore >= 80 -> MaterialTheme.colorScheme.primaryContainer
                overallScore >= 60 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
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
                    text = "Performance Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "$overallScore/100",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        overallScore >= 80 -> Color.Green
                        overallScore >= 60 -> Color(0xFFFF9800) // Orange
                        else -> Color.Red
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PerformanceMetric(
                    label = "FPS",
                    value = fps.toString(),
                    isGood = fps >= 50
                )
                
                PerformanceMetric(
                    label = "Dropped",
                    value = "$droppedFramePercent%",
                    isGood = droppedFramePercent <= 5
                )
                
                PerformanceMetric(
                    label = "Memory",
                    value = "$memoryUsagePercent%",
                    isGood = memoryUsagePercent <= 80
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetric(
    label: String,
    value: String,
    isGood: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isGood) Color.Green else Color.Red
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MemoryStatisticsCard(
    usedMemoryMB: Int,
    maxMemoryMB: Int,
    poolStatistics: Map<String, Int>,
    onClearPools: () -> Unit,
    onForceGC: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Memory Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Heap Usage: $usedMemoryMB MB / $maxMemoryMB MB",
                style = MaterialTheme.typography.bodyMedium
            )
            
            LinearProgressIndicator(
                progress = usedMemoryMB.toFloat() / maxMemoryMB,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            
            if (poolStatistics.isNotEmpty()) {
                Text(
                    text = "Object Pools:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                poolStatistics.forEach { (name, count) ->
                    Text(
                        text = "• $name: $count objects",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearPools,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Pools")
                }
                
                OutlinedButton(
                    onClick = onForceGC,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Force GC")
                }
            }
        }
    }
}

@Composable
private fun DatabasePerformanceCard(
    cacheHitRate: Int,
    slowQueryCount: Int,
    averageQueryTime: Long,
    onOptimizeDatabase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Database Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Cache Hit Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$cacheHitRate%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (cacheHitRate >= 70) Color.Green else Color.Red
                    )
                }
                
                Column {
                    Text(
                        text = "Slow Queries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = slowQueryCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (slowQueryCount <= 3) Color.Green else Color.Red
                    )
                }
                
                Column {
                    Text(
                        text = "Avg Query Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${averageQueryTime}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (averageQueryTime <= 50) Color.Green else Color.Red
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onOptimizeDatabase,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Optimize Database")
            }
        }
    }
}

@Composable
private fun PerformanceIssuesCard(
    issues: List<PerformanceIssue>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Issues (${issues.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            issues.take(3).forEach { issue ->
                PerformanceIssueItem(issue = issue)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (issues.size > 3) {
                Text(
                    text = "... and ${issues.size - 3} more issues",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun PerformanceIssueItem(issue: PerformanceIssue) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = when (issue.severity) {
                IssueSeverity.HIGH, IssueSeverity.CRITICAL -> Icons.Default.Error
                IssueSeverity.MEDIUM -> Icons.Default.Warning
                IssueSeverity.LOW -> Icons.Default.Info
            },
            contentDescription = null,
            tint = when (issue.severity) {
                IssueSeverity.HIGH, IssueSeverity.CRITICAL -> Color.Red
                IssueSeverity.MEDIUM -> Color(0xFFFF9800) // Orange
                IssueSeverity.LOW -> Color.Blue
            },
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = issue.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MethodPerformanceCard(
    slowMethods: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Slow Methods (${slowMethods.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            slowMethods.take(5).forEach { method ->
                Text(
                    text = "• $method",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            
            if (slowMethods.size > 5) {
                Text(
                    text = "... and ${slowMethods.size - 5} more methods",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PerformanceSettingsCard(
    monitoringEnabled: Boolean,
    frameRateMonitoringEnabled: Boolean,
    methodTrackingEnabled: Boolean,
    onMonitoringToggle: (Boolean) -> Unit,
    onFrameRateToggle: (Boolean) -> Unit,
    onMethodTrackingToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Monitoring Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingToggleItem(
                title = "Performance Monitoring",
                description = "Enable overall performance monitoring",
                checked = monitoringEnabled,
                onCheckedChange = onMonitoringToggle
            )
            
            SettingToggleItem(
                title = "Frame Rate Monitoring",
                description = "Monitor UI frame rate and dropped frames",
                checked = frameRateMonitoringEnabled,
                onCheckedChange = onFrameRateToggle
            )
            
            SettingToggleItem(
                title = "Method Tracking",
                description = "Track method execution times",
                checked = methodTrackingEnabled,
                onCheckedChange = onMethodTrackingToggle
            )
        }
    }
}

@Composable
private fun SettingToggleItem(
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
        Column(modifier = Modifier.weight(1f)) {
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
private fun RecommendationsCard(
    recommendations: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PerformanceSettingsScreenPreview() {
    VoiceLedgerTheme {
        PerformanceSettingsScreen()
    }
}