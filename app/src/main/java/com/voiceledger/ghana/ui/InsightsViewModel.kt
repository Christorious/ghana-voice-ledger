package com.voiceledger.ghana.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voiceledger.ghana.data.DayTotal
import com.voiceledger.ghana.data.LedgerDatabase
import com.voiceledger.ghana.data.ProductTotal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import kotlin.math.roundToInt

enum class Period(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month") }

data class InsightsState(
    val period: Period = Period.DAY,
    val total: Double = 0.0,          // sales in the period
    val count: Int = 0,
    val salesGrowthPct: Int? = null,  // sales vs previous same-length period; null = no data
    val expenses: Double = 0.0,       // costs in the period
    val profit: Double = 0.0,         // sales − expenses
    val profitGrowthPct: Int? = null, // profit vs previous period; null = no positive baseline
    val topProducts: List<ProductTotal> = emptyList(),
    val daily: List<DayTotal> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = LedgerDatabase.get(app)
    private val dao = db.transactionDao()
    private val expenseDao = db.expenseDao()

    private val _period = MutableStateFlow(Period.DAY)
    val period: StateFlow<Period> = _period.asStateFlow()

    fun setPeriod(p: Period) { _period.value = p }

    /** Sales-side aggregates, kept together so the expense flows can be combined on top. */
    private data class SalesAgg(
        val total: Double,
        val count: Int,
        val prevTotal: Double,
        val top: List<ProductTotal>,
        val daily: List<DayTotal>
    )

    val state: StateFlow<InsightsState> = _period.flatMapLatest { p ->
        val b = bounds(p)
        val trendSince = startOfDaysAgo(if (p == Period.MONTH) 29 else 6)
        val sales = combine(
            dao.observeTotalBetween(b.curStart, b.curEnd),
            dao.observeCountBetween(b.curStart, b.curEnd),
            dao.observeTotalBetween(b.prevStart, b.curStart),
            dao.observeTopProducts(b.curStart, b.curEnd, 5),
            dao.observeDailyTotals(trendSince)
        ) { total, count, prevTotal, top, daily ->
            SalesAgg(total, count, prevTotal, top, daily)
        }
        combine(
            sales,
            expenseDao.observeTotalBetween(b.curStart, b.curEnd),
            expenseDao.observeTotalBetween(b.prevStart, b.curStart)
        ) { s, expenses, prevExpenses ->
            InsightsState(
                period = p,
                total = s.total,
                count = s.count,
                salesGrowthPct = growth(s.total, s.prevTotal),
                expenses = expenses,
                profit = s.total - expenses,
                profitGrowthPct = growth(s.total - expenses, s.prevTotal - prevExpenses),
                topProducts = s.top,
                daily = s.daily
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())

    /** A short recap for the system share sheet (WhatsApp etc.). */
    fun shareText(s: InsightsState): String = buildString {
        appendLine("Ghana Voice Ledger — ${s.period.label} recap")
        appendLine("Sales: GHS ${"%.2f".format(s.total)} from ${s.count} ${if (s.count == 1) "sale" else "sales"}")
        if (s.expenses > 0.0049) appendLine("Expenses: GHS ${"%.2f".format(s.expenses)}")
        appendLine("Profit: GHS ${"%.2f".format(s.profit)}")
        s.profitGrowthPct?.let { appendLine("${if (it >= 0) "▲" else "▼"} ${kotlin.math.abs(it)}% vs previous ${s.period.label.lowercase()}") }
        s.topProducts.firstOrNull()?.let { appendLine("Top: ${it.name} (GHS ${"%.2f".format(it.total)})") }
    }.trim()

    // --- period maths ---

    private data class Bounds(val curStart: Long, val curEnd: Long, val prevStart: Long)

    private fun bounds(p: Period): Bounds = when (p) {
        Period.DAY -> {
            val start = startOfToday()
            Bounds(start, addDays(start, 1), addDays(start, -1))
        }
        Period.WEEK -> {
            val start = startOfWeek()
            Bounds(start, addDays(start, 7), addDays(start, -7))
        }
        Period.MONTH -> {
            val start = startOfMonth()
            Bounds(start, addMonths(start, 1), addMonths(start, -1))
        }
    }

    private fun growth(total: Double, prev: Double): Int? =
        if (prev <= 0.0049) null else (((total - prev) / prev) * 100).roundToInt()

    private fun midnight(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfToday(): Long = midnight(Calendar.getInstance())

    private fun startOfDaysAgo(days: Int): Long =
        midnight(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) })

    private fun startOfWeek(): Long {
        val c = Calendar.getInstance()
        val fromMonday = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        c.add(Calendar.DAY_OF_YEAR, -fromMonday)
        return midnight(c)
    }

    private fun startOfMonth(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        return midnight(c)
    }

    private fun addDays(start: Long, days: Int): Long =
        Calendar.getInstance().apply { timeInMillis = start; add(Calendar.DAY_OF_YEAR, days) }.timeInMillis

    private fun addMonths(start: Long, months: Int): Long =
        Calendar.getInstance().apply { timeInMillis = start; add(Calendar.MONTH, months) }.timeInMillis
}
