package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.KeyboardTab
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.KeyboardCapslock
import androidx.compose.material.icons.rounded.North
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SubdirectoryArrowLeft
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ClipboardItem
import com.example.data.EnterKeyBehavior
import com.example.data.KeyHighlightMode
import com.example.data.KeyboardPreferences
import com.example.haptics.HapticEventType
import com.example.haptics.KeyboardHaptics
import com.example.model.SyntaxLayout
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ShiftState {
    OFF,
    SINGLE,
    CAPS_LOCK
}

enum class KeyLayer {
    QWERTY,
    SYMBOLS_NUMBERS
}

interface KeyboardActions {
    fun onCommitToken(token: String)
    fun onBackspace()
    fun onEnter()
    fun onNewline()
    fun onSpace()
    fun onTab()
    fun onCursorLeft()
    fun onCursorRight()
    fun onToggleSyntaxLayout()
    fun onToggleAutoClose()
    fun onToggleHaptics()
    fun onOpenSettings()
    fun onPasteClipboardItem(text: String)
    fun onTogglePinClipboardItem(id: String)
    fun onDeleteClipboardItem(id: String)
    fun onClearUnpinnedClipboard()
}

private data class KeyboardMetrics(
    val isLandscape: Boolean,
    val controlBarHeight: Dp,
    val symbolRowHeight: Dp,
    val syntaxStripHeight: Dp,
    val mainKeyHeight: Dp,
    val bottomRowHeight: Dp,
    val verticalRowSpacing: Dp,
    val horizontalKeyGap: Dp,
    val outerHorizontalPadding: Dp,
    val outerVerticalPadding: Dp,
    val qwertyRow2HorizontalPadding: Dp,
    val keyCornerRadius: Dp,
    val controlCornerRadius: Dp,
    val primaryFontSp: Int,
    val symbolFontSp: Int,
    val syntaxFontSp: Int,
    val syntaxSmallFontSp: Int,
    val spaceFontSp: Int,
    val hintFontSp: Int,
    val iconSize: Dp,
    val repeatIconSize: Dp,
    val controlIconSize: Dp,
    val hintIconSize: Dp,
    val clipboardPanelHeight: Dp
)

private fun keyboardMetricsFor(isLandscape: Boolean): KeyboardMetrics =
    if (isLandscape) {
        KeyboardMetrics(
            isLandscape = true,
            controlBarHeight = 30.dp,
            symbolRowHeight = 28.dp,
            syntaxStripHeight = 24.dp,
            mainKeyHeight = 29.dp,
            bottomRowHeight = 30.dp,
            verticalRowSpacing = 2.dp,
            horizontalKeyGap = 3.5.dp,
            outerHorizontalPadding = 8.dp,
            outerVerticalPadding = 2.dp,
            qwertyRow2HorizontalPadding = 22.dp,
            keyCornerRadius = 6.dp,
            controlCornerRadius = 7.dp,
            primaryFontSp = 14,
            symbolFontSp = 14,
            syntaxFontSp = 12,
            syntaxSmallFontSp = 10,
            spaceFontSp = 10,
            hintFontSp = 9,
            iconSize = 17.dp,
            repeatIconSize = 18.dp,
            controlIconSize = 15.dp,
            hintIconSize = 9.dp,
            clipboardPanelHeight = 172.dp
        )
    } else {
        // Portrait dimensions kept completely untouched
        KeyboardMetrics(
            isLandscape = false,
            controlBarHeight = 42.dp,
            symbolRowHeight = 44.dp,
            syntaxStripHeight = 36.dp,
            mainKeyHeight = 46.dp,
            bottomRowHeight = 46.dp,
            verticalRowSpacing = 5.dp,
            horizontalKeyGap = 3.dp,
            outerHorizontalPadding = 4.dp,
            outerVerticalPadding = 6.dp,
            qwertyRow2HorizontalPadding = 14.dp,
            keyCornerRadius = 8.dp,
            controlCornerRadius = 10.dp,
            primaryFontSp = 16,
            symbolFontSp = 16,
            syntaxFontSp = 13,
            syntaxSmallFontSp = 11,
            spaceFontSp = 11,
            hintFontSp = 9,
            iconSize = 20.dp,
            repeatIconSize = 21.dp,
            controlIconSize = 17.dp,
            hintIconSize = 10.dp,
            clipboardPanelHeight = 250.dp
        )
    }

private val QWERTY_ROW_1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val QWERTY_ROW_2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val QWERTY_ROW_3 = listOf("z", "x", "c", "v", "b", "n", "m")

private val NUM_ROW_1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val NUM_ROW_2 = listOf("@", "#", "$", "%", "^", "&", "*", "-", "_", "+")
private val NUM_ROW_3 = listOf("~", "|", "\\", "?", ":", "\"", "'", "!", "`")

@Composable
fun CodeKeyboard(
    preferences: KeyboardPreferences,
    clipboardItems: List<ClipboardItem>,
    haptics: KeyboardHaptics,
    actions: KeyboardActions,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val metrics = remember(isLandscape) { keyboardMetricsFor(isLandscape) }

    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var keyLayer by remember { mutableStateOf(KeyLayer.QWERTY) }
    var showClipboardPanel by remember { mutableStateOf(false) }
    var lastShiftTapMs by remember { mutableLongStateOf(0L) }
    val rgbController = rememberRgbHighlightController()

    val activeLayout = preferences.syntaxLayout

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = metrics.outerHorizontalPadding,
                    vertical = metrics.outerVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(metrics.verticalRowSpacing)
        ) {
            // Top Utility Bar: Compact Syntax Toggle, Clipboard History, Auto-Pair, Haptics, Tab, Left/Right Cursor
            KeyboardControlStrip(
                preferences = preferences,
                metrics = metrics,
                clipboardOpen = showClipboardPanel,
                clipboardCount = clipboardItems.size,
                rgbController = rgbController,
                onToggleSyntax = {
                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                    actions.onToggleSyntaxLayout()
                },
                onToggleClipboard = {
                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                    showClipboardPanel = !showClipboardPanel
                },
                onToggleAutoClose = {
                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                    actions.onToggleAutoClose()
                },
                onToggleHaptics = {
                    haptics.trigger(view, !preferences.hapticEnabled || preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                    actions.onToggleHaptics()
                },
                onTab = {
                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                    actions.onTab()
                },
                onCursorLeft = {
                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.CURSOR_STEP)
                    actions.onCursorLeft()
                },
                onCursorRight = {
                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.CURSOR_STEP)
                    actions.onCursorRight()
                }
            )

            if (showClipboardPanel) {
                ClipboardHistoryPanel(
                    items = clipboardItems,
                    panelHeight = metrics.clipboardPanelHeight,
                    onPasteItem = { item ->
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.SYMBOL_TAP)
                        actions.onPasteClipboardItem(item.text)
                        showClipboardPanel = false
                    },
                    onTogglePin = { item ->
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                        actions.onTogglePinClipboardItem(item.id)
                    },
                    onDeleteItem = { item ->
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                        actions.onDeleteClipboardItem(item.id)
                    },
                    onClearUnpinned = {
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                        actions.onClearUnpinnedClipboard()
                    },
                    onClose = {
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                        showClipboardPanel = false
                    }
                )
            } else {
                // Dedicated Top Priority Symbol Row: ( ) [ ] { } ; < > = /
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dedicated_symbol_row"),
                    horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                ) {
                    SyntaxLayout.PRIORITY_TOP_SYMBOLS.forEach { symbol ->
                        val isBracketOpener = symbol in setOf("(", "[", "{", "<")
                        val isOperator = symbol in setOf(";", "=", "/")
                        val labelColor = when {
                            isBracketOpener -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        KeyCapButton(
                            label = symbol,
                            preferences = preferences,
                            rgbController = rgbController,
                            cornerRadius = metrics.keyCornerRadius,
                            hintFontSp = metrics.hintFontSp,
                            hintIconSize = metrics.hintIconSize,
                            modifier = Modifier
                                .weight(1f)
                                .height(metrics.symbolRowHeight)
                                .testTag("symbol_key_${symbolTagName(symbol)}"),
                            labelColor = labelColor,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            fontSizeSp = metrics.symbolFontSp,
                            onTap = {
                                val eventType = if (isBracketOpener && preferences.autoCloseBrackets) {
                                    HapticEventType.AUTO_PAIR
                                } else {
                                    HapticEventType.SYMBOL_TAP
                                }
                                haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, eventType)
                                actions.onCommitToken(symbol)
                            }
                        )
                    }
                }

                // Syntax-Specific Character Layout Row (morphs when toggling KT/C, PY, WEB, SQL)
                if (preferences.showSyntaxStrip) {
                    AnimatedContent(
                        targetState = activeLayout,
                        transitionSpec = {
                            fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessMedium))
                        },
                        label = "syntax_strip_transition"
                    ) { layout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("syntax_specific_row"),
                            horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                        ) {
                            layout.syntaxStripKeys.forEach { spec ->
                                val isQuote = spec.token in setOf("\"", "'", "`", "f\"\"")
                                KeyCapButton(
                                    label = spec.token,
                                    preferences = preferences,
                                    rgbController = rgbController,
                                    cornerRadius = metrics.keyCornerRadius,
                                    hintFontSp = metrics.hintFontSp,
                                    hintIconSize = metrics.hintIconSize,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(metrics.syntaxStripHeight)
                                        .testTag("syntax_key_${spec.token}"),
                                    labelColor = if (isQuote) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    fontSizeSp = if (spec.token.length >= 3) {
                                        metrics.syntaxSmallFontSp
                                    } else {
                                        metrics.syntaxFontSp
                                    },
                                    onTap = {
                                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.SYMBOL_TAP)
                                        actions.onCommitToken(spec.token)
                                    }
                                )
                            }
                        }
                    }
                }

                if (keyLayer == KeyLayer.QWERTY) {
                    // QWERTY Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                    ) {
                        QWERTY_ROW_1.forEach { ch ->
                            val display = if (shiftState != ShiftState.OFF) ch.uppercase() else ch
                            val hint = if (preferences.showKeyHints) activeLayout.secondaryHints[ch] else null
                            KeyCapButton(
                                label = display,
                                secondaryHint = hint,
                                preferences = preferences,
                                rgbController = rgbController,
                                cornerRadius = metrics.keyCornerRadius,
                                fontSizeSp = metrics.primaryFontSp,
                                hintFontSp = metrics.hintFontSp,
                                hintIconSize = metrics.hintIconSize,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(metrics.mainKeyHeight)
                                    .testTag("key_$ch"),
                                onTap = {
                                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                                    actions.onCommitToken(display)
                                    if (shiftState == ShiftState.SINGLE) shiftState = ShiftState.OFF
                                },
                                onLongPress = hint?.let { h ->
                                    {
                                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.SYMBOL_TAP)
                                        actions.onCommitToken(h)
                                    }
                                }
                            )
                        }
                    }

                    // QWERTY Row 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.qwertyRow2HorizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                    ) {
                        QWERTY_ROW_2.forEach { ch ->
                            val display = if (shiftState != ShiftState.OFF) ch.uppercase() else ch
                            val hint = if (preferences.showKeyHints) activeLayout.secondaryHints[ch] else null
                            KeyCapButton(
                                label = display,
                                secondaryHint = hint,
                                preferences = preferences,
                                rgbController = rgbController,
                                cornerRadius = metrics.keyCornerRadius,
                                fontSizeSp = metrics.primaryFontSp,
                                hintFontSp = metrics.hintFontSp,
                                hintIconSize = metrics.hintIconSize,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(metrics.mainKeyHeight)
                                    .testTag("key_$ch"),
                                onTap = {
                                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                                    actions.onCommitToken(display)
                                    if (shiftState == ShiftState.SINGLE) shiftState = ShiftState.OFF
                                },
                                onLongPress = hint?.let { h ->
                                    {
                                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.SYMBOL_TAP)
                                        actions.onCommitToken(h)
                                    }
                                }
                            )
                        }
                    }

                    // QWERTY Row 3: Shift + Z..M + Backspace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                    ) {
                        val shiftActive = shiftState != ShiftState.OFF
                        val shiftBg by animateColorAsState(
                            targetValue = when (shiftState) {
                                ShiftState.CAPS_LOCK -> MaterialTheme.colorScheme.primary
                                ShiftState.SINGLE -> MaterialTheme.colorScheme.primaryContainer
                                ShiftState.OFF -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            label = "shift_bg"
                        )
                        val shiftFg = when (shiftState) {
                            ShiftState.CAPS_LOCK -> MaterialTheme.colorScheme.onPrimary
                            ShiftState.SINGLE -> MaterialTheme.colorScheme.onPrimaryContainer
                            ShiftState.OFF -> MaterialTheme.colorScheme.onSurface
                        }

                        ActionIconKeyCap(
                            icon = if (shiftState == ShiftState.CAPS_LOCK) {
                                Icons.Rounded.KeyboardCapslock
                            } else {
                                Icons.Rounded.North
                            },
                            contentDescription = stringResource(R.string.cd_shift),
                            containerColor = shiftBg,
                            iconTint = shiftFg,
                            preferences = preferences,
                            rgbController = rgbController,
                            cornerRadius = metrics.keyCornerRadius,
                            iconSize = metrics.iconSize,
                            modifier = Modifier
                                .weight(1.45f)
                                .height(metrics.mainKeyHeight)
                                .testTag("key_shift"),
                            onTap = {
                                haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                                val now = System.currentTimeMillis()
                                shiftState = when {
                                    shiftState == ShiftState.CAPS_LOCK -> ShiftState.OFF
                                    shiftState == ShiftState.SINGLE && (now - lastShiftTapMs) < 350L -> ShiftState.CAPS_LOCK
                                    shiftActive -> ShiftState.OFF
                                    else -> ShiftState.SINGLE
                                }
                                lastShiftTapMs = now
                            },
                            onLongPress = {
                                haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                                shiftState = ShiftState.CAPS_LOCK
                            }
                        )

                        QWERTY_ROW_3.forEach { ch ->
                            val display = if (shiftState != ShiftState.OFF) ch.uppercase() else ch
                            val hint = if (preferences.showKeyHints) activeLayout.secondaryHints[ch] else null
                            KeyCapButton(
                                label = display,
                                secondaryHint = hint,
                                preferences = preferences,
                                rgbController = rgbController,
                                cornerRadius = metrics.keyCornerRadius,
                                fontSizeSp = metrics.primaryFontSp,
                                hintFontSp = metrics.hintFontSp,
                                hintIconSize = metrics.hintIconSize,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(metrics.mainKeyHeight)
                                    .testTag("key_$ch"),
                                onTap = {
                                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                                    actions.onCommitToken(display)
                                    if (shiftState == ShiftState.SINGLE) shiftState = ShiftState.OFF
                                },
                                onLongPress = hint?.let { h ->
                                    {
                                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.SYMBOL_TAP)
                                        actions.onCommitToken(h)
                                    }
                                }
                            )
                        }

                        RepeatableIconKeyCap(
                            icon = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = stringResource(R.string.cd_backspace),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            iconTint = MaterialTheme.colorScheme.onSurface,
                            preferences = preferences,
                            rgbController = rgbController,
                            cornerRadius = metrics.keyCornerRadius,
                            iconSize = metrics.repeatIconSize,
                            modifier = Modifier
                                .weight(1.45f)
                                .height(metrics.mainKeyHeight)
                                .testTag("key_backspace"),
                            onTrigger = {
                                haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                                actions.onBackspace()
                            }
                        )
                    }
                } else {
                    // 123 / Extended Symbols Layer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                    ) {
                        NUM_ROW_1.forEach { digit ->
                            KeyCapButton(
                                label = digit,
                                preferences = preferences,
                                rgbController = rgbController,
                                cornerRadius = metrics.keyCornerRadius,
                                fontSizeSp = metrics.primaryFontSp,
                                hintFontSp = metrics.hintFontSp,
                                hintIconSize = metrics.hintIconSize,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(metrics.mainKeyHeight)
                                    .testTag("key_num_$digit"),
                                onTap = {
                                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                                    actions.onCommitToken(digit)
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                    ) {
                        NUM_ROW_2.forEach { sym ->
                            KeyCapButton(
                                label = sym,
                                preferences = preferences,
                                rgbController = rgbController,
                                cornerRadius = metrics.keyCornerRadius,
                                fontSizeSp = metrics.primaryFontSp,
                                hintFontSp = metrics.hintFontSp,
                                hintIconSize = metrics.hintIconSize,
                                labelColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(metrics.mainKeyHeight)
                                    .testTag("key_sym_$sym"),
                                onTap = {
                                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.SYMBOL_TAP)
                                    actions.onCommitToken(sym)
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                    ) {
                        NUM_ROW_3.forEach { sym ->
                            KeyCapButton(
                                label = sym,
                                preferences = preferences,
                                rgbController = rgbController,
                                cornerRadius = metrics.keyCornerRadius,
                                fontSizeSp = metrics.primaryFontSp,
                                hintFontSp = metrics.hintFontSp,
                                hintIconSize = metrics.hintIconSize,
                                labelColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(metrics.mainKeyHeight)
                                    .testTag("key_sym_$sym"),
                                onTap = {
                                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.SYMBOL_TAP)
                                    actions.onCommitToken(sym)
                                }
                            )
                        }

                        RepeatableIconKeyCap(
                            icon = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = stringResource(R.string.cd_backspace),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            iconTint = MaterialTheme.colorScheme.onSurface,
                            preferences = preferences,
                            rgbController = rgbController,
                            cornerRadius = metrics.keyCornerRadius,
                            iconSize = metrics.repeatIconSize,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(metrics.mainKeyHeight)
                                .testTag("key_backspace"),
                            onTrigger = {
                                haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                                actions.onBackspace()
                            }
                        )
                    }
                }

                // Bottom Action Row: 123/ABC, Comma (long-press Settings), Spacebar, Period, Enter/Newline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.horizontalKeyGap)
                ) {
                    KeyCapButton(
                        label = if (keyLayer == KeyLayer.QWERTY) "123" else "ABC",
                        preferences = preferences,
                        rgbController = rgbController,
                        cornerRadius = metrics.keyCornerRadius,
                        hintFontSp = metrics.hintFontSp,
                        hintIconSize = metrics.hintIconSize,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(metrics.bottomRowHeight)
                            .testTag("layer_toggle_button"),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        fontSizeSp = if (metrics.isLandscape) 12 else 13,
                        onTap = {
                            haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                            keyLayer = if (keyLayer == KeyLayer.QWERTY) KeyLayer.SYMBOLS_NUMBERS else KeyLayer.QWERTY
                        }
                    )

                    KeyCapButton(
                        label = ",",
                        secondaryHintIcon = Icons.Rounded.Settings,
                        preferences = preferences,
                        rgbController = rgbController,
                        cornerRadius = metrics.keyCornerRadius,
                        fontSizeSp = metrics.primaryFontSp,
                        hintFontSp = metrics.hintFontSp,
                        hintIconSize = metrics.hintIconSize,
                        modifier = Modifier
                            .weight(1f)
                            .height(metrics.bottomRowHeight)
                            .testTag("key_comma"),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        longPressDelayMs = 400L,
                        onTap = {
                            haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                            actions.onCommitToken(",")
                        },
                        onLongPress = {
                            haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                            actions.onOpenSettings()
                        }
                    )

                    KeyCapButton(
                        label = activeLayout.spaceLabel,
                        preferences = preferences,
                        rgbController = rgbController,
                        cornerRadius = metrics.keyCornerRadius,
                        hintFontSp = metrics.hintFontSp,
                        hintIconSize = metrics.hintIconSize,
                        modifier = Modifier
                            .weight(4.2f)
                            .height(metrics.bottomRowHeight)
                            .testTag("key_space"),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSizeSp = metrics.spaceFontSp,
                        onTap = {
                            haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                            actions.onSpace()
                        }
                    )

                    KeyCapButton(
                        label = ".",
                        preferences = preferences,
                        rgbController = rgbController,
                        cornerRadius = metrics.keyCornerRadius,
                        fontSizeSp = metrics.primaryFontSp,
                        hintFontSp = metrics.hintFontSp,
                        hintIconSize = metrics.hintIconSize,
                        modifier = Modifier
                            .weight(1f)
                            .height(metrics.bottomRowHeight)
                            .testTag("key_period"),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onTap = {
                            haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                            actions.onCommitToken(".")
                        }
                    )

                    val tapIsEnter = preferences.enterKeyBehavior == EnterKeyBehavior.PRESS_ENTER_HOLD_NEWLINE
                    ActionIconKeyCap(
                        icon = if (tapIsEnter) {
                            Icons.AutoMirrored.Rounded.KeyboardReturn
                        } else {
                            Icons.Rounded.SubdirectoryArrowLeft
                        },
                        subHint = if (tapIsEnter) "\\n" else "ENT",
                        contentDescription = stringResource(R.string.cd_enter),
                        containerColor = MaterialTheme.colorScheme.primary,
                        iconTint = MaterialTheme.colorScheme.onPrimary,
                        preferences = preferences,
                        rgbController = rgbController,
                        cornerRadius = metrics.keyCornerRadius,
                        iconSize = metrics.iconSize,
                        modifier = Modifier
                            .weight(1.6f)
                            .height(metrics.bottomRowHeight)
                            .testTag("key_enter"),
                        onTap = {
                            haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                            if (tapIsEnter) actions.onEnter() else actions.onNewline()
                        },
                        onLongPress = {
                            haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.AUTO_PAIR)
                            if (tapIsEnter) actions.onNewline() else actions.onEnter()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardControlStrip(
    preferences: KeyboardPreferences,
    metrics: KeyboardMetrics,
    clipboardOpen: Boolean,
    clipboardCount: Int,
    rgbController: RgbHighlightController,
    onToggleSyntax: () -> Unit,
    onToggleClipboard: () -> Unit,
    onToggleAutoClose: () -> Unit,
    onToggleHaptics: () -> Unit,
    onTab: () -> Unit,
    onCursorLeft: () -> Unit,
    onCursorRight: () -> Unit
) {
    val syntaxBadgeDesc = stringResource(R.string.cd_syntax_toggle)
    val clipboardDesc = stringResource(R.string.cd_clipboard_history)
    val autoCloseDesc = stringResource(R.string.cd_autoclose_toggle)
    val hapticDesc = stringResource(R.string.cd_haptic_toggle)

    val pairBg by animateColorAsState(
        targetValue = if (preferences.autoCloseBrackets) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "autoclose_bg"
    )
    val pairFg = if (preferences.autoCloseBrackets) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val hapticColor = if (preferences.hapticEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    val controlShape = RoundedCornerShape(metrics.controlCornerRadius)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (metrics.isLandscape) 0.dp else 2.dp),
        horizontalArrangement = Arrangement.spacedBy(if (metrics.isLandscape) 6.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Syntax Layout Toggle Chip — compact fixed/capped width in landscape so it never stretches
        Surface(
            modifier = (if (metrics.isLandscape) {
                Modifier.widthIn(min = 74.dp, max = 96.dp)
            } else {
                Modifier.weight(1.1f)
            })
                .height(metrics.controlBarHeight)
                .clip(controlShape)
                .clickable(role = Role.Button, onClick = onToggleSyntax)
                .semantics { contentDescription = syntaxBadgeDesc }
                .testTag("syntax_layout_toggle"),
            shape = controlShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (metrics.isLandscape) 10.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(if (metrics.isLandscape) 14.dp else 14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = preferences.syntaxLayout.shortLabel.replace(" ", ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }

        // Dedicated Clipboard History Button
        Surface(
            modifier = Modifier
                .width(if (metrics.isLandscape) 44.dp else 42.dp)
                .height(metrics.controlBarHeight)
                .clip(controlShape)
                .clickable(role = Role.Button, onClick = onToggleClipboard)
                .semantics { contentDescription = clipboardDesc }
                .testTag("clipboard_history_button"),
            shape = controlShape,
            color = if (clipboardOpen) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            border = BorderStroke(
                1.dp,
                if (clipboardOpen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.ContentPaste,
                    contentDescription = null,
                    tint = if (clipboardOpen) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(metrics.controlIconSize)
                )
                if (clipboardCount > 0 && !clipboardOpen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = if (metrics.isLandscape) 4.dp else 6.dp,
                                end = if (metrics.isLandscape) 5.dp else 6.dp
                            )
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Auto-Close Pair Toggle Chip — compact balanced width in landscape
        Surface(
            modifier = (if (metrics.isLandscape) {
                Modifier.widthIn(min = 66.dp, max = 86.dp)
            } else {
                Modifier.weight(0.95f)
            })
                .height(metrics.controlBarHeight)
                .clip(controlShape)
                .clickable(role = Role.Button, onClick = onToggleAutoClose)
                .semantics { contentDescription = autoCloseDesc }
                .testTag("auto_close_toggle"),
            shape = controlShape,
            color = pairBg,
            border = BorderStroke(
                1.dp,
                if (preferences.autoCloseBrackets) pairFg.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (metrics.isLandscape) 10.dp else 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(pairFg)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "{|}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (metrics.isLandscape) 11.sp else 11.sp,
                    color = pairFg,
                    maxLines = 1
                )
            }
        }

        // Haptic Toggle Chip
        Surface(
            modifier = Modifier
                .width(if (metrics.isLandscape) 44.dp else 40.dp)
                .height(metrics.controlBarHeight)
                .clip(controlShape)
                .clickable(role = Role.Button, onClick = onToggleHaptics)
                .semantics { contentDescription = hapticDesc }
                .testTag("haptic_toggle"),
            shape = controlShape,
            color = if (preferences.hapticEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            border = BorderStroke(
                1.dp,
                if (preferences.hapticEnabled) hapticColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = hapticColor,
                    modifier = Modifier.size(metrics.controlIconSize)
                )
            }
        }

        // In landscape, bridge left mode pills and right cursor/tab controls cleanly without stretching pills
        if (metrics.isLandscape) {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Tab Indent Key
        ActionIconKeyCap(
            icon = Icons.AutoMirrored.Rounded.KeyboardTab,
            contentDescription = stringResource(R.string.cd_tab),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconTint = MaterialTheme.colorScheme.onSurface,
            preferences = preferences,
            rgbController = rgbController,
            cornerRadius = metrics.controlCornerRadius,
            iconSize = metrics.controlIconSize,
            modifier = Modifier
                .width(if (metrics.isLandscape) 52.dp else 40.dp)
                .height(metrics.controlBarHeight)
                .testTag("key_tab"),
            onTap = onTab
        )

        // Left Cursor Navigation Button (hold-to-repeat)
        RepeatableIconKeyCap(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.cd_cursor_left),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            iconTint = MaterialTheme.colorScheme.primary,
            preferences = preferences,
            rgbController = rgbController,
            cornerRadius = metrics.controlCornerRadius,
            iconSize = metrics.repeatIconSize,
            modifier = Modifier
                .width(if (metrics.isLandscape) 56.dp else 44.dp)
                .height(metrics.controlBarHeight)
                .testTag("cursor_left_button"),
            onTrigger = onCursorLeft
        )

        // Right Cursor Navigation Button (hold-to-repeat)
        RepeatableIconKeyCap(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(R.string.cd_cursor_right),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            iconTint = MaterialTheme.colorScheme.primary,
            preferences = preferences,
            rgbController = rgbController,
            cornerRadius = metrics.controlCornerRadius,
            iconSize = metrics.repeatIconSize,
            modifier = Modifier
                .width(if (metrics.isLandscape) 56.dp else 44.dp)
                .height(metrics.controlBarHeight)
                .testTag("cursor_right_button"),
            onTrigger = onCursorRight
        )
    }
}

@Composable
fun KeyCapButton(
    label: String,
    preferences: KeyboardPreferences,
    rgbController: RgbHighlightController,
    modifier: Modifier = Modifier,
    secondaryHint: String? = null,
    secondaryHintIcon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSizeSp: Int = 16,
    hintFontSp: Int = 9,
    hintIconSize: Dp = 10.dp,
    cornerRadius: Dp = 8.dp,
    longPressDelayMs: Long = 340L,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    var isPressed by remember { mutableStateOf(false) }
    var activeDynamicColor by remember { mutableStateOf(rgbController.currentDynamicColor()) }
    val glowProgress = remember { Animatable(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "key_scale"
    )

    val rgbAppearance = resolveRgbPressAppearance(
        preferences = preferences,
        activeDynamicColor = activeDynamicColor,
        defaultPressedBg = MaterialTheme.colorScheme.primaryContainer,
        defaultPressedFg = MaterialTheme.colorScheme.onPrimaryContainer,
        defaultBorder = MaterialTheme.colorScheme.primary,
        intensity = glowProgress.value
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(containerColor)
            .then(
                if (glowProgress.value > 0.01f) {
                    Modifier.background(rgbAppearance.fillBrush)
                } else {
                    Modifier
                }
            )
            .border(
                width = if (glowProgress.value > 0.05f && preferences.highlightMode != KeyHighlightMode.OFF) 1.5.dp else 1.dp,
                brush = if (glowProgress.value > 0.05f) {
                    rgbAppearance.borderBrush
                } else {
                    SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                },
                shape = shape
            )
            .pointerInput(preferences.highlightMode, longPressDelayMs) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    if (preferences.highlightMode == KeyHighlightMode.DYNAMIC) {
                        activeDynamicColor = rgbController.nextDynamicColor()
                    }
                    scope.launch { glowProgress.snapTo(1f) }
                    var longPressed = false
                    val longPressJob = if (currentOnLongPress != null) {
                        scope.launch {
                            delay(longPressDelayMs)
                            longPressed = true
                            currentOnLongPress?.invoke()
                        }
                    } else null
                    val up = waitForUpOrCancellation()
                    longPressJob?.cancel()
                    isPressed = false
                    scope.launch {
                        glowProgress.animateTo(0f, animationSpec = tween(durationMillis = 190))
                    }
                    if (up != null && !longPressed) {
                        currentOnTap()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (secondaryHint != null) {
            Text(
                text = secondaryHint,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = hintFontSp.sp,
                lineHeight = hintFontSp.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        } else if (secondaryHintIcon != null) {
            Icon(
                imageVector = secondaryHintIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 3.dp)
                    .size(hintIconSize)
            )
        }
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSizeSp.sp,
            color = if (glowProgress.value > 0.35f && preferences.highlightMode != KeyHighlightMode.OFF) {
                rgbAppearance.contentColor
            } else if (isPressed) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                labelColor
            },
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ActionIconKeyCap(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconTint: Color,
    preferences: KeyboardPreferences,
    rgbController: RgbHighlightController,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    iconSize: Dp = 20.dp,
    subHint: String? = null,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    var isPressed by remember { mutableStateOf(false) }
    var activeDynamicColor by remember { mutableStateOf(rgbController.currentDynamicColor()) }
    val glowProgress = remember { Animatable(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "action_key_scale"
    )

    val rgbAppearance = resolveRgbPressAppearance(
        preferences = preferences,
        activeDynamicColor = activeDynamicColor,
        defaultPressedBg = MaterialTheme.colorScheme.primaryContainer,
        defaultPressedFg = MaterialTheme.colorScheme.onPrimaryContainer,
        defaultBorder = MaterialTheme.colorScheme.primary,
        intensity = glowProgress.value
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(containerColor)
            .then(
                if (glowProgress.value > 0.01f) {
                    Modifier.background(rgbAppearance.fillBrush)
                } else {
                    Modifier
                }
            )
            .border(
                width = if (glowProgress.value > 0.05f && preferences.highlightMode != KeyHighlightMode.OFF) 1.5.dp else 1.dp,
                brush = if (glowProgress.value > 0.05f) {
                    rgbAppearance.borderBrush
                } else {
                    SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                },
                shape = shape
            )
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(preferences.highlightMode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    if (preferences.highlightMode == KeyHighlightMode.DYNAMIC) {
                        activeDynamicColor = rgbController.nextDynamicColor()
                    }
                    scope.launch { glowProgress.snapTo(1f) }
                    var longPressed = false
                    val longPressJob = if (currentOnLongPress != null) {
                        scope.launch {
                            delay(360L)
                            longPressed = true
                            currentOnLongPress?.invoke()
                        }
                    } else null
                    val up = waitForUpOrCancellation()
                    longPressJob?.cancel()
                    isPressed = false
                    scope.launch {
                        glowProgress.animateTo(0f, animationSpec = tween(durationMillis = 190))
                    }
                    if (up != null && !longPressed) {
                        currentOnTap()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (subHint != null) {
            Text(
                text = subHint,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                color = iconTint.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun RepeatableIconKeyCap(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconTint: Color,
    preferences: KeyboardPreferences,
    rgbController: RgbHighlightController,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    iconSize: Dp = 21.dp,
    onTrigger: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    var isPressed by remember { mutableStateOf(false) }
    var activeDynamicColor by remember { mutableStateOf(rgbController.currentDynamicColor()) }
    val glowProgress = remember { Animatable(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "repeat_key_scale"
    )

    val rgbAppearance = resolveRgbPressAppearance(
        preferences = preferences,
        activeDynamicColor = activeDynamicColor,
        defaultPressedBg = MaterialTheme.colorScheme.primaryContainer,
        defaultPressedFg = MaterialTheme.colorScheme.onPrimaryContainer,
        defaultBorder = MaterialTheme.colorScheme.primary,
        intensity = glowProgress.value
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(containerColor)
            .then(
                if (glowProgress.value > 0.01f) {
                    Modifier.background(rgbAppearance.fillBrush)
                } else {
                    Modifier
                }
            )
            .border(
                width = if (glowProgress.value > 0.05f && preferences.highlightMode != KeyHighlightMode.OFF) 1.5.dp else 1.dp,
                brush = if (glowProgress.value > 0.05f) {
                    rgbAppearance.borderBrush
                } else {
                    SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                },
                shape = shape
            )
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(preferences.highlightMode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    if (preferences.highlightMode == KeyHighlightMode.DYNAMIC) {
                        activeDynamicColor = rgbController.nextDynamicColor()
                    }
                    scope.launch { glowProgress.snapTo(1f) }
                    currentOnTrigger()
                    val repeatJob = scope.launch {
                        delay(300L)
                        while (true) {
                            if (preferences.highlightMode == KeyHighlightMode.DYNAMIC) {
                                activeDynamicColor = rgbController.nextDynamicColor()
                            }
                            currentOnTrigger()
                            delay(65L)
                        }
                    }
                    waitForUpOrCancellation()
                    repeatJob.cancel()
                    isPressed = false
                    scope.launch {
                        glowProgress.animateTo(0f, animationSpec = tween(durationMillis = 190))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (glowProgress.value > 0.35f && preferences.highlightMode != KeyHighlightMode.OFF) {
                rgbAppearance.contentColor
            } else {
                iconTint
            },
            modifier = Modifier.size(iconSize)
        )
    }
}

fun symbolTagName(symbol: String): String = when (symbol) {
    "(" -> "lparen"
    ")" -> "rparen"
    "[" -> "lbracket"
    "]" -> "rbracket"
    "{" -> "lbrace"
    "}" -> "rbrace"
    ";" -> "semicolon"
    "<" -> "lt"
    ">" -> "gt"
    "=" -> "eq"
    "/" -> "slash"
    else -> symbol
}
