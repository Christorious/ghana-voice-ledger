package com.voiceledger.ghana.presentation.summary

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.domain.service.*
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * Summary screen with visual dashboard and insights
 * Displays comprehensive daily/weekly/monthly summaries with voice output
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showDatePicker by remember { mutableStateOf(false) }
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
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = when (uiState.summaryType) {
                        SummaryType.DAILY -> "Daily Summary"
                        SummaryType.WEEKLY -> "Weekly Summary"
                        SummaryType.MONTHLY -> "Monthly Summary"
                    },
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
                // Voice output button
                IconButton(
                    onClick = {
                        if (uiState.isSpeaking) {
                            viewModel.stopSpeaking()
                        } else {
                            viewModel.speakSummary()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (uiState.isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (uiState.isSpeaking) "Stop speaking" else "Speak summary",
                        tint = if (uiState.isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Export button
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export summary"
                    )
                }
                
                // Refresh button
                IconButton(onClick = viewModel::refreshSummary) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh summary"
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
                // Summary Type Selector
                item {
                    SummaryTypeSelector(
                        selectedType = uiState.summaryType,
                        onTypeSelected = { type ->
                            when (type) {
                                SummaryType.DAILY -> viewModel.loadTodaysSummary()
                                SummaryType.WEEKLY -> {
                                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    val calendar = Calendar.getInstance()
                                    calendar.add(Calendar.DAY_OF_YEAR, -6)
                                    val weekStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                                    viewModel.loadWeeklySummary(weekStart, today)
                                }
                                SummaryType.MONTHLY -> {
                                    val calendar = Calendar.getInstance()
                                    viewModel.loadMonthlySummary(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
                                }
                            }
                        }
                    )
                }
                
                // Date/Period Display
                item {
                    DatePeriodCard(
                        summaryType = uiState.summaryType,
                        selectedDate = uiState.selectedDate,
                        formatDate = viewModel::formatDate,
                        onDateClick = { showDatePicker = true }
                    )
                }
                
                // Main Metrics
                when (uiState.summaryType) {
                    SummaryType.DAILY -> {
                        uiState.currentSummary?.let { summary ->
                            item {
                                DailyMetricsCard(
                                    summary = summary,
                                    formatCurrency = viewModel::formatCurrency
                                )
                            }
                            
                            // Performance Insights
                            item {
                                PerformanceInsightsCard(
                                    summary = summary,
                                    formatCurrency = viewModel::formatCurrency,
                                    formatPercentage = viewModel::formatPercentage
                                )
                            }
                            
                            // Product Breakdown
                            if (summary.productBreakdown.isNotEmpty()) {
                                item {
                                    ProductBreakdownCard(
                                        productBreakdown = summary.productBreakdown,
                                        formatCurrency = viewModel::formatCurrency
                                    )
                                }
                            }
                            
                            // Hourly Analysis
                            if (summary.hourlyBreakdown.isNotEmpty()) {
                                item {
                                    HourlyAnalysisCard(
                                        hourlyBreakdown = summary.hourlyBreakdown,
                                        formatCurrency = viewModel::formatCurrency
                                    )
                                }
                            }
                            
                            // Customer Insights
                            if (summary.customerInsights.isNotEmpty()) {
                                item {
                                    CustomerInsightsCard(
                                        customerInsights = summary.customerInsights,
                                        formatCurrency = viewModel::formatCurrency
                                    )
                                }
                            }
                            
                            // Recommendations
                            if (summary.recommendations.isNotEmpty()) {
                                item {
                                    RecommendationsCard(
                                        recommendations = summary.recommendations
                                    )
                                }
                            }
                        }
                    }
                    
                    SummaryType.WEEKLY, SummaryType.MONTHLY -> {
                        uiState.currentPeriodSummary?.let { summary ->
                            item {
                                PeriodMetricsCard(
                                    summary = summary,
                                    summaryType = uiState.summaryType,
                                    formatCurrency = viewModel::formatCurrency,
                                    formatPercentage = viewModel::formatPercentage,
                                    formatDate = viewModel::formatDate
                                )
                            }
                            
                            // Top Products
                            if (summary.topProducts.isNotEmpty()) {
                                item {
                                    TopProductsCard(
                                        topProducts = summary.topProducts
                                    )
                                }
                            }
                            
                            // Trends and Growth
                            item {
                                TrendsCard(
                                    summary = summary,
                                    formatPercentage = viewModel::formatPercentage
                                )
                            }
                            
                            // Period Recommendations
                            if (summary.recommendations.isNotEmpty()) {
                                item {
                                    RecommendationsCard(
                                        recommendations = summary.recommendations
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Empty state
                if (uiState.currentSummary == null && uiState.currentPeriodSummary == null && !uiState.isLoading) {
                    item {
                        EmptySummaryCard(
                            summaryType = uiState.summaryType,
                            onGenerateSummary = viewModel::refreshSummary
                        )
                    }
                }
            }
        }
    }
    
    // Export Dialog
    if (showExportDialog) {
        SummaryExportDialog(
            isExporting = uiState.isExporting,
            summaryType = uiState.summaryType,
            onExport = {
                viewModel.exportSummary()
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun SummaryTypeSelector(
    selectedType: SummaryType,
    onTypeSelected: (SummaryType) -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = {
                        Text(
                            text = when (type) {
                                SummaryType.DAILY -> "Daily"
                                SummaryType.WEEKLY -> "Weekly"
                                SummaryType.MONTHLY -> "Monthly"
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DatePeriodCard(
    summaryType: SummaryType,
    selectedDate: String,
    formatDate: (String) -> String,
    onDateClick: () -> Unit
) {
    Card(
        onClick = onDateClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (summaryType) {
                        SummaryType.DAILY -> "Date"
                        SummaryType.WEEKLY -> "Week Starting"
                        SummaryType.MONTHLY -> "Month"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select date",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DailyMetricsCard(
    summary: DailySummary,
    formatCurrency: (Double) -> String
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Key Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Total Sales",
                    value = formatCurrency(summary.totalSales),
                    icon = Icons.Default.AttachMoney,
                    color = MaterialTheme.colorScheme.primary
                )
                
                MetricItem(
                    label = "Transactions",
                    value = summary.transactionCount.toString(),
                    icon = Icons.Default.Receipt,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                MetricItem(
                    label = "Customers",
                    value = summary.uniqueCustomers.toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Avg. Value",
                    value = formatCurrency(summary.averageTransactionValue),
                    icon = Icons.Default.TrendingUp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                MetricItem(
                    label = "Repeat Customers",
                    value = summary.repeatCustomers.toString(),
                    icon = Icons.Default.Favorite,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                MetricItem(
                    label = "New Customers",
                    value = summary.newCustomers.toString(),
                    icon = Icons.Default.PersonAdd,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PerformanceInsightsCard(
    summary: DailySummary,
    formatCurrency: (Double) -> String,
    formatPercentage: (Double) -> String
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Top Product
            summary.topProduct?.let { product ->
                InsightRow(
                    icon = Icons.Default.Star,
                    label = "Top Product",
                    value = "$product (${formatCurrency(summary.topProductSales)})"
                )
            }
            
            // Peak Hour
            summary.peakHour?.let { hour ->
                InsightRow(
                    icon = Icons.Default.Schedule,
                    label = "Peak Hour",
                    value = "${hour}:00 (${formatCurrency(summary.peakHourSales)})"
                )
            }
            
            // Comparisons
            summary.comparisonWithYesterday?.let { comparison ->
                val isPositive = comparison.salesChange >= 0
                InsightRow(
                    icon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    label = "vs. Yesterday",
                    value = formatPercentage(comparison.salesChange),
                    valueColor = if (isPositive) Color.Green else Color.Red
                )
            }
            
            summary.comparisonWithLastWeek?.let { comparison ->
                val isPositive = comparison.salesChange >= 0
                InsightRow(
                    icon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    label = "vs. Last Week",
                    value = formatPercentage(comparison.salesChange),
                    valueColor = if (isPositive) Color.Green else Color.Red
                )
            }
            
            // Quality Metrics
            InsightRow(
                icon = Icons.Default.Psychology,
                label = "Avg. Confidence",
                value = "${(summary.confidenceScore * 100).toInt()}%"
            )
        }
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun ProductBreakdownCard(
    productBreakdown: Map<String, ProductSummary>,
    formatCurrency: (Double) -> String
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Product Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            productBreakdown.entries.sortedByDescending { it.value.totalSales }.take(5).forEach { (product, productSummary) ->
                ProductBreakdownItem(
                    product = product,
                    summary = productSummary,
                    formatCurrency = formatCurrency
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProductBreakdownItem(
    product: String,
    summary: ProductSummary,
    formatCurrency: (Double) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = product,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatCurrency(summary.totalSales),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${summary.transactionCount} sales • ${summary.totalQuantity.toInt()} units",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            summary.peakHour?.let { hour ->
                Text(
                    text = "Peak: ${hour}:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HourlyAnalysisCard(
    hourlyBreakdown: Map<String, HourlySummary>,
    formatCurrency: (Double) -> String
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Hourly Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(hourlyBreakdown.entries.sortedBy { it.key.toIntOrNull() ?: 0 }) { (hour, hourlySummary) ->
                    HourlyAnalysisItem(
                        hour = hour,
                        summary = hourlySummary,
                        formatCurrency = formatCurrency
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyAnalysisItem(
    hour: String,
    summary: HourlySummary,
    formatCurrency: (Double) -> String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${hour}:00",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = formatCurrency(summary.totalSales),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "${summary.transactionCount} sales",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            summary.topProduct?.let { product ->
                Text(
                    text = product,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CustomerInsightsCard(
    customerInsights: Map<String, CustomerInsight>,
    formatCurrency: (Double) -> String
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Customer Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            customerInsights.entries.sortedByDescending { it.value.totalSpent }.take(5).forEach { (customerId, insight) ->
                CustomerInsightItem(
                    customerId = customerId,
                    insight = insight,
                    formatCurrency = formatCurrency
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CustomerInsightItem(
    customerId: String,
    insight: CustomerInsight,
    formatCurrency: (Double) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Customer #${customerId.takeLast(4)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row {
                Text(
                    text = "${insight.transactionCount} purchases",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                insight.favoriteProduct?.let { product ->
                    Text(
                        text = " • Prefers $product",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Text(
            text = formatCurrency(insight.totalSpent),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RecommendationsCard(
    recommendations: List<String>
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
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySummaryCard(
    summaryType: SummaryType,
    onGenerateSummary: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Summarize,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No ${summaryType.name.lowercase()} summary available",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Generate a summary to see your business insights and performance metrics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onGenerateSummary) {
                Text("Generate Summary")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryScreenPreview() {
    VoiceLedgerTheme {
        // Preview would need mock data
    }
}