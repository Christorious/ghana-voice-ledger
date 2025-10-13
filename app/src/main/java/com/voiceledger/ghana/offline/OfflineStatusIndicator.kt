package com.voiceledger.ghana.offline

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Composable that shows offline/online status and sync information
 * Provides visual feedback about network connectivity and sync status
 */
@Composable
fun OfflineStatusIndicator(
    networkState: NetworkState,
    queueState: OfflineQueueState,
    conflictState: ConflictState,
    modifier: Modifier = Modifier,
    showDetails: Boolean = false
) {
    val statusColor = when {
        !networkState.isAvailable -> MaterialTheme.colorScheme.error
        queueState.failedOperations > 0 -> MaterialTheme.colorScheme.error
        conflictState.totalConflicts > 0 -> Color(0xFFFF9800) // Orange
        queueState.pendingOperations > 0 -> MaterialTheme.colorScheme.secondary
        else -> Color.Green
    }
    
    val statusText = when {
        !networkState.isAvailable -> "Offline"
        queueState.failedOperations > 0 -> "Sync Failed"
        conflictState.totalConflicts > 0 -> "Conflicts"
        queueState.pendingOperations > 0 -> "Syncing"
        else -> "Online"
    }
    
    val statusIcon = when {
        !networkState.isAvailable -> Icons.Default.CloudOff
        queueState.failedOperations > 0 -> Icons.Default.SyncProblem
        conflictState.totalConflicts > 0 -> Icons.Default.Warning
        queueState.pendingOperations > 0 -> Icons.Default.Sync
        else -> Icons.Default.CloudDone
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusText,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
                
                if (queueState.processingOperations > 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = statusColor
                    )
                }
            }
            
            if (showDetails) {
                AnimatedVisibility(
                    visible = queueState.pendingOperations > 0 || queueState.failedOperations > 0 || conflictState.totalConflicts > 0,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (queueState.pendingOperations > 0) {
                            DetailRow(
                                icon = Icons.Default.Schedule,
                                text = "${queueState.pendingOperations} pending",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        if (queueState.failedOperations > 0) {
                            DetailRow(
                                icon = Icons.Default.Error,
                                text = "${queueState.failedOperations} failed",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        if (conflictState.totalConflicts > 0) {
                            DetailRow(
                                icon = Icons.Default.Warning,
                                text = "${conflictState.totalConflicts} conflicts",
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Compact status indicator for use in app bars
 */
@Composable
fun CompactOfflineStatusIndicator(
    networkState: NetworkState,
    queueState: OfflineQueueState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val statusColor = when {
        !networkState.isAvailable -> MaterialTheme.colorScheme.error
        queueState.failedOperations > 0 -> MaterialTheme.colorScheme.error
        queueState.pendingOperations > 0 -> MaterialTheme.colorScheme.secondary
        else -> Color.Green
    }
    
    val statusIcon = when {
        !networkState.isAvailable -> Icons.Default.CloudOff
        queueState.failedOperations > 0 -> Icons.Default.SyncProblem
        queueState.pendingOperations > 0 -> Icons.Default.Sync
        else -> Icons.Default.CloudDone
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(statusColor.copy(alpha = 0.1f))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
            
            if (queueState.pendingOperations > 0) {
                Text(
                    text = queueState.pendingOperations.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Network quality indicator
 */
@Composable
fun NetworkQualityIndicator(
    networkState: NetworkState,
    modifier: Modifier = Modifier
) {
    val qualityColor = when (networkState.networkQuality) {
        NetworkQuality.EXCELLENT -> Color.Green
        NetworkQuality.GOOD -> Color(0xFF4CAF50)
        NetworkQuality.FAIR -> Color(0xFFFF9800)
        NetworkQuality.POOR -> Color(0xFFFF5722)
        NetworkQuality.NONE -> MaterialTheme.colorScheme.error
    }
    
    val qualityText = when (networkState.networkQuality) {
        NetworkQuality.EXCELLENT -> "Excellent"
        NetworkQuality.GOOD -> "Good"
        NetworkQuality.FAIR -> "Fair"
        NetworkQuality.POOR -> "Poor"
        NetworkQuality.NONE -> "No Connection"
    }
    
    val networkTypeText = when (networkState.networkType) {
        NetworkType.WIFI -> "WiFi"
        NetworkType.MOBILE -> if (networkState.isMetered) "Mobile (Metered)" else "Mobile"
        NetworkType.ETHERNET -> "Ethernet"
        NetworkType.OTHER -> "Other"
        NetworkType.NONE -> "None"
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Signal strength bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(4) { index ->
                val barHeight = (index + 1) * 3.dp
                val isActive = when (networkState.networkQuality) {
                    NetworkQuality.EXCELLENT -> true
                    NetworkQuality.GOOD -> index < 3
                    NetworkQuality.FAIR -> index < 2
                    NetworkQuality.POOR -> index < 1
                    NetworkQuality.NONE -> false
                }
                
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(barHeight)
                        .background(
                            color = if (isActive) qualityColor else qualityColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
        
        Column {
            Text(
                text = qualityText,
                style = MaterialTheme.typography.labelMedium,
                color = qualityColor,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = networkTypeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Sync progress indicator
 */
@Composable
fun SyncProgressIndicator(
    queueState: OfflineQueueState,
    modifier: Modifier = Modifier
) {
    if (queueState.totalOperations == 0) return
    
    val progress = if (queueState.totalOperations > 0) {
        (queueState.totalOperations - queueState.pendingOperations).toFloat() / queueState.totalOperations
    } else {
        1f
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Syncing...",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${queueState.totalOperations - queueState.pendingOperations}/${queueState.totalOperations}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
    }
}