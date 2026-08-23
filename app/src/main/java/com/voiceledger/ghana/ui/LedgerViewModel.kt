package com.voiceledger.ghana.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.data.LedgerDatabase
import com.voiceledger.ghana.data.Transaction
import com.voiceledger.ghana.voice.TransactionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Editable draft of a sale shown in the confirm/edit sheet.
 * `id == null` means a new entry; otherwise we're editing an existing one.
 * Amount/quantity are strings so they bind directly to text fields.
 */
data class TransactionDraft(
    val id: Long? = null,
    val description: String = "",
    val amount: String = "",
    val quantity: String = "",
    val rawText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

class LedgerViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = LedgerDatabase.get(app).transactionDao()

    val transactions: StateFlow<List<Transaction>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayTotal: StateFlow<Double> =
        dao.observeTotalSince(startOfToday())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _draft = MutableStateFlow<TransactionDraft?>(null)
    val draft: StateFlow<TransactionDraft?> = _draft.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Voice or quick text input: parse, then open the confirm/edit sheet (never saves silently). */
    fun beginFromText(text: String) {
        val parsed = TransactionParser.parse(text)
        _draft.value = if (parsed != null) {
            TransactionDraft(
                description = parsed.description,
                amount = formatAmount(parsed.amount),
                quantity = parsed.quantity?.toString() ?: "",
                rawText = text,
                note = "Heard: “$text” — check and save"
            )
        } else {
            TransactionDraft(
                description = text.trim().replaceFirstChar { it.uppercase() },
                amount = "",
                rawText = text,
                note = "Couldn’t read the amount — please add it"
            )
        }
    }

    /** Open an empty sheet for manual entry. */
    fun beginManualEntry() {
        _draft.value = TransactionDraft()
    }

    /** Open the sheet to edit an existing entry. */
    fun editTransaction(transaction: Transaction) {
        _draft.value = TransactionDraft(
            id = transaction.id,
            description = transaction.description,
            amount = formatAmount(transaction.amount),
            quantity = transaction.quantity?.toString() ?: "",
            rawText = transaction.rawText,
            timestamp = transaction.timestamp
        )
    }

    fun onDescriptionChange(value: String) = _draft.update { it?.copy(description = value) }
    fun onAmountChange(value: String) = _draft.update { it?.copy(amount = value) }
    fun onQuantityChange(value: String) = _draft.update { it?.copy(quantity = value) }

    /** Validate and persist the current draft (insert or update). */
    fun saveDraft() {
        val current = _draft.value ?: return
        val amount = current.amount.trim().replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _message.value = "Please enter a valid amount"
            return
        }
        val description = current.description.trim().ifBlank { "Sale" }
        val quantity = current.quantity.trim().toIntOrNull()

        viewModelScope.launch {
            if (current.id == null) {
                dao.insert(
                    Transaction(
                        description = description,
                        amount = amount,
                        quantity = quantity,
                        rawText = current.rawText
                    )
                )
                _message.value = "Saved $description — GHS ${"%.2f".format(amount)}"
            } else {
                dao.update(
                    Transaction(
                        id = current.id,
                        description = description,
                        amount = amount,
                        quantity = quantity,
                        timestamp = current.timestamp,
                        rawText = current.rawText
                    )
                )
                _message.value = "Updated $description"
            }
            _draft.value = null
        }
    }

    fun dismissDraft() {
        _draft.value = null
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { dao.delete(transaction) }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

    private fun startOfToday(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
