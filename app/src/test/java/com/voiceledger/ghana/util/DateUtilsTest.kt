package com.voiceledger.ghana.util

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.*

/**
 * Unit tests for DateUtils
 * Tests thread-safe date formatting operations
 */
class DateUtilsTest {

    @Test
    fun testFormatDate_withTimestamp() {
        // Given
        val timestamp = 1640995200000L // Jan 1, 2022 00:00:00 UTC
        
        // When
        val result = DateUtils.formatDate(timestamp)
        
        // Then
        // Result should be in YYYY-MM-DD format (may vary by timezone)
        assertNotNull("Should return formatted date", result)
        assertTrue("Should be in YYYY-MM-DD format", 
            result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun testGetTodayDateString() {
        // When
        val today = DateUtils.getTodayDateString()
        
        // Then
        assertNotNull("Should return today's date", today)
        assertTrue("Should be in YYYY-MM-DD format", 
            today.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        
        // Should match LocalDate.now() formatted the same way
        val expected = LocalDate.now().format(DateUtils.DATE_FORMATTER)
        assertEquals("Should match current date", expected, today)
    }

    @Test
    fun testFormatDate_withDate() {
        // Given
        val date = Date(1640995200000L) // Jan 1, 2022
        
        // When
        val result = DateUtils.formatDate(date)
        
        // Then
        assertNotNull("Should return formatted date", result)
        assertTrue("Should be in YYYY-MM-DD format", 
            result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun testParseDate() {
        // Given
        val dateString = "2024-01-15"
        
        // When
        val result = DateUtils.parseDate(dateString)
        
        // Then
        assertNotNull("Should parse date successfully", result)
        assertEquals("Year should be 2024", 2024, result.year)
        assertEquals("Month should be 1", 1, result.monthValue)
        assertEquals("Day should be 15", 15, result.dayOfMonth)
    }

    @Test
    fun testGetDateDaysAgo() {
        // Given
        val daysAgo = 5
        
        // When
        val result = DateUtils.getDateDaysAgo(daysAgo)
        
        // Then
        assertNotNull("Should return date string", result)
        assertTrue("Should be in YYYY-MM-DD format", 
            result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        
        // Parse and verify it's 5 days ago
        val parsedDate = DateUtils.parseDate(result)
        val expectedDate = LocalDate.now().minusDays(daysAgo.toLong())
        assertEquals("Should be 5 days ago", expectedDate, parsedDate)
    }

    @Test
    fun testGetDateDaysAhead() {
        // Given
        val daysAhead = 3
        
        // When
        val result = DateUtils.getDateDaysAhead(daysAhead)
        
        // Then
        assertNotNull("Should return date string", result)
        assertTrue("Should be in YYYY-MM-DD format", 
            result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        
        // Parse and verify it's 3 days ahead
        val parsedDate = DateUtils.parseDate(result)
        val expectedDate = LocalDate.now().plusDays(daysAhead.toLong())
        assertEquals("Should be 3 days ahead", expectedDate, parsedDate)
    }

    @Test
    fun testIsToday() {
        // Given
        val today = DateUtils.getTodayDateString()
        val yesterday = DateUtils.getDateDaysAgo(1)
        val tomorrow = DateUtils.getDateDaysAhead(1)
        
        // When & Then
        assertTrue("Should detect today correctly", DateUtils.isToday(today))
        assertFalse("Should not detect yesterday as today", DateUtils.isToday(yesterday))
        assertFalse("Should not detect tomorrow as today", DateUtils.isToday(tomorrow))
    }

    @Test
    fun testGetStartOfDayTimestamp() {
        // Given
        val dateString = "2024-01-15"
        
        // When
        val timestamp = DateUtils.getStartOfDayTimestamp(dateString)
        
        // Then
        assertTrue("Should return positive timestamp", timestamp > 0)
        
        // Parse back to verify it's start of day
        val date = DateUtils.formatDate(timestamp)
        assertEquals("Should be same date", dateString, date)
    }

    @Test
    fun testGetEndOfDayTimestamp() {
        // Given
        val dateString = "2024-01-15"
        
        // When
        val timestamp = DateUtils.getEndOfDayTimestamp(dateString)
        
        // Then
        assertTrue("Should return positive timestamp", timestamp > 0)
        
        // Should be greater than start of day timestamp
        val startTimestamp = DateUtils.getStartOfDayTimestamp(dateString)
        assertTrue("End should be after start", timestamp > startTimestamp)
        
        // Should be less than start of next day
        val nextDayStart = DateUtils.getStartOfDayTimestamp(DateUtils.getDateDaysAhead(1))
        assertTrue("End should be before next day start", timestamp < nextDayStart)
    }

    @Test
    fun testThreadSafety() {
        // Given
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<String>()
        val iterations = 100
        val latch = java.util.concurrent.CountDownLatch(iterations)
        
        // When - run multiple threads concurrently
        repeat(iterations) {
            val thread = Thread {
                try {
                    val result = DateUtils.getTodayDateString()
                    synchronized(results) {
                        results.add(result)
                    }
                } finally {
                    latch.countDown()
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        latch.await()
        
        // Then
        assertEquals("All threads should complete", iterations, results.size)
        
        // All results should be identical (same day)
        val firstResult = results.first()
        assertTrue("All results should be identical", 
            results.all { it == firstResult })
        
        // Result should be valid date format
        assertTrue("Should be valid date format", 
            firstResult.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun testConsistencyAcrossMethods() {
        // Given
        val timestamp = System.currentTimeMillis()
        val dateString = DateUtils.formatDate(timestamp)
        
        // When
        val parsedDate = DateUtils.parseDate(dateString)
        val reformattedDate = DateUtils.formatDate(timestamp)
        
        // Then
        assertEquals("Formatting should be consistent", dateString, reformattedDate)
        assertEquals("Parsed date should match formatted string", dateString, parsedDate.format(DateUtils.DATE_FORMATTER))
    }
}