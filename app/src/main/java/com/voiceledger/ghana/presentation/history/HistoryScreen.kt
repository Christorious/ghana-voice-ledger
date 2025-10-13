package com.voiceledger.ghana.presentation.history

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme
import java.util.*

/**
 * Transaction history screen with search and filtering
 * Displays all transactions with comprehensive filtering options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToTransactionDetail: (String) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilters by viewModel.selectedFilters.collectAsStateWithLifecycle()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
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
        // Top App Bar with Search
        TopAppBar(
            title = {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export transactions"
                    )
                }
                
                IconButton(onClick = { showFilterSheet = true }) {
                    Badge(
                        modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                    ) {
                        if (viewModel.hasActiveFilters()) {
                            Text("!")
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter transactions"
                    )
                }
            }
        )
        
        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Filter Summary
        if (viewModel.hasActiveFilters()) {
            FilterSummaryCard(
                summary = viewModel.getFilterSummary(),
                onClearFilters = viewModel::clearFilters,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        
        // Summary Stats
        if (uiState.transactions.isNotEmpty()) {
            SummaryStatsCard(
                totalTransactions = uiState.totalTransactions,
                totalAmount = uiState.totalAmount,
                formatCurrency = viewModel::formatCurrency,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Transaction List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.transactions.isEmpty()) {
            EmptyHistoryCard(
                hasFilters = viewModel.hasActiveFilters(),
                onClearFilters = viewModel::clearFilters,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.transactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionHistoryItem(
                        transaction = transaction,
                        formatCurrency = viewModel::formatCurrency,
                        formatDate = viewModel::formatDate,
                        formatTime = viewModel::formatTime,
                        onTransactionClick = { onNavigateToTransactionDetail(transaction.id) },
                        onDeleteClick = { viewModel.deleteTransaction(transaction) },
                        onMarkReviewedClick = { viewModel.markAsReviewed(transaction) }
                    )
                }
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            filters = selectedFilters,
            availableProducts = uiState.availableProducts,
            availableCustomers = uiState.availableCustomers,
            onFiltersChanged = { newFilters ->
                viewModel.updateDateFilter(newFilters.startDate, newFilters.endDate)
                viewModel.updateProductFilter(newFilters.selectedProducts)
                viewModel.updateCustomerFilter(newFilters.selectedCustomers)
                viewModel.updateAmountFilter(newFilters.minAmount, newFilters.maxAmount)
                viewModel.updateReviewFilter(newFilters.showOnlyNeedsReview)
            },
            onDismiss = { showFilterSheet = false }
        )
    }
    
    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            isExporting = uiState.isExporting,
            onExport = {
                viewModel.exportTransactions()
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text("Search transactions, products, amounts...")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FilterSummaryCard(
    summary: String,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(onClick = onClearFilters) {
                Text("Clear All")
            }
        }
    }
}

@Composable
private fun SummaryStatsCard(
    totalTransactions: Int,
    totalAmount: Double,
    formatCurrency: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = totalTransactions.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider(
                modifier = Modifier
                    .height(48.dp)
                    .width(1.dp)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatCurrency(totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Total Value",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionHistoryItem(
    transaction: Transaction,
    formatCurrency: (Double) -> String,
    formatDate: (Long) -> String,
    formatTime: (Long) -> String,
    onTransactionClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkReviewedClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        onClick = onTransactionClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.product,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(transaction.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = " • ${formatTime(transaction.timestamp)}",
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (transaction.needsReview) {
                            AssistChip(
                                onClick = onMarkReviewedClick,
                                label = { Text("Review") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            )
                        }
                        
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (transaction.needsReview) {
                                    DropdownMenuItem(
                                        text = { Text("Mark as Reviewed") },
                                        onClick = {
                                            onMarkReviewedClick()
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                                
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        onDeleteClick()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Confidence and Transcript
            if (transaction.confidence < 0.9f || transaction.transcriptSnippet.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                if (transaction.confidence < 0.9f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Confidence: ${(transaction.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (transaction.transcriptSnippet.isNotEmpty()) {
                    Text(
                        text = "\"${transaction.transcriptSnippet}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Customer ID if available
            transaction.customerId?.let { customerId ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Customer: $customerId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (hasFilters) Icons.Default.FilterList else Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (hasFilters) "No transactions match your filters" else "No transactions yet",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (hasFilters) {
                    "Try adjusting your search criteria or clearing filters to see more results."
                } else {
                    "Start recording voice transactions to see them appear here."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (hasFilters) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = onClearFilters) {
                    Text("Clear Filters")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filters: TransactionFilters,
    availableProducts: List<String>,
    availableCustomers: List<String>,
    onFiltersChanged: (TransactionFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var tempFilters by remember { mutableStateOf(filters) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Filter Transactions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Review Filter
                item {
                    ReviewFilter(
                        showOnlyNeedsReview = tempFilters.showOnlyNeedsReview,
                        onReviewFilterChanged = { needsReview ->
                            tempFilters = tempFilters.copy(showOnlyNeedsReview = needsReview)
                        }
                    )
                }
                
                // Amount Range Filter
                item {
                    AmountRangeFilter(
                        minAmount = tempFilters.minAmount,
                        maxAmount = tempFilters.maxAmount,
                        onAmountRangeChanged = { min, max ->
                            tempFilters = tempFilters.copy(minAmount = min, maxAmount = max)
                        }
                    )
                }
                
                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tempFilters = TransactionFilters()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear All")
                        }
                        
                        Button(
                            onClick = {
                                onFiltersChanged(tempFilters)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply Filters")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountRangeFilter(
    minAmount: Double?,
    maxAmount: Double?,
    onAmountRangeChanged: (Double?, Double?) -> Unit
) {
    var minAmountText by remember { mutableStateOf(minAmount?.toString() ?: "") }
    var maxAmountText by remember { mutableStateOf(maxAmount?.toString() ?: "") }
    
    Column {
        Text(
            text = "Amount Range (GHS)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minAmountText,
                onValueChange = { 
                    minAmountText = it
                    onAmountRangeChanged(it.toDoubleOrNull(), maxAmount)
                },
                label = { Text("Min Amount") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            OutlinedTextField(
                value = maxAmountText,
                onValueChange = { 
                    maxAmountText = it
                    onAmountRangeChanged(minAmount, it.toDoubleOrNull())
                },
                label = { Text("Max Amount") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

@Composable
private fun ReviewFilter(
    showOnlyNeedsReview: Boolean,
    onReviewFilterChanged: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Review Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showOnlyNeedsReview,
                onCheckedChange = onReviewFilterChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Show only transactions that need review")
        }
    }
}

@Composable
private fun ExportDialog(
    isExporting: Boolean,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Transactions") },
        text = {
            if (isExporting) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Exporting transactions...")
                }
            } else {
                Text("Export all filtered transactions to a CSV file?")
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

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    VoiceLedgerTheme {
        // Preview would need mock data
    }
}