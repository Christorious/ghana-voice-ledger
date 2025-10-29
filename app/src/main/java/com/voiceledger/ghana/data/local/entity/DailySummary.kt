package com.voiceledger.ghana.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voiceledger.ghana.domain.service.*

/**
 * Entity representing a comprehensive daily summary of transactions
 * Contains aggregated data and insights for a specific day
 */
@Entity(
    tableName = "daily_summaries",
    indices = [
        Index(value = ["synced"]),
        Index(value = ["timestamp"]),
        Index(value = ["date", "synced"])
    ]
)
@TypeConverters(DailySummaryConverters::class)
data class DailySummary(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd
    val totalSales: Double,
    val transactionCount: Int,
    val uniqueCustomers: Int,
    val topProduct: String?,
    val topProductSales: Double,
    val peakHour: String?, // Format: HH (24-hour)
    val peakHourSales: Double,
    val averageTransactionValue: Double,
    val repeatCustomers: Int,
    val newCustomers: Int,
    val totalQuantitySold: Double,
    val mostProfitableHour: String?,
    val leastActiveHour: String?,
    val confidenceScore: Float, // Average confidence of all transactions
    val reviewedTransactions: Int, // Count of transactions that were reviewed
    val comparisonWithYesterday: ComparisonData?,
    val comparisonWithLastWeek: ComparisonData?,
    val productBreakdown: Map<String, ProductSummary>,
    val hourlyBreakdown: Map<String, HourlySummary>,
    val customerInsights: Map<String, CustomerInsight>,
    val recommendations: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

/**
 * Type converters for complex data types in DailySummary
 */
class DailySummaryConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromComparisonData(value: ComparisonData?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toComparisonData(value: String?): ComparisonData? {
        return value?.let { gson.fromJson(it, ComparisonData::class.java) }
    }
    
    @TypeConverter
    fun fromProductSummaryMap(value: Map<String, ProductSummary>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toProductSummaryMap(value: String): Map<String, ProductSummary> {
        val type = object : TypeToken<Map<String, ProductSummary>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromHourlySummaryMap(value: Map<String, HourlySummary>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toHourlySummaryMap(value: String): Map<String, HourlySummary> {
        val type = object : TypeToken<Map<String, HourlySummary>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromCustomerInsightMap(value: Map<String, CustomerInsight>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toCustomerInsightMap(value: String): Map<String, CustomerInsight> {
        val type = object : TypeToken<Map<String, CustomerInsight>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}