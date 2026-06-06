package com.example

import com.example.utils.VoiceNlpParser
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceNlpParserTest {

    @Test
    fun testSureshSeLiye() {
        val result = VoiceNlpParser.parse("Suresh se ₹500 liye")
        assertEquals("Suresh", result.name)
        assertEquals("credit", result.intent)
        assertEquals(500.0, result.amount, 0.01)
    }

    @Test
    fun testSureshNeDiye() {
        val result = VoiceNlpParser.parse("Suresh ne 500 rupaye diye")
        assertEquals("Suresh", result.name)
        assertEquals("credit", result.intent)
        assertEquals(500.0, result.amount, 0.01)
    }

    @Test
    fun testSureshSeUdharLiya() {
        val result = VoiceNlpParser.parse("Suresh se udhar liya")
        assertEquals("Suresh", result.name)
        assertEquals("credit", result.intent)
        // Defaults to 500.0 as there are no numbers in phrase
        assertEquals(500.0, result.amount, 0.01)
    }

    @Test
    fun testSureshKoDiya() {
        val result = VoiceNlpParser.parse("Suresh ko 500 rupaye diya")
        assertEquals("Suresh", result.name)
        assertEquals("debit", result.intent)
        assertEquals(500.0, result.amount, 0.01)
    }

    @Test
    fun testSureshSeMila() {
        val result = VoiceNlpParser.parse("Suresh se 150 rupaye mila")
        assertEquals("Suresh", result.name)
        assertEquals("credit", result.intent)
        assertEquals(150.0, result.amount, 0.01)
    }
}
