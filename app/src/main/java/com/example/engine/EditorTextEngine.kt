package com.example.engine

import com.example.data.KeyboardPreferences
import com.example.model.SyntaxLayout

data class TextEditResult(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    val feedbackBadge: String? = null,
    val didAutoPair: Boolean = false
)

object EditorTextEngine {

    private val basePairs = mapOf(
        "(" to ")",
        "[" to "]",
        "{" to "}",
        "\"" to "\"",
        "'" to "'",
        "`" to "`"
    )

    private val closingDelimiters = setOf(')', ']', '}', '>', '"', '\'', '`')

    private val matchingCharPairs = setOf(
        '(' to ')',
        '[' to ']',
        '{' to '}',
        '<' to '>',
        '"' to '"',
        '\'' to '\'',
        '`' to '`'
    )

    fun closingFor(openToken: String, prefs: KeyboardPreferences): String? {
        if (!prefs.autoCloseBrackets) return null
        if (openToken == "<") {
            return if (prefs.autoCloseAngleBrackets) ">" else null
        }
        return basePairs[openToken]
    }

    fun insertToken(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        token: String,
        prefs: KeyboardPreferences
    ): TextEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)

        // Special composite token: f"" -> insert f"" with cursor between quotes
        if (token == "f\"\"") {
            val newText = text.substring(0, start) + "f\"\"" + text.substring(end)
            val cursor = start + 2
            return TextEditResult(newText, cursor, cursor, "Auto-paired f\" | \"", didAutoPair = true)
        }

        // Wrap non-empty selection in matching pair if user taps an opening bracket/quote
        val closing = closingFor(token, prefs)
        if (start != end && closing != null) {
            val selected = text.substring(start, end)
            val wrapped = token + selected + closing
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            val newStart = start + token.length
            val newEnd = newStart + selected.length
            return TextEditResult(
                text = newText,
                selectionStart = newStart,
                selectionEnd = newEnd,
                feedbackBadge = "Wrapped $token…$closing",
                didAutoPair = true
            )
        }

        // Overtype / skip-over closing delimiter when cursor sits right before identical closing char
        if (start == end &&
            prefs.autoCloseBrackets &&
            token.length == 1 &&
            token[0] in closingDelimiters &&
            start < text.length &&
            text[start] == token[0]
        ) {
            // For symmetric quotes, only skip over if it's closing an active quote or sitting inside empty pair
            val isQuote = token[0] == '"' || token[0] == '\'' || token[0] == '`'
            val shouldSkip = if (!isQuote) {
                true
            } else {
                val prevChar = text.getOrNull(start - 1)
                prevChar == token[0] || countOccurrencesBefore(text, token[0], start) % 2 == 1
            }
            if (shouldSkip) {
                val nextCursor = start + 1
                return TextEditResult(
                    text = text,
                    selectionStart = nextCursor,
                    selectionEnd = nextCursor,
                    feedbackBadge = "Stepped over ${token}"
                )
            }
        }

        // Auto-closing bracket / quote insertion with cursor placed between pair
        if (closing != null) {
            val pairText = token + closing
            val newText = text.substring(0, start) + pairText + text.substring(end)
            val cursor = start + token.length
            return TextEditResult(
                text = newText,
                selectionStart = cursor,
                selectionEnd = cursor,
                feedbackBadge = "Auto-paired $token | $closing",
                didAutoPair = true
            )
        }

        val newText = text.substring(0, start) + token + text.substring(end)
        val cursor = start + token.length
        return TextEditResult(newText, cursor, cursor)
    }

    fun backspace(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        prefs: KeyboardPreferences
    ): TextEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)

        if (start != end) {
            val newText = text.substring(0, start) + text.substring(end)
            return TextEditResult(newText, start, start)
        }

        if (start == 0) {
            return TextEditResult(text, 0, 0)
        }

        // Smart pair delete when cursor is directly between matching empty pair e.g. (|)
        if (prefs.smartPairBackspace && start < text.length) {
            val leftChar = text[start - 1]
            val rightChar = text[start]
            if ((leftChar to rightChar) in matchingCharPairs) {
                val newText = text.substring(0, start - 1) + text.substring(start + 1)
                val cursor = start - 1
                return TextEditResult(
                    text = newText,
                    selectionStart = cursor,
                    selectionEnd = cursor,
                    feedbackBadge = "Removed pair $leftChar$rightChar"
                )
            }
        }

        val newText = text.substring(0, start - 1) + text.substring(start)
        val cursor = start - 1
        return TextEditResult(newText, cursor, cursor)
    }

    fun enter(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        prefs: KeyboardPreferences
    ): TextEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)

        if (!prefs.smartIndentOnEnter) {
            val newText = text.substring(0, start) + "\n" + text.substring(end)
            return TextEditResult(newText, start + 1, start + 1)
        }

        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0))
            .let { if (it == -1 || start == 0) 0 else it + 1 }
        val currentLinePrefix = text.substring(lineStart, start)
        val baseIndent = currentLinePrefix.takeWhile { it == ' ' || it == '\t' }
        val indentUnit = " ".repeat(prefs.syntaxLayout.tabSpaces)

        val charBefore = currentLinePrefix.trimEnd().lastOrNull()
        val charAfter = text.getOrNull(end)

        // Expand {|}, (|), [|] onto indented inner line
        val isEmptyBlock = (charBefore == '{' && charAfter == '}') ||
            (charBefore == '(' && charAfter == ')') ||
            (charBefore == '[' && charAfter == ']')

        if (start == end && isEmptyBlock) {
            val innerIndent = baseIndent + indentUnit
            val insertion = "\n$innerIndent\n$baseIndent"
            val newText = text.substring(0, start) + insertion + text.substring(end)
            val cursor = start + 1 + innerIndent.length
            return TextEditResult(
                text = newText,
                selectionStart = cursor,
                selectionEnd = cursor,
                feedbackBadge = "Expanded block"
            )
        }

        val shouldIncreaseIndent = when (charBefore) {
            '{', '(', '[' -> true
            ':' -> prefs.syntaxLayout == SyntaxLayout.PYTHON
            else -> false
        }
        val nextIndent = if (shouldIncreaseIndent) baseIndent + indentUnit else baseIndent
        val insertion = "\n$nextIndent"
        val newText = text.substring(0, start) + insertion + text.substring(end)
        val cursor = start + insertion.length
        return TextEditResult(newText, cursor, cursor)
    }

    fun insertTab(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        prefs: KeyboardPreferences
    ): TextEditResult {
        val spaces = " ".repeat(prefs.syntaxLayout.tabSpaces)
        return insertToken(text, selectionStart, selectionEnd, spaces, prefs)
    }

    fun moveCursorLeft(text: String, selectionStart: Int, selectionEnd: Int): TextEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val target = if (start != end) start else (start - 1).coerceAtLeast(0)
        return TextEditResult(text, target, target)
    }

    fun moveCursorRight(text: String, selectionStart: Int, selectionEnd: Int): TextEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val target = if (start != end) end else (end + 1).coerceAtMost(text.length)
        return TextEditResult(text, target, target)
    }

    private fun countOccurrencesBefore(text: String, target: Char, limit: Int): Int {
        var count = 0
        for (i in 0 until limit.coerceAtMost(text.length)) {
            if (text[i] == target && (i == 0 || text[i - 1] != '\\')) {
                count++
            }
        }
        return count
    }
}
