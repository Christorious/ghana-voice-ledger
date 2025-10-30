package com.voiceledger.ghana.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Thread-safe utility class for date formatting operations
 * Replaces SimpleDateFormat usage throughout the application
 */
object DateUtils {
    
    val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DEFAULT_ZONE_ID = ZoneId.systemDefault()
    
    /**
     * Format a timestamp to YYYY-MM-DD string format
     * @param timestamp Unix timestamp in milliseconds
     * @return Date string in YYYY-MM-DD format
     */
    fun formatDate(timestamp: Long): String {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            DEFAULT_ZONE_ID
        ).format(DATE_FORMATTER)
    }
    
    /**
     * Get today's date in YYYY-MM-DD format
     * @return Today's date string
     */
    fun getTodayDateString(): String {
        return LocalDate.now().format(DATE_FORMATTER)
    }
    
    /**
     * Format a Date object to YYYY-MM-DD string format
     * @param date Date to format
     * @return Date string in YYYY-MM-DD format
     */
    fun formatDate(date: Date): String {
        return date.toInstant()
            .atZone(DEFAULT_ZONE_ID)
            .toLocalDate()
            .format(DATE_FORMATTER)
    }
    
    /**
     * Parse a YYYY-MM-DD string to LocalDate
     * @param dateString Date string in YYYY-MM-DD format
     * @return LocalDate object
     */
    fun parseDate(dateString: String): LocalDate {
        return LocalDate.parse(dateString, DATE_FORMATTER)
    }
    
    /**
     * Get date string for a specific number of days ago
     * @param daysAgo Number of days to go back
     * @return Date string in YYYY-MM-DD format
     */
    fun getDateDaysAgo(daysAgo: Int): String {
        return LocalDate.now().minusDays(daysAgo.toLong()).format(DATE_FORMATTER)
    }
    
    /**
     * Get date string for a specific number of days in the future
     * @param daysAhead Number of days to go forward
     * @return Date string in YYYY-MM-DD format
     */
    fun getDateDaysAhead(daysAhead: Int): String {
        return LocalDate.now().plusDays(daysAhead.toLong()).format(DATE_FORMATTER)
    }
    
    /**
     * Check if a date string represents today
     * @param dateString Date string in YYYY-MM-DD format
     * @return True if the date is today
     */
    fun isToday(dateString: String): Boolean {
        return dateString == getTodayDateString()
    }
    
    /**
     * Get the start of day timestamp for a given date string
     * @param dateString Date string in YYYY-MM-DD format
     * @return Start of day timestamp in milliseconds
     */
    fun getStartOfDayTimestamp(dateString: String): Long {
        return LocalDate.parse(dateString, DATE_FORMATTER)
            .atStartOfDay(DEFAULT_ZONE_ID)
            .toInstant()
            .toEpochMilli()
    }
    
    /**
     * Get the end of day timestamp for a given date string
     * @param dateString Date string in YYYY-MM-DD format
     * @return End of day timestamp in milliseconds
     */
    fun getEndOfDayTimestamp(dateString: String): Long {
        return LocalDate.parse(dateString, DATE_FORMATTER)
            .atTime(23, 59, 59, 999_999_999)
            .atZone(DEFAULT_ZONE_ID)
            .toInstant()
            .toEpochMilli()
    }
}