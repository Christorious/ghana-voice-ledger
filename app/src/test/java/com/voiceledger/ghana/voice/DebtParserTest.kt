package com.voiceledger.ghana.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DebtParserTest {

    @Test
    fun `parses owes phrase with note`() {
        val r = DebtParser.parse("Ama owes 20 cedis for fish")!!
        assertEquals("Ama", r.customerName)
        assertEquals(20.0, r.amount, 0.001)
        assertEquals("Fish", r.note)
    }

    @Test
    fun `parses credit keyword`() {
        val r = DebtParser.parse("Kofi credit 5 cedis")!!
        assertEquals("Kofi", r.customerName)
        assertEquals(5.0, r.amount, 0.001)
        assertEquals("", r.note)
    }

    @Test
    fun `parses give phrase without keyword`() {
        val r = DebtParser.parse("give Adwoa 15 cedis")!!
        assertEquals("Adwoa", r.customerName)
        assertEquals(15.0, r.amount, 0.001)
    }

    @Test
    fun `parses compound number amount`() {
        val r = DebtParser.parse("Yaw owes twenty five cedis")!!
        assertEquals("Yaw", r.customerName)
        assertEquals(25.0, r.amount, 0.001)
    }

    @Test
    fun `falls back to Customer when no name`() {
        val r = DebtParser.parse("owes 10 cedis")!!
        assertEquals("Customer", r.customerName)
        assertEquals(10.0, r.amount, 0.001)
    }

    @Test
    fun `returns null without amount`() {
        assertNull(DebtParser.parse("Ama owes for fish"))
        assertNull(DebtParser.parse(""))
    }
}
