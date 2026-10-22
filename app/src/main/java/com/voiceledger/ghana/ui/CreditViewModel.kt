package com.voiceledger.ghana.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.data.Customer
import com.voiceledger.ghana.data.CustomerBalance
import com.voiceledger.ghana.data.Debt
import com.voiceledger.ghana.data.LedgerDatabase
import com.voiceledger.ghana.voice.DebtParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editable draft of a credit shown in the confirm/edit sheet. */
data class DebtDraft(
    val id: Long? = null,
    val customerId: Long? = null,
    val customerName: String = "",
    val nameLocked: Boolean = false,
    val amount: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val hint: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CreditViewModel(app: Application) : AndroidViewModel(app) {

    private val db = LedgerDatabase.get(app)
    private val customerDao = db.customerDao()
    private val debtDao = db.debtDao()

    val debtors: StateFlow<List<CustomerBalance>> =
        customerDao.observeDebtors()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalOutstanding: StateFlow<Double> =
        debtDao.observeTotalOutstanding()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    val selectedCustomer: StateFlow<Customer?> =
        _selectedCustomerId.flatMapLatest { id ->
            if (id == null) flowOf(null) else customerDao.observeById(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedCustomerDebts: StateFlow<List<Debt>> =
        _selectedCustomerId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else debtDao.observeByCustomer(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow<DebtDraft?>(null)
    val draft: StateFlow<DebtDraft?> = _draft.asStateFlow()

    private val _paymentTarget = MutableStateFlow<Debt?>(null)
    val paymentTarget: StateFlow<Debt?> = _paymentTarget.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // --- navigation within the Credit tab ---
    fun selectCustomer(id: Long) { _selectedCustomerId.value = id }
    fun clearSelection() { _selectedCustomerId.value = null }

    // --- creating / editing credit ---
    fun beginCreditFromText(text: String) {
        val parsed = DebtParser.parse(text)
        _draft.value = if (parsed != null) {
            DebtDraft(
                customerName = parsed.customerName,
                amount = formatAmount(parsed.amount),
                note = parsed.note,
                hint = "Heard: “$text” — check and save"
            )
        } else {
            DebtDraft(
                customerName = "",
                note = text.trim(),
                hint = "Couldn’t read the amount — please add it"
            )
        }
    }

    /** Add a new credit for a brand-new customer (name editable). */
    fun beginNewCredit() {
        _draft.value = DebtDraft()
    }

    /** Add another credit to the customer currently open in detail. */
    fun addCreditToSelectedCustomer() {
        val customer = selectedCustomer.value ?: return
        _draft.value = DebtDraft(customerId = customer.id, customerName = customer.name, nameLocked = true)
    }

    fun editDebt(debt: Debt) {
        val name = selectedCustomer.value?.name ?: ""
        _draft.value = DebtDraft(
            id = debt.id,
            customerId = debt.customerId,
            customerName = name,
            nameLocked = true,
            amount = formatAmount(debt.amount),
            note = debt.note,
            timestamp = debt.timestamp
        )
    }

    fun onNameChange(value: String) = _draft.update { it?.copy(customerName = value) }
    fun onAmountChange(value: String) = _draft.update { it?.copy(amount = value) }
    fun onNoteChange(value: String) = _draft.update { it?.copy(note = value) }

    fun saveDraft() {
        val current = _draft.value ?: return
        val amount = current.amount.trim().replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _message.value = "Please enter a valid amount"
            return
        }
        viewModelScope.launch {
            val customerId = current.customerId ?: findOrCreateCustomer(current.customerName)
            if (current.id == null) {
                debtDao.insert(Debt(customerId = customerId, amount = amount, note = current.note.trim()))
                _message.value = "Credit recorded"
            } else {
                debtDao.update(
                    Debt(
                        id = current.id,
                        customerId = customerId,
                        amount = amount,
                        amountPaid = existingPaidFor(current.id),
                        note = current.note.trim(),
                        timestamp = current.timestamp
                    )
                )
                _message.value = "Credit updated"
            }
            _draft.value = null
        }
    }

    fun dismissDraft() { _draft.value = null }

    // --- payments ---
    fun beginPayment(debt: Debt) { _paymentTarget.value = debt }
    fun dismissPayment() { _paymentTarget.value = null }

    fun confirmPayment(amountText: String) {
        val debt = _paymentTarget.value ?: return
        val pay = amountText.trim().replace(",", ".").toDoubleOrNull()
        if (pay == null || pay <= 0.0) {
            _message.value = "Please enter a valid amount"
            return
        }
        val newPaid = (debt.amountPaid + pay).coerceAtMost(debt.amount)
        viewModelScope.launch {
            debtDao.update(debt.copy(amountPaid = newPaid))
            _message.value = if (newPaid >= debt.amount - 0.0049) "Debt settled 🎉" else "Payment recorded"
            _paymentTarget.value = null
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch { debtDao.delete(debt) }
    }

    fun clearMessage() { _message.value = null }

    // --- helpers ---
    private suspend fun findOrCreateCustomer(rawName: String): Long {
        val name = rawName.trim().ifBlank { "Customer" }
        return customerDao.findByName(name)?.id ?: customerDao.insert(Customer(name = name))
    }

    private fun existingPaidFor(debtId: Long): Double =
        selectedCustomerDebts.value.firstOrNull { it.id == debtId }?.amountPaid ?: 0.0

    private fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
}
