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
 * # DashboardScreen
 * 
 * **Clean Architecture - Presentation Layer (UI)**
 * 
 * The main dashboard screen built with Jetpack Compose. This is the primary screen users see,
 * displaying real-time sales data, voice service status, and quick actions.
 * 
 * ## Jetpack Compose Fundamentals:
 * 
 * Compose is Android's modern, declarative UI toolkit. Instead of XML layouts:
 * - **Declarative**: Describe what the UI should look like, not how to build it
 * - **Reactive**: UI automatically updates when state changes
 * - **Composable Functions**: UI components are Kotlin functions marked with @Composable
 * 
 * ## The @Composable Annotation:
 * 
 * Marks this function as a Composable - a UI component that can be composed with other
 * Composables. Composables can:
 * - Call other Composables to build complex UIs from simple pieces
 * - Read state and automatically recompose (re-execute) when state changes
 * - Use special Compose APIs like remember, LaunchedEffect, etc.
 * 
 * ## Function Parameters as Props:
 * 
 * Navigation callbacks (onNavigateToHistory, etc.) are passed as parameters, making this
 * component reusable and testable. The parent decides what happens when buttons are clicked.
 * 
 * ## Default Parameter for viewModel:
 * 
 * `viewModel: DashboardViewModel = hiltViewModel()` provides a default ViewModel from Hilt.
 * This pattern:
 * - Makes production code convenient (no need to pass the ViewModel)
 * - Makes tests easy (pass a test ViewModel instead of the default)
 * 
 * ## State Collection:
 * 
 * `collectAsStateWithLifecycle()` is crucial - it:
 * 1. Collects StateFlow from the ViewModel
 * 2. Converts it to Compose State (triggers recomposition)
 * 3. Lifecycle-aware (stops collecting when screen is not visible, saving resources)
 * 
 * ## Material 3:
 * 
 * Uses Material Design 3 (Material You) components for modern, adaptive UI that respects
 * user theme preferences (light/dark mode, dynamic colors).
 * 
 * @param onNavigateToHistory Callback to navigate to the transaction history screen
 * @param onNavigateToSettings Callback to navigate to the settings screen
 * @param onNavigateToSummary Callback to navigate to daily summary details (receives date)
 * @param viewModel The ViewModel managing this screen's state (injected by Hilt by default)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSummary: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // Collect state from ViewModel as Compose State
    // The 'by' keyword uses Kotlin's property delegation to unwrap the State<T>
    // so we can access uiState directly instead of uiState.value
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // LocalContext provides access to the Android Context
    // CompositionLocal is Compose's way of passing data down the tree without explicit parameters
    val context = LocalContext.current
    
    /**
     * LaunchedEffect for side effects.
     * 
     * ## What are Side Effects in Compose?
     * 
     * Side effects are operations that affect something outside the Composable function's scope:
     * - Showing a Snackbar
     * - Making a network call
     * - Starting an animation
     * - Updating a database
     * 
     * ## LaunchedEffect:
     * 
     * Runs a coroutine when the Composable enters composition or when its key changes.
     * Here, the key is `uiState.message` - when the message changes, this block runs.
     * 
     * ## Lifecycle:
     * 
     * - LaunchedEffect is cancelled if the Composable leaves composition
     * - It restarts if the key (uiState.message) changes
     * - This prevents leaks and ensures side effects match the current state
     */
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            // TODO: Show snackbar with the message
            viewModel.clearMessage()
        }
    }
    
    // Handle error messages separately
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // TODO: Show error snackbar
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