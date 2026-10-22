package com.voiceledger.ghana.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionParserTest {

    @Test
    fun `parses classic sold-for phrase`() {
        val r = TransactionParser.parse("sold 3 tilapia for 20 cedis")!!
        assertEquals("Tilapia", r.description)
        assertEquals(3, r.quantity)
        assertEquals(20.0, r.amount, 0.001)
    }

    @Test
    fun `parses leading quantity with trailing currency`() {
        val r = TransactionParser.parse("two yams 15 cedis")!!
        assertEquals("Yam", r.description)
        assertEquals(2, r.quantity)
        assertEquals(15.0, r.amount, 0.001)
    }

    @Test
    fun `parses bare product and trailing number as amount`() {
        val r = TransactionParser.parse("gari 5")!!
        assertEquals("Gari", r.description)
        assertNull(r.quantity)
        assertEquals(5.0, r.amount, 0.001)
    }

    @Test
    fun `parses compound english numbers`() {
        val r = TransactionParser.parse("twenty five cedis rice")!!
        assertEquals("Rice", r.description)
        assertEquals(25.0, r.amount, 0.001)
        assertNull(r.quantity)
    }

    @Test
    fun `parses hundreds`() {
        val r = TransactionParser.parse("one hundred twenty cedis")!!
        assertEquals(120.0, r.amount, 0.001)
    }

    @Test
    fun `parses cedis and pesewas as fractional amount`() {
        val r = TransactionParser.parse("5 cedis 50 pesewas bread")!!
        assertEquals("Bread", r.description)
        assertEquals(5.50, r.amount, 0.001)
    }

    @Test
    fun `parses pesewas only`() {
        val r = TransactionParser.parse("50 pesewas koko")!!
        assertEquals("Koko", r.description)
        assertEquals(0.50, r.amount, 0.001)
    }

    @Test
    fun `parses twi number word`() {
        val r = TransactionParser.parse("baako kenkey for 3 cedis")!!
        assertEquals("Kenkey", r.description)
        assertEquals(1, r.quantity)
        assertEquals(3.0, r.amount, 0.001)
    }

    @Test
    fun `parses ewe number word`() {
        val r = TransactionParser.parse("eve banku 10 cedis")!!
        assertEquals("Banku", r.description)
        assertEquals(2, r.quantity)
        assertEquals(10.0, r.amount, 0.001)
    }

    @Test
    fun `canonicalises common speech-to-text mishears`() {
        assertEquals("Tilapia", TransactionParser.parse("talapia 8 cedis")!!.description)
        assertEquals("Waakye", TransactionParser.parse("wakye 12 cedis")!!.description)
    }

    @Test
    fun `does not treat amount as quantity when no product`() {
        val r = TransactionParser.parse("sold 20 cedis")!!
        assertEquals("Sale", r.description)
        assertNull(r.quantity)
        assertEquals(20.0, r.amount, 0.001)
    }

    @Test
    fun `returns null for blank input`() {
        assertNull(TransactionParser.parse(""))
        assertNull(TransactionParser.parse("   "))
    }

    @Test
    fun `returns null when no amount present`() {
        assertNull(TransactionParser.parse("hello there"))
        assertNull(TransactionParser.parse("just some tilapia"))
    }

    @Test
    fun `keeps unknown products but title-cases them`() {
        val r = TransactionParser.parse("mango 4 cedis")!!
        assertEquals("Mango", r.description)
        assertEquals(4.0, r.amount, 0.001)
    }

    @Test
    fun `handles multi-word descriptions`() {
        val r = TransactionParser.parse("sold groundnut and rice for 7 cedis")!!
        assertTrue(r.description.contains("Groundnut"))
        assertTrue(r.description.contains("Rice"))
        assertEquals(7.0, r.amount, 0.001)
    }
}
