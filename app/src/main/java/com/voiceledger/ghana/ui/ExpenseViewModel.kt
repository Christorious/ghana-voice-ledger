package com.voiceledger.ghana.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.data.Expense
import com.voiceledger.ghana.data.ExpenseCategory
import com.voiceledger.ghana.data.LedgerDatabase
import com.voiceledger.ghana.voice.ExpenseParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editable draft of an expense shown in the confirm/edit sheet.
 * `id == null` means a new entry; otherwise we're editing an existing one.
 */
data class ExpenseDraft(
    val id: Long? = null,
    val description: String = "",
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val rawText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = LedgerDatabase.get(app).expenseDao()

    val recent: StateFlow<List<Expense>> =
        dao.observeRecent(20).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow<ExpenseDraft?>(null)
    val draft: StateFlow<ExpenseDraft?> = _draft.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Voice or quick text input: parse, then open the confirm/edit sheet (never saves silently). */
    fun beginFromText(text: String) {
        val parsed = ExpenseParser.parse(text)
        _draft.value = if (parsed != null) {
            ExpenseDraft(
                description = parsed.description,
                amount = formatAmount(parsed.amount),
                category = parsed.category,
                rawText = text,
                note = "Heard: “$text” — check and save"
            )
        } else {
            ExpenseDraft(
                description = text.trim().replaceFirstChar { it.uppercase() },
                amount = "",
                rawText = text,
                note = "Couldn’t read the amount — please add it"
            )
        }
    }

    /** Open an empty sheet for manual entry. */
    fun beginManualEntry() {
        _draft.value = ExpenseDraft()
    }

    /** Open the sheet to edit an existing entry. */
    fun editExpense(expense: Expense) {
        _draft.value = ExpenseDraft(
            id = expense.id,
            description = expense.description,
            amount = formatAmount(expense.amount),
            category = ExpenseCategory.fromName(expense.category),
            rawText = expense.rawText,
            timestamp = expense.timestamp
        )
    }

    fun onDescriptionChange(value: String) = _draft.update { it?.copy(description = value) }
    fun onAmountChange(value: String) = _draft.update { it?.copy(amount = value) }
    fun onCategoryChange(value: ExpenseCategory) = _draft.update { it?.copy(category = value) }

    /** Validate and persist the current draft (insert or update). */
    fun saveDraft() {
        val current = _draft.value ?: return
        val amount = current.amount.trim().replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _message.value = "Please enter a valid amount"
            return
        }
        val description = current.description.trim().ifBlank { current.category.label }

        viewModelScope.launch {
            if (current.id == null) {
                dao.insert(
                    Expense(
                        description = description,
                        amount = amount,
                        category = current.category.name,
                        rawText = current.rawText
                    )
                )
                _message.value = "Saved $description — GHS ${"%.2f".format(amount)}"
            } else {
                dao.update(
                    Expense(
                        id = current.id,
                        description = description,
                        amount = amount,
                        category = current.category.name,
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

    fun delete(expense: Expense) {
        viewModelScope.launch { dao.delete(expense) }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
}
