package com.familylink.slf

/**
 * Regeln & Bewertung für Stadt-Land-Fluss gegen den Computer.
 *
 * Punkte pro Kategorie (klassische Variante):
 *  - 0  Punkte: kein oder ungültiges Wort
 *  - 10 Punkte: gültiges Wort, das der andere ebenfalls hat
 *  - 20 Punkte: gültiges Wort, das nur man selbst hat
 */
object GameLogic {

    /** Buchstaben, die als Rundenbuchstabe vorkommen können (schwere ausgelassen). */
    val playableLetters: List<Char> =
        ('A'..'Z').filter { it !in listOf('Q', 'X', 'Y') }

    fun randomLetter(): Char = playableLetters.random()

    /** Normalisiert ein Wort: Großbuchstaben, Umlaute auf Grundbuchstaben. */
    fun normalize(word: String): String {
        val sb = StringBuilder()
        for (c in word.trim().uppercase()) {
            when (c) {
                'Ä' -> sb.append('A')
                'Ö' -> sb.append('O')
                'Ü' -> sb.append('U')
                'ß' -> sb.append("SS")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Erster (normalisierter) Buchstabe eines Wortes, oder ' ' bei leerem Wort. */
    fun firstLetter(word: String): Char {
        val n = normalize(word)
        return if (n.isEmpty()) ' ' else n.first()
    }

    /** Prüft, ob eine Eingabe formal gültig ist (Anfangsbuchstabe passt, echtes Wort). */
    fun isValidAnswer(answer: String, letter: Char): Boolean {
        val trimmed = answer.trim()
        if (trimmed.length < 2) return false
        if (firstLetter(trimmed) != letter) return false
        // Nur Buchstaben, Bindestrich und Leerzeichen zulassen.
        return trimmed.all { it.isLetter() || it == '-' || it == ' ' }
    }

    /**
     * Bewertet eine einzelne Kategorie.
     * @param player  Eingabe der Spielerin / des Spielers
     * @param computer Wort des Computers (null = nichts gefunden)
     */
    fun scoreCategory(player: String, computer: String?, letter: Char): CategoryResult {
        val playerValid = isValidAnswer(player, letter)
        val same = playerValid && computer != null &&
                normalize(player) == normalize(computer)

        val playerPoints = when {
            !playerValid -> 0
            same -> 10
            else -> 20
        }
        val computerPoints = when {
            computer == null -> 0
            same -> 10
            else -> 20
        }
        return CategoryResult(
            playerAnswer = player.trim(),
            computerAnswer = computer,
            playerValid = playerValid,
            duplicate = same,
            playerPoints = playerPoints,
            computerPoints = computerPoints
        )
    }
}

data class CategoryResult(
    val playerAnswer: String,
    val computerAnswer: String?,
    val playerValid: Boolean,
    val duplicate: Boolean,
    val playerPoints: Int,
    val computerPoints: Int
)
