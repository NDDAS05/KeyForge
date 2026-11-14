package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SyntaxLayout
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun CodeSandboxEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    activeLayout: SyntaxLayout,
    onSelectLayout: (SyntaxLayout) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onResetSnippet: () -> Unit,
    onCopyToHistory: (String) -> Unit,
    onClear: () -> Unit,
    feedbackBadge: String?,
    modifier: Modifier = Modifier
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val normalColor = MaterialTheme.colorScheme.onSurface

    val syntaxTransformation = remember(
        activeLayout,
        primaryColor,
        secondaryColor,
        tertiaryColor,
        mutedColor,
        normalColor
    ) {
        CodeSyntaxVisualTransformation(
            layout = activeLayout,
            keywordColor = secondaryColor,
            bracketColor = primaryColor,
            stringColor = tertiaryColor,
            commentColor = mutedColor,
            normalColor = normalColor
        )
    }

    val (cursorLine, cursorCol) = remember(value.text, value.selection.start) {
        computeLineAndCol(value.text, value.selection.start)
    }
    val lineCount = remember(value.text) {
        value.text.count { it == '\n' } + 1
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Syntax Selector + Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SyntaxLayout.entries.forEach { layout ->
                        val selected = layout == activeLayout
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectLayout(layout) }
                                .testTag("editor_layout_chip_${layout.id}"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Text(
                                text = layout.shortLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onResetSnippet,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_snippet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Load starter snippet",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val selectedOrAll = if (!value.selection.collapsed) {
                                val min = minOf(value.selection.start, value.selection.end)
                                val max = maxOf(value.selection.start, value.selection.end)
                                value.text.substring(min, max)
                            } else {
                                value.text
                            }
                            if (selectedOrAll.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(selectedOrAll))
                                onCopyToHistory(selectedOrAll)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Clear editor",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Monospace Code Surface with Gutter + Suppressed System Soft Keyboard
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .verticalScroll(verticalScroll)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Line Numbers Gutter
                    Column(
                        modifier = Modifier
                            .padding(end = 10.dp, top = 2.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (line in 1..lineCount.coerceAtLeast(1)) {
                            Text(
                                text = line.toString(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = if (line == cursorLine) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }

                    // Suppress Android system IME inside the sandbox so our custom CodeKeyboard stays in control
                    @Suppress("DEPRECATION")
                    CompositionLocalProvider(LocalTextInputService provides null) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScroll)
                                .padding(top = 2.dp)
                                .testTag("sandbox_code_editor"),
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            visualTransformation = syntaxTransformation,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (value.text.isEmpty()) {
                                        Text(
                                            text = "// Tap symbols above or type code...",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 13.5.sp,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            // Bottom Telemetry Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ln $cursorLine, Col $cursorCol  •  ${value.text.length} ch",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("editor_cursor_telemetry")
                )

                AnimatedVisibility(
                    visible = feedbackBadge != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = feedbackBadge.orEmpty(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                .testTag("editor_feedback_badge")
                        )
                    }
                }
            }
        }
    }
}

private fun computeLineAndCol(text: String, offset: Int): Pair<Int, Int> {
    val safeOffset = offset.coerceIn(0, text.length)
    var line = 1
    var lastNewline = -1
    for (i in 0 until safeOffset) {
        if (text[i] == '\n') {
            line++
            lastNewline = i
        }
    }
    val col = safeOffset - lastNewline
    return line to col
}

private class CodeSyntaxVisualTransformation(
    private val layout: SyntaxLayout,
    private val keywordColor: Color,
    private val bracketColor: Color,
    private val stringColor: Color,
    private val commentColor: Color,
    private val normalColor: Color
) : VisualTransformation {

    private val keywords: Set<String> = when (layout) {
        SyntaxLayout.KOTLIN_C -> setOf(
            "fun", "val", "var", "for", "in", "if", "else", "return", "class",
            "object", "when", "true", "false", "null", "import", "package"
        )
        SyntaxLayout.PYTHON -> setOf(
            "def", "for", "in", "if", "elif", "else", "return", "class",
            "import", "from", "as", "True", "False", "None", "and", "or", "not"
        )
        SyntaxLayout.WEB_JSX -> setOf(
            "export", "const", "let", "var", "return", "function", "import",
            "from", "if", "else", "true", "false", "null", "undefined"
        )
        SyntaxLayout.SQL_SHELL -> setOf(
            "SELECT", "FROM", "WHERE", "LEFT", "JOIN", "ON", "AS", "AND", "OR",
            "ORDER", "BY", "DESC", "ASC", "GROUP", "LIMIT", "INSERT", "INTO", "VALUES"
        )
    }

    private val bracketChars = setOf('(', ')', '[', ']', '{', '}', '<', '>')

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val highlighted = buildAnnotatedString {
            append(raw)
            addStyle(SpanStyle(color = normalColor), 0, raw.length)

            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                // Single-line comments //, #, --
                val isLineComment =
                    (c == '/' && i + 1 < raw.length && raw[i + 1] == '/') ||
                        (c == '#' && layout == SyntaxLayout.PYTHON) ||
                        (c == '-' && i + 1 < raw.length && raw[i + 1] == '-' && layout == SyntaxLayout.SQL_SHELL)
                if (isLineComment) {
                    val end = raw.indexOf('\n', i).let { if (it == -1) raw.length else it }
                    addStyle(SpanStyle(color = commentColor), i, end)
                    i = end
                    continue
                }

                // String literals
                if (c == '"' || c == '\'' || c == '`') {
                    var j = i + 1
                    while (j < raw.length) {
                        if (raw[j] == '\\') {
                            j += 2
                            continue
                        }
                        if (raw[j] == c) {
                            j++
                            break
                        }
                        if (raw[j] == '\n') break
                        j++
                    }
                    addStyle(SpanStyle(color = stringColor), i, j.coerceAtMost(raw.length))
                    i = j
                    continue
                }

                // Brackets
                if (c in bracketChars) {
                    addStyle(SpanStyle(color = bracketColor, fontWeight = FontWeight.Bold), i, i + 1)
                    i++
                    continue
                }

                // Identifiers / Keywords
                if (c.isLetter() || c == '_') {
                    var j = i + 1
                    while (j < raw.length && (raw[j].isLetterOrDigit() || raw[j] == '_')) {
                        j++
                    }
                    val word = raw.substring(i, j)
                    if (word in keywords) {
                        addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), i, j)
                    }
                    i = j
                    continue
                }

                i++
            }
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
