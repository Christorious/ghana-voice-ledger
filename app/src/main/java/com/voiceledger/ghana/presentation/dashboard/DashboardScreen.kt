package com.voiceledger.ghana.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.R
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Dashboard screen composable
 * Main screen showing today's sales summary and listening status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSummary: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle messages and errors
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            // Show snackbar or toast
            viewModel.clearMessage()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Show error snackbar
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
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // Battery indicator
                uiState.data?.let { data ->
                    BatteryIndicator(
                        batteryLevel = data.batteryLevel,
                        color = viewModel.getBatteryStatusColor()
                    )
                }
                
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            uiState.data?.let { data ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Listening Status Card
                    item {
                        ListeningStatusCard(
                            isListening = data.isListening,
                            serviceStatus = data.serviceStatus,
                            marketStatus = viewModel.getMarketStatus(),
                            timeSinceLastTransaction = viewModel.getTimeSinceLastTransaction(),
                            onPauseResume = {
                                if (data.isListening) {
                                    viewModel.pauseListening()
                                } else {
                                    viewModel.resumeListening()
                                }
                            }
                        )
                    }
                    
                    // Daily Total Card
                    item {
                        DailySalesCard(
                            totalSales = data.totalSales,
                            transactionCount = data.transactionCount,
                            formatCurrency = viewModel::formatCurrency
                        )
                    }
                    
                    // Quick Stats Row
                    item {
                        QuickStatsRow(
                            topProduct = data.topProduct,
                            regularCustomers = data.regularCustomers,
                            peakHour = data.peakHour
                        )
                    }
                    
                    // Recent Transactions
                    item {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    if (data.recentTransactions.isEmpty()) {
                        item {
                            EmptyTransactionsCard()
                        }
                    } else {
                        items(data.recentTransactions) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                formatCurrency = viewModel::formatCurrency
                            )
                        }
                    }
                    
                    // Action Buttons
                    item {
                        ActionButtonsRow(
                            onViewHistory = onNavigateToHistory,
                            onGenerateSummary = viewModel::generateDailySummary,
                            onViewSummary = { onNavigateToSummary(viewModel.getCurrentDate()) },
                            isGeneratingSummary = uiState.isGeneratingSummary,
                            hasTodaysSummary = uiState.todaysSummary != null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningStatusCard(
    isListening: Boolean,
    serviceStatus: String,
    marketStatus: MarketStatus,
    timeSinceLastTransaction: String?,
    onPauseResume: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isListening) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Listening indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Color.Green else Color.Gray
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (isListening) "Listening..." else "Paused",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Pause/Resume button
                FilledTonalButton(
                    onClick = onPauseResume
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isListening) "Pause" else "Resume"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isListening) "Pause" else "Resume")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Market status
            Text(
                text = when (marketStatus) {
                    MarketStatus.OPEN -> "Market Open"
                    MarketStatus.BEFORE_HOURS -> "Before Market Hours"
                    MarketStatus.AFTER_HOURS -> "After Market Hours"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Time since last transaction
            timeSinceLastTransaction?.let { time ->
                Text(
                    text = "Last transaction: $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailySalesCard(
    totalSales: Double,
    transactionCount: Int,
    formatCurrency: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today's Sales",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = formatCurrency(totalSales),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "$transactionCount sales today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickStatsRow(
    topProduct: String,
    regularCustomers: Int,
    peakHour: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Product
        Card(
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = topProduct,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Top Product",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Regular Customers
        Card(
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$regularCustomers",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Regulars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Peak Hour
        Card(
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = peakHour,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Peak Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    formatCurrency: (Double) -> String
) {
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    val zoneId = ZoneId.systemDefault()
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.product,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Instant.ofEpochMilli(transaction.timestamp).atZone(zoneId).toLocalTime()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    transaction.quantity?.let { quantity ->
                        Text(
                            text = " • $quantity ${transaction.unit ?: "pcs"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (transaction.needsReview) {
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No transactions yet today",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Start selling and transactions will appear here automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    onViewHistory: () -> Unit,
    onGenerateSummary: () -> Unit,
    onViewSummary: () -> Unit,
    isGeneratingSummary: Boolean,
    hasTodaysSummary: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("History")
            }
            
            if (hasTodaysSummary) {
                FilledButton(
                    onClick = onViewSummary,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Summary")
                }
            } else {
                FilledButton(
                    onClick = onGenerateSummary,
                    modifier = Modifier.weight(1f),
                    enabled = !isGeneratingSummary
                ) {
                    if (isGeneratingSummary) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = null
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate")
                }
            }
        }
        
        if (hasTodaysSummary) {
            OutlinedButton(
                onClick = onGenerateSummary,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGeneratingSummary
            ) {
                if (isGeneratingSummary) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh Summary")
            }
        }
    }
}

@Composable
private fun BatteryIndicator(
    batteryLevel: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Battery6Bar, // You might want to use different icons based on level
            contentDescription = "Battery",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "$batteryLevel%",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    VoiceLedgerTheme {
        // Preview would need mock data
    }
}