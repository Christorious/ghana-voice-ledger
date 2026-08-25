package com.voiceledger.ghana.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.data.DayTotal
import com.voiceledger.ghana.data.Expense
import com.voiceledger.ghana.data.ExpenseCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    expenseViewModel: ExpenseViewModel,
    onRecordExpense: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expenses by expenseViewModel.recent.collectAsStateWithLifecycle()
    val draft by expenseViewModel.draft.collectAsStateWithLifecycle()
    val message by expenseViewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            expenseViewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRecordExpense,
                icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                text = { Text("Add expense") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Period.entries.forEach { p ->
                        FilterChip(
                            selected = state.period == p,
                            onClick = { viewModel.setPeriod(p) },
                            label = { Text(p.label) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                ProfitHero(state)
                Spacer(Modifier.height(20.dp))
                Text(
                    if (state.period == Period.MONTH) "Last 30 days" else "Last 7 days",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                TrendBars(state.daily)
                Spacer(Modifier.height(20.dp))
                Text("Best sellers", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (state.topProducts.isEmpty()) {
                item {
                    Text(
                        "No sales in this period yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(state.topProducts, key = { _, p -> "prod-${p.name}" }) { i, p ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${i + 1}. ${p.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${p.count} ${if (p.count == 1) "sale" else "sales"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "GHS ${"%.2f".format(p.total)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent expenses", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = expenseViewModel::beginManualEntry) { Text("Add") }
                }
                Spacer(Modifier.height(4.dp))
                if (expenses.isEmpty()) {
                    Text(
                        "No expenses recorded yet. Tap \"Add expense\" and say something like\n" +
                            "\"bought rice stock for 200 cedis\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(expenses, key = { _, e -> "exp-${e.id}" }) { _, e ->
                ExpenseRow(
                    expense = e,
                    onClick = { expenseViewModel.editExpense(e) },
                    onDelete = { expenseViewModel.delete(e) }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, viewModel.shareText(state))
                        }
                        context.startActivity(Intent.createChooser(send, "Share recap"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Share recap")
                }
            }
        }
    }

    draft?.let { current ->
        ExpenseEditSheet(
            draft = current,
            onDescription = expenseViewModel::onDescriptionChange,
            onAmount = expenseViewModel::onAmountChange,
            onCategory = expenseViewModel::onCategoryChange,
            onSave = expenseViewModel::saveDraft,
            onDismiss = expenseViewModel::dismissDraft
        )
    }
}

@Composable
private fun ProfitHero(state: InsightsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                "${state.period.label}'s profit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "GHS ${"%.2f".format(state.profit)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            state.profitGrowthPct?.let { g ->
                Spacer(Modifier.height(2.dp))
                Text(
                    "${if (g >= 0) "▲" else "▼"} ${kotlin.math.abs(g)}% vs last ${state.period.label.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                HeroStat(
                    label = "Sales",
                    value = "GHS ${"%.2f".format(state.total)}",
                    sub = "${state.count} ${if (state.count == 1) "sale" else "sales"}",
                    modifier = Modifier.weight(1f)
                )
                HeroStat(
                    label = "Expenses",
                    value = "GHS ${"%.2f".format(state.expenses)}",
                    sub = null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        sub?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${ExpenseCategory.fromName(expense.category).label} · ${formatTime(expense.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "− GHS ${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ExpenseEditSheet(
    draft: ExpenseDraft,
    onDescription: (String) -> Unit,
    onAmount: (String) -> Unit,
    onCategory: (ExpenseCategory) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == null) "Confirm expense" else "Edit expense") },
        text = {
            Column {
                draft.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = onDescription,
                    label = { Text("What for") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = draft.amount,
                    onValueChange = onAmount,
                    label = { Text("Amount (GHS)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseCategory.entries.forEach { c ->
                        FilterChip(
                            selected = draft.category == c,
                            onClick = { onCategory(c) },
                            label = { Text(c.label) }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TrendBars(daily: List<DayTotal>) {
    val max = (daily.maxOfOrNull { it.total } ?: 0.0).coerceAtLeast(1.0)
    if (daily.isEmpty()) {
        Text(
            "No sales to chart yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        daily.forEach { d ->
            val frac = (d.total / max).toFloat().coerceIn(0.03f, 1f)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(frac)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
