package com.voiceledger.ghana.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Hosts the tabs — Today (sales), Insights (clarity & growth), Credit (debts) — behind a shared
 * bottom bar. Each screen keeps its own scaffold and renders the same bottom bar, so switching
 * tabs is seamless without a navigation library.
 */
@Composable
fun AppRoot(
    ledgerViewModel: LedgerViewModel,
    insightsViewModel: InsightsViewModel,
    expenseViewModel: ExpenseViewModel,
    creditViewModel: CreditViewModel,
    onRecordSale: () -> Unit,
    onRecordExpense: () -> Unit,
    onRecordCredit: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val bottomBar: @Composable () -> Unit = { AppBottomBar(tab) { tab = it } }

    when (tab) {
        0 -> LedgerScreen(
            viewModel = ledgerViewModel,
            onStartVoice = onRecordSale,
            bottomBar = bottomBar
        )
        1 -> InsightsScreen(
            viewModel = insightsViewModel,
            expenseViewModel = expenseViewModel,
            onRecordExpense = onRecordExpense,
            bottomBar = bottomBar
        )
        else -> CreditScreen(
            viewModel = creditViewModel,
            onStartVoice = onRecordCredit,
            bottomBar = bottomBar
        )
    }
}

@Composable
private fun AppBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = selected == 0,
            onClick = { onSelect(0) },
            icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
            label = { Text("Today") }
        )
        NavigationBarItem(
            selected = selected == 1,
            onClick = { onSelect(1) },
            icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
            label = { Text("Insights") }
        )
        NavigationBarItem(
            selected = selected == 2,
            onClick = { onSelect(2) },
            icon = { Icon(Icons.Filled.People, contentDescription = null) },
            label = { Text("Credit") }
        )
    }
}
