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
import kotlinx.coroutines.launch
import java.util.Calendar

class LedgerViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = LedgerDatabase.get(app).transactionDao()

    val transactions: StateFlow<List<Transaction>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayTotal: StateFlow<Double> =
        dao.observeTotalSince(startOfToday())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Parse a spoken/typed phrase and, if understood, record it. */
    fun recordFromText(text: String) {
        val parsed = TransactionParser.parse(text)
        if (parsed == null) {
            _message.value = "Couldn't understand \"$text\". Try e.g. \"sold 3 tilapia for 20 cedis\"."
            return
        }
        viewModelScope.launch {
            dao.insert(
                Transaction(
                    description = parsed.description,
                    amount = parsed.amount,
                    quantity = parsed.quantity,
                    rawText = text
                )
            )
            _message.value = "Recorded ${parsed.description} — GHS ${"%.2f".format(parsed.amount)}"
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { dao.delete(transaction) }
    }

    fun clearMessage() {
        _message.value = null
    }

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
