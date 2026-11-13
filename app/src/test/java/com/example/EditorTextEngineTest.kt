package com.example

import com.example.data.KeyboardPreferences
import com.example.engine.EditorTextEngine
import com.example.model.SyntaxLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTextEngineTest {

    private val defaultPrefs = KeyboardPreferences()

    @Test
    fun `auto closing brackets insert pair and place cursor between them`() {
        val parens = EditorTextEngine.insertToken("", 0, 0, "(", defaultPrefs)
        assertEquals("()", parens.text)
        assertEquals(1, parens.selectionStart)
        assertEquals(1, parens.selectionEnd)
        assertTrue(parens.didAutoPair)

        val brackets = EditorTextEngine.insertToken("arr", 3, 3, "[", defaultPrefs)
        assertEquals("arr[]", brackets.text)
        assertEquals(4, brackets.selectionStart)

        val braces = EditorTextEngine.insertToken("fn ", 3, 3, "{", defaultPrefs)
        assertEquals("fn {}", braces.text)
        assertEquals(4, braces.selectionStart)

        val angles = EditorTextEngine.insertToken("List", 4, 4, "<", defaultPrefs)
        assertEquals("List<>", angles.text)
        assertEquals(5, angles.selectionStart)
    }

    @Test
    fun `typing inside auto-closed pair inserts inside and overtypes closer`() {
        val opened = EditorTextEngine.insertToken("val x = ", 8, 8, "(", defaultPrefs)
        val inner = EditorTextEngine.insertToken(
            opened.text,
            opened.selectionStart,
            opened.selectionEnd,
            "a",
            defaultPrefs
        )
        assertEquals("val x = (a)", inner.text)
        assertEquals(10, inner.selectionStart)

        val stepped = EditorTextEngine.insertToken(
            inner.text,
            inner.selectionStart,
            inner.selectionEnd,
            ")",
            defaultPrefs
        )
        assertEquals("val x = (a)", stepped.text)
        assertEquals(11, stepped.selectionStart)
    }

    @Test
    fun `selection wrapping surrounds selected text with matching brackets`() {
        val wrapped = EditorTextEngine.insertToken("hello", 0, 5, "(", defaultPrefs)
        assertEquals("(hello)", wrapped.text)
        assertEquals(1, wrapped.selectionStart)
        assertEquals(6, wrapped.selectionEnd)
    }

    @Test
    fun `smart pair backspace deletes both delimiters when empty`() {
        val deleted = EditorTextEngine.backspace("call()", 5, 5, defaultPrefs)
        assertEquals("call", deleted.text)
        assertEquals(4, deleted.selectionStart)
    }

    @Test
    fun `cursor left and right buttons move cursor safely`() {
        val left = EditorTextEngine.moveCursorLeft("abc", 2, 2)
        assertEquals(1, left.selectionStart)

        val leftClamp = EditorTextEngine.moveCursorLeft("abc", 0, 0)
        assertEquals(0, leftClamp.selectionStart)

        val right = EditorTextEngine.moveCursorRight("abc", 1, 1)
        assertEquals(2, right.selectionStart)

        val rightClamp = EditorTextEngine.moveCursorRight("abc", 3, 3)
        assertEquals(3, rightClamp.selectionStart)
    }

    @Test
    fun `smart enter inside empty braces creates indented inner line`() {
        val result = EditorTextEngine.enter("fun main() {}", 12, 12, defaultPrefs)
        assertEquals("fun main() {\n    \n}", result.text)
        assertEquals(17, result.selectionStart)
    }

    @Test
    fun `python layout indents after colon on enter`() {
        val pyPrefs = defaultPrefs.copy(syntaxLayout = SyntaxLayout.PYTHON)
        val result = EditorTextEngine.enter("if ok:", 6, 6, pyPrefs)
        assertEquals("if ok:\n    ", result.text)
        assertEquals(11, result.selectionStart)
    }
}
