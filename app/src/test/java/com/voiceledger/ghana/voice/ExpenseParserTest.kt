package com.voiceledger.ghana.voice

import com.voiceledger.ghana.data.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseParserTest {

    @Test
    fun `parses restock phrase as stock`() {
        val r = ExpenseParser.parse("bought rice stock for 200 cedis")!!
        assertEquals(200.0, r.amount, 0.001)
        assertEquals(ExpenseCategory.STOCK, r.category)
        assertTrue(r.description.contains("Rice"))
    }

    @Test
    fun `parses transport phrase as transport`() {
        val r = ExpenseParser.parse("paid 5 cedis trotro")!!
        assertEquals(5.0, r.amount, 0.001)
        assertEquals(ExpenseCategory.TRANSPORT, r.category)
    }

    @Test
    fun `parses light bill as utilities`() {
        val r = ExpenseParser.parse("light bill 40 cedis")!!
        assertEquals(40.0, r.amount, 0.001)
        assertEquals(ExpenseCategory.UTILITIES, r.category)
    }

    @Test
    fun `parses table toll as rent`() {
        val r = ExpenseParser.parse("paid table toll 3 cedis")!!
        assertEquals(3.0, r.amount, 0.001)
        assertEquals(ExpenseCategory.RENT, r.category)
    }

    @Test
    fun `parses wages phrase`() {
        val r = ExpenseParser.parse("gave the boy 20 cedis wages")!!
        assertEquals(20.0, r.amount, 0.001)
        assertEquals(ExpenseCategory.WAGES, r.category)
    }

    @Test
    fun `unknown spending falls back to other`() {
        val r = ExpenseParser.parse("miscellaneous 15 cedis")!!
        assertEquals(15.0, r.amount, 0.001)
        assertEquals(ExpenseCategory.OTHER, r.category)
    }

    @Test
    fun `parses number words and pesewas`() {
        val r = ExpenseParser.parse("bought fuel for twenty cedis 50 pesewas")!!
        assertEquals(20.50, r.amount, 0.001)
        assertEquals(ExpenseCategory.TRANSPORT, r.category)
    }

    @Test
    fun `falls back to category label when description empties out`() {
        val r = ExpenseParser.parse("paid 8 cedis for transport")!!
        assertEquals(8.0, r.amount, 0.001)
        assertEquals(ExpenseCategory.TRANSPORT, r.category)
        assertEquals("Transport", r.description)
    }

    @Test
    fun `returns null for blank input`() {
        assertNull(ExpenseParser.parse(""))
        assertNull(ExpenseParser.parse("   "))
    }

    @Test
    fun `returns null when no amount present`() {
        assertNull(ExpenseParser.parse("bought some rice"))
    }
}
