package com.voiceledger.ghana.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.data.CustomerBalance
import com.voiceledger.ghana.data.Debt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditScreen(
    viewModel: CreditViewModel,
    onStartVoice: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val debtors by viewModel.debtors.collectAsStateWithLifecycle()
    val total by viewModel.totalOutstanding.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedCustomerId.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val customerDebts by viewModel.selectedCustomerDebts.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val payment by viewModel.paymentTarget.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val inDetail = selectedId != null
    BackHandler(enabled = inDetail) { viewModel.clearSelection() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (inDetail) (selectedCustomer?.name ?: "Customer") else "Credit") },
                navigationIcon = {
                    if (inDetail) {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (inDetail) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::addCreditToSelectedCustomer,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add credit") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = onStartVoice,
                    icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                    text = { Text("Record credit") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (inDetail) {
                CustomerDetail(
                    debts = customerDebts,
                    onPay = viewModel::beginPayment,
                    onEdit = viewModel::editDebt,
                    onDelete = viewModel::deleteDebt
                )
            } else {
                DebtorList(
                    total = total,
                    debtors = debtors,
                    onOpen = { viewModel.selectCustomer(it.customerId) },
                    onAddManual = viewModel::beginNewCredit
                )
            }
        }
    }

    draft?.let { current ->
        EditDebtSheet(
            draft = current,
            onName = viewModel::onNameChange,
            onAmount = viewModel::onAmountChange,
            onNote = viewModel::onNoteChange,
            onSave = viewModel::saveDraft,
            onDismiss = viewModel::dismissDraft
        )
    }

    payment?.let { debt ->
        PaymentDialog(
            debt = debt,
            onConfirm = viewModel::confirmPayment,
            onDismiss = viewModel::dismissPayment
        )
    }
}

@Composable
private fun DebtorList(
    total: Double,
    debtors: List<CustomerBalance>,
    onOpen: (CustomerBalance) -> Unit,
    onAddManual: () -> Unit
) {
    Spacer(Modifier.height(4.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                "Owed to you",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "GHS ${"%.2f".format(total)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                if (debtors.isEmpty()) "No one owes you" else "${debtors.size} ${if (debtors.size == 1) "person" else "people"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("People who owe", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAddManual) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Add")
        }
    }
    Spacer(Modifier.height(4.dp))

    if (debtors.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Nobody owes you 🎉", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap \"Record credit\" and say\n\"Ama owes 20 cedis for fish\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(debtors, key = { it.customerId }) { debtor ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(debtor) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                debtor.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${debtor.debtCount} ${if (debtor.debtCount == 1) "credit" else "credits"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "GHS ${"%.2f".format(debtor.outstanding)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerDetail(
    debts: List<Debt>,
    onPay: (Debt) -> Unit,
    onEdit: (Debt) -> Unit,
    onDelete: (Debt) -> Unit
) {
    val outstanding = debts.sumOf { it.outstanding }
    Spacer(Modifier.height(4.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                "Balance owed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "GHS ${"%.2f".format(outstanding)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("Credits", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(debts, key = { it.id }) { debt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(debt) },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                debt.note.ifBlank { "Credit" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                formatDate(debt.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "GHS ${"%.2f".format(debt.outstanding)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (debt.isSettled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary
                            )
                            if (debt.amountPaid > 0.0 && !debt.isSettled) {
                                Text(
                                    "of GHS ${"%.2f".format(debt.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (debt.isSettled) {
                            Text(
                                "Settled ✓",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FilledTonalButton(onClick = { onPay(debt) }) { Text("Received") }
                        }
                        IconButton(onClick = { onDelete(debt) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditDebtSheet(
    draft: DebtDraft,
    onName: (String) -> Unit,
    onAmount: (String) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == null) "Record credit" else "Edit credit") },
        text = {
            Column {
                draft.hint?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = draft.customerName,
                    onValueChange = onName,
                    label = { Text("Customer") },
                    singleLine = true,
                    enabled = !draft.nameLocked,
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
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = onNote,
                    label = { Text("For (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PaymentDialog(
    debt: Debt,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(debt.id) {
        mutableStateOf(if (debt.outstanding % 1.0 == 0.0) debt.outstanding.toInt().toString() else debt.outstanding.toString())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column {
                Text(
                    "Outstanding: GHS ${"%.2f".format(debt.outstanding)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("How much did they pay?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
