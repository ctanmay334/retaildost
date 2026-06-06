package com.example.utils

data class ParsedVoiceEntry(
    val name: String,
    val intent: String, // "debit" or "credit"
    val amount: Double
)

object VoiceNlpParser {
    fun parse(phrase: String): ParsedVoiceEntry {
        var intent = "debit"
        var amount = 500.0

        val lower = phrase.lowercase()
        val hasSe = lower.contains("se")
        val hasKo = lower.contains("ko")
        val hasNe = lower.contains("ne")
        val hasLiye = lower.contains("liye") || lower.contains("liya")
        val hasDiya = lower.contains("diya") || lower.contains("diye")
        val hasMila = lower.contains("mila")
        val hasUdhar = lower.contains("udhar") || lower.contains("udhaar")

        if (hasSe && hasLiye) {
            intent = "credit"
        } else if (hasNe && hasDiya) {
            intent = "credit"
        } else if (hasSe && hasUdhar && hasLiye) {
            intent = "credit"
        } else if (hasSe && hasMila) {
            intent = "credit"
        } else if (hasKo && hasDiya) {
            intent = "debit"
        } else if (hasKo && hasUdhar) {
            intent = "debit"
        } else if (phrase.contains("diye", ignoreCase = true) || 
            phrase.contains("back", ignoreCase = true) || 
            phrase.contains("mila", ignoreCase = true) ||
            phrase.contains("jama", ignoreCase = true) ||
            phrase.contains("pay", ignoreCase = true) ||
            phrase.contains("credit", ignoreCase = true)) {
            intent = "credit"
        }

        // Try extracting numbers for amount
        val numbers = "\\d+".toRegex().findAll(phrase).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()
        if (numbers.isNotEmpty()) {
            amount = numbers[0]
        }

        // Parse Name Heuristics:
        // Clean and split phrase into words
        val words = phrase.trim().split("\\s+".toRegex())
        val nameWords = mutableListOf<String>()

        // Look for common Hinglish markers: "ka", "ko", "ne", "se", "pe"
        val markers = listOf("ka", "ko", "ne", "se", "pe", "ki")
        var markerIndex = -1
        for (i in words.indices) {
            if (markers.contains(words[i].lowercase())) {
                markerIndex = i
                break
            }
        }

        if (markerIndex > 0) {
            // Take all words before the marker
            for (i in 0 until markerIndex) {
                nameWords.add(words[i])
            }
        } else if (words.isNotEmpty()) {
            // Fallback: take the first word if it doesn't look like a number or command/verb
            val firstWord = words[0]
            val invalidNameKeywords = listOf("udhar", "udhaar", "debit", "credit", "pay", "mila", "diya", "back", "gave", "got", "received", "total", "balance", "hisab")
            if (firstWord.toDoubleOrNull() == null && !invalidNameKeywords.contains(firstWord.lowercase())) {
                nameWords.add(firstWord)
                // If there is a second word and it also isn't a number/verb, take it (e.g. "Ramesh Kumar")
                if (words.size > 1) {
                    val secondWord = words[1]
                    if (secondWord.toDoubleOrNull() == null && 
                        !invalidNameKeywords.contains(secondWord.lowercase()) && 
                        !markers.contains(secondWord.lowercase())) {
                        nameWords.add(secondWord)
                    }
                }
            }
        }

        val parsedName = if (nameWords.isNotEmpty()) {
            nameWords.joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        } else {
            "Ramesh Kumar" // Ultimate fallback if nothing else works
        }

        return ParsedVoiceEntry(parsedName, intent, amount)
    }
}
