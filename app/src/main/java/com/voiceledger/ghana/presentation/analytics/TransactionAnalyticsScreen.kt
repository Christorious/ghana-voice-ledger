package com.voiceledger.ghana.presentation.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.domain.model.AnalyticsRange
import com.voiceledger.ghana.domain.model.TransactionAnalytics
import com.voiceledger.ghana.domain.model.TransactionAnalyticsOverview
import com.voiceledger.ghana.domain.model.TransactionCategoryMetric
import com.voiceledger.ghana.domain.model.TransactionHourlyMetric
import com.voiceledger.ghana.domain.model.TransactionSpeakerMetric
import com.voiceledger.ghana.domain.model.TransactionSuccessMetrics
import com.voiceledger.ghana.domain.model.TransactionTrendPoint
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme
import java.util.Locale

@Composable
fun TransactionAnalyticsScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TransactionAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TransactionAnalyticsContent(
        uiState = uiState,
        onRangeSelected = viewModel::selectRange,
        formatCurrency = viewModel::formatCurrency,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
fun TransactionAnalyticsContent(
    uiState: TransactionAnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    formatCurrency: (Double) -> String,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading && uiState.overview == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.overview == null -> {
                    EmptyAnalyticsState()
                }

                !uiState.overview.analyticsEnabled -> {
                    AnalyticsDisabledCard(onNavigateToSettings)
                }

                else -> {
                    AnalyticsDataContent(
                        uiState = uiState,
                        onRangeSelected = onRangeSelected,
                        formatCurrency = formatCurrency,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }

            AnimatedVisibility(visible = uiState.isLoading && uiState.overview != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Refreshing analytics…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsDataContent(
    uiState: TransactionAnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    formatCurrency: (Double) -> String,
    onNavigateToSettings: () -> Unit
) {
    val overview = uiState.overview ?: return
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsRangeSelector(
                selectedRange = uiState.selectedRange,
                onRangeSelected = onRangeSelected
            )
        }

        item {
            AnalyticsSummaryCard(
                overview = overview,
                formatCurrency = formatCurrency
            )
        }

        item {
            SuccessMetricsCard(overview.successMetrics)
        }

        item {
            CategoryBreakdownCard(overview.categories, formatCurrency)
        }

        item {
            DailyTrendCard(overview.dailyTrend, formatCurrency)
        }

        item {
            HourlyTrendCard(overview.hourlyTrend)
        }

        item {
            SpeakerStatsCard(overview.speakerStats)
        }

        item {
            PrivacyReminderCard(onNavigateToSettings)
        }
    }
}

@Composable
private fun AnalyticsRangeSelector(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit
) {
    val availableRanges = listOf(AnalyticsRange.Last7Days, AnalyticsRange.Last30Days)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        availableRanges.forEach { range ->
            FilterChip(
                selected = selectedRange::class == range::class,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun AnalyticsSummaryCard(
    overview: TransactionAnalyticsOverview,
    formatCurrency: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            SummaryMetricRow(
                label = "Total sales",
                value = formatCurrency(overview.summary.totalSales)
            )
            SummaryMetricRow(
                label = "Transactions",
                value = overview.summary.transactionCount.toString()
            )
            SummaryMetricRow(
                label = "Unique customers",
                value = overview.summary.uniqueCustomers.toString()
            )
            SummaryMetricRow(
                label = "Average value",
                value = formatCurrency(overview.summary.averageTransactionValue)
            )
            SummaryMetricRow(
                label = "Success rate",
                value = percentageString(overview.summary.successRate)
            )
        }
    }
}

@Composable
private fun SummaryMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SuccessMetricsCard(metrics: TransactionSuccessMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Success metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Auto-saved: ${metrics.autoSaved}")
            Text("Flagged for review: ${metrics.flaggedForReview}")
            Text("Manual corrections: ${metrics.manual}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Overall success rate: ${percentageString(metrics.successRate)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    categories: List<TransactionCategoryMetric>,
    formatCurrency: (Double) -> String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Category breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (categories.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Not enough data to calculate product categories yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                categories.forEach { category ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = category.category,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${category.transactionCount} • ${formatCurrency(category.totalAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(category.percentageOfTransactions.toFloat().coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTrendCard(
    dailyTrend: List<TransactionTrendPoint>,
    formatCurrency: (Double) -> String
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (dailyTrend.isEmpty()) {
                Text(
                    text = "Daily trend will appear once transactions are tracked.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                TrendBarRow(
                    data = dailyTrend.takeLast(10),
                    labelProvider = { it.date.substringAfterLast('-') },
                    valueProvider = { it.transactionCount },
                    tooltipProvider = { formatCurrency(it.totalAmount) }
                )
            }
        }
    }
}

@Composable
private fun HourlyTrendCard(hourlyTrend: List<TransactionHourlyMetric>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hourly peak times",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (hourlyTrend.isEmpty()) {
                Text(
                    text = "Hourly data will appear once transactions are captured.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                TrendBarRow(
                    data = hourlyTrend,
                    labelProvider = { it.label.substring(0, 2) },
                    valueProvider = { it.transactionCount },
                    tooltipProvider = { "${it.transactionCount}" }
                )
            }
        }
    }
}

@Composable
private fun <T> TrendBarRow(
    data: List<T>,
    labelProvider: (T) -> String,
    valueProvider: (T) -> Int,
    tooltipProvider: (T) -> String,
    maxBarHeight: Dp = 140.dp
) {
    val maxValue = (data.maxOfOrNull(valueProvider) ?: 1).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { item ->
            val ratio = valueProvider(item).toFloat() / maxValue.toFloat()
            val barHeight = maxBarHeight * ratio.coerceIn(0f, 1f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tooltipProvider(item),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(barHeight)
                        .width(18.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labelProvider(item),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SpeakerStatsCard(speakers: List<TransactionSpeakerMetric>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Speaker performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (speakers.isEmpty()) {
                Text(
                    text = "Speaker statistics will appear once voice profiles are recognised.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                speakers.take(5).forEach { speaker ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = speaker.speakerId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Transactions: ${speaker.transactionCount} · Success: ${percentageString(speaker.successRate)}",
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
private fun PrivacyReminderCard(onNavigateToSettings: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Privacy controls",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You can fine-tune analytics collection in privacy settings at any time.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onNavigateToSettings) {
                Text("Open privacy settings")
            }
        }
    }
}

@Composable
private fun AnalyticsDisabledCard(onNavigateToSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Analytics disabled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Enable analytics in privacy settings to view transaction insights.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateToSettings) {
                    Text("Go to privacy settings")
                }
            }
        }
    }
}

@Composable
private fun EmptyAnalyticsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No analytics yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start logging transactions to unlock trend insights.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun percentageString(value: Double): String {
    return String.format(Locale.getDefault(), "%.0f%%", value.coerceAtLeast(0.0) * 100)
}

@Preview
@Composable
private fun AnalyticsScreenPreview() {
    val overview = TransactionAnalyticsOverview(
        summary = TransactionAnalytics(
            totalSales = 540.0,
            transactionCount = 28,
            topProduct = "Tilapia",
            peakHour = "10:00-11:00",
            uniqueCustomers = 12,
            averageTransactionValue = 19.2,
            successRate = 0.82
        ),
        categories = listOf(
            TransactionCategoryMetric("Tilapia", 12, 220.0, 0.42),
            TransactionCategoryMetric("Mackerel", 8, 180.0, 0.28),
            TransactionCategoryMetric("Prawns", 5, 90.0, 0.18)
        ),
        dailyTrend = listOf(
            TransactionTrendPoint("2024-06-21", 4, 90.0),
            TransactionTrendPoint("2024-06-22", 6, 110.0),
            TransactionTrendPoint("2024-06-23", 8, 140.0),
            TransactionTrendPoint("2024-06-24", 5, 120.0)
        ),
        hourlyTrend = listOf(
            TransactionHourlyMetric(8, "08:00-09:00", 3, 45.0),
            TransactionHourlyMetric(9, "09:00-10:00", 5, 75.0),
            TransactionHourlyMetric(10, "10:00-11:00", 9, 130.0)
        ),
        speakerStats = listOf(
            TransactionSpeakerMetric("Ama", 10, 180.0, 0.9),
            TransactionSpeakerMetric("Kwame", 7, 140.0, 0.8)
        ),
        successMetrics = TransactionSuccessMetrics(autoSaved = 20, flaggedForReview = 5, manual = 3, successRate = 0.82),
        analyticsRange = AnalyticsRange.Last7Days
    )

    VoiceLedgerTheme {
        TransactionAnalyticsContent(
            uiState = TransactionAnalyticsUiState(
                isLoading = false,
                overview = overview,
                selectedRange = AnalyticsRange.Last7Days
            ),
            onRangeSelected = {},
            formatCurrency = { "GH₵%.2f".format(it) },
            onNavigateToSettings = {}
        )
    }
}
