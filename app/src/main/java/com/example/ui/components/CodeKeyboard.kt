package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.KeyboardTab
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.KeyboardCapslock
import androidx.compose.material.icons.rounded.North
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
import androidx.compose.ui.input.pointer.pointerInput
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
    fun onSpace()
    fun onTab()
    fun onCursorLeft()
    fun onCursorRight()
    fun onToggleSyntaxLayout()
    fun onToggleAutoClose()
    fun onToggleHaptics()
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
    haptics: KeyboardHaptics,
    actions: KeyboardActions,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var keyLayer by remember { mutableStateOf(KeyLayer.QWERTY) }
    var lastShiftTapMs by remember { mutableLongStateOf(0L) }

    val activeLayout = preferences.syntaxLayout

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Top Utility Bar: Syntax Layout Toggle, Auto-Pair, Haptics, Tab, Left/Right Cursor
            KeyboardControlStrip(
                preferences = preferences,
                onToggleSyntax = {
                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                    actions.onToggleSyntaxLayout()
                },
                onToggleAutoClose = {
                    haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                    actions.onToggleAutoClose()
                },
                onToggleHaptics = {
                    // Always give a tactile confirmation when enabling
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

            // Dedicated Top Priority Symbol Row: ( ) [ ] { } ; < > = /
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dedicated_symbol_row"),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("symbol_key_${symbolTagName(symbol)}"),
                        labelColor = labelColor,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        fontSizeSp = 16,
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
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        layout.syntaxStripKeys.forEach { spec ->
                            val isQuote = spec.token in setOf("\"", "'", "`", "f\"\"")
                            KeyCapButton(
                                label = spec.token,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("syntax_key_${spec.token}"),
                                labelColor = if (isQuote) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                fontSizeSp = if (spec.token.length >= 3) 11 else 13,
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
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    QWERTY_ROW_1.forEach { ch ->
                        val display = if (shiftState != ShiftState.OFF) ch.uppercase() else ch
                        val hint = if (preferences.showKeyHints) activeLayout.secondaryHints[ch] else null
                        KeyCapButton(
                            label = display,
                            secondaryHint = hint,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
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
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    QWERTY_ROW_2.forEach { ch ->
                        val display = if (shiftState != ShiftState.OFF) ch.uppercase() else ch
                        val hint = if (preferences.showKeyHints) activeLayout.secondaryHints[ch] else null
                        KeyCapButton(
                            label = display,
                            secondaryHint = hint,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                        modifier = Modifier
                            .weight(1.45f)
                            .height(46.dp)
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
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
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
                        modifier = Modifier
                            .weight(1.45f)
                            .height(46.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    NUM_ROW_1.forEach { digit ->
                        KeyCapButton(
                            label = digit,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    NUM_ROW_2.forEach { sym ->
                        KeyCapButton(
                            label = sym,
                            labelColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    NUM_ROW_3.forEach { sym ->
                        KeyCapButton(
                            label = sym,
                            labelColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
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
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("key_backspace"),
                        onTrigger = {
                            haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                            actions.onBackspace()
                        }
                    )
                }
            }

            // Bottom Action Row: 123/ABC, Comma, Spacebar, Period, Enter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                KeyCapButton(
                    label = if (keyLayer == KeyLayer.QWERTY) "123" else "ABC",
                    modifier = Modifier
                        .weight(1.4f)
                        .height(46.dp)
                        .testTag("layer_toggle_button"),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    fontSizeSp = 13,
                    onTap = {
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                        keyLayer = if (keyLayer == KeyLayer.QWERTY) KeyLayer.SYMBOLS_NUMBERS else KeyLayer.QWERTY
                    }
                )

                KeyCapButton(
                    label = ",",
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("key_comma"),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onTap = {
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                        actions.onCommitToken(",")
                    }
                )

                KeyCapButton(
                    label = activeLayout.spaceLabel,
                    modifier = Modifier
                        .weight(4.2f)
                        .height(46.dp)
                        .testTag("key_space"),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSizeSp = 11,
                    onTap = {
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                        actions.onSpace()
                    }
                )

                KeyCapButton(
                    label = ".",
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("key_period"),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onTap = {
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.KEY_TAP)
                        actions.onCommitToken(".")
                    }
                )

                ActionIconKeyCap(
                    icon = Icons.AutoMirrored.Rounded.KeyboardReturn,
                    contentDescription = stringResource(R.string.cd_enter),
                    containerColor = MaterialTheme.colorScheme.primary,
                    iconTint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .weight(1.6f)
                        .height(46.dp)
                        .testTag("key_enter"),
                    onTap = {
                        haptics.trigger(view, preferences.hapticEnabled, preferences.hapticStrength, HapticEventType.MODIFIER_TAP)
                        actions.onEnter()
                    }
                )
            }
        }
    }
}

@Composable
private fun KeyboardControlStrip(
    preferences: KeyboardPreferences,
    onToggleSyntax: () -> Unit,
    onToggleAutoClose: () -> Unit,
    onToggleHaptics: () -> Unit,
    onTab: () -> Unit,
    onCursorLeft: () -> Unit,
    onCursorRight: () -> Unit
) {
    val syntaxBadgeDesc = stringResource(R.string.cd_syntax_toggle)
    val autoCloseDesc = stringResource(R.string.cd_autoclose_toggle)
    val hapticDesc = stringResource(R.string.cd_haptic_toggle)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Syntax Layout Toggle Chip
        Surface(
            modifier = Modifier
                .weight(1.6f)
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onToggleSyntax)
                .semantics { contentDescription = syntaxBadgeDesc }
                .testTag("syntax_layout_toggle"),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = preferences.syntaxLayout.shortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Auto-Close Pair Toggle Chip
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
        Surface(
            modifier = Modifier
                .weight(1.2f)
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onToggleAutoClose)
                .semantics { contentDescription = autoCloseDesc }
                .testTag("auto_close_toggle"),
            shape = RoundedCornerShape(10.dp),
            color = pairBg,
            border = BorderStroke(
                1.dp,
                if (preferences.autoCloseBrackets) pairFg.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
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
                    fontSize = 12.sp,
                    color = pairFg,
                    maxLines = 1
                )
            }
        }

        // Haptic Toggle Chip
        val hapticColor = if (preferences.hapticEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
        Surface(
            modifier = Modifier
                .width(44.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onToggleHaptics)
                .semantics { contentDescription = hapticDesc }
                .testTag("haptic_toggle"),
            shape = RoundedCornerShape(10.dp),
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
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Tab Indent Key
        ActionIconKeyCap(
            icon = Icons.AutoMirrored.Rounded.KeyboardTab,
            contentDescription = stringResource(R.string.cd_tab),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconTint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .width(44.dp)
                .height(42.dp)
                .testTag("key_tab"),
            onTap = onTab
        )

        // Left Cursor Navigation Button (hold-to-repeat)
        RepeatableIconKeyCap(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.cd_cursor_left),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            iconTint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(48.dp)
                .height(42.dp)
                .testTag("cursor_left_button"),
            onTrigger = onCursorLeft
        )

        // Right Cursor Navigation Button (hold-to-repeat)
        RepeatableIconKeyCap(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(R.string.cd_cursor_right),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            iconTint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(48.dp)
                .height(42.dp)
                .testTag("cursor_right_button"),
            onTrigger = onCursorRight
        )
    }
}

@Composable
private fun KeyCapButton(
    label: String,
    modifier: Modifier = Modifier,
    secondaryHint: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSizeSp: Int = 16,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "key_scale"
    )
    val bg by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primaryContainer else containerColor,
        label = "key_bg"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (isPressed) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    var longPressed = false
                    val longPressJob = if (currentOnLongPress != null) {
                        scope.launch {
                            delay(340L)
                            longPressed = true
                            currentOnLongPress?.invoke()
                        }
                    } else null
                    val up = waitForUpOrCancellation()
                    longPressJob?.cancel()
                    isPressed = false
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
                fontSize = 9.sp,
                lineHeight = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        }
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSizeSp.sp,
            color = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer else labelColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ActionIconKeyCap(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "action_key_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp)
            )
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
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
                    if (up != null && !longPressed) {
                        currentOnTap()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun RepeatableIconKeyCap(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onTrigger: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "repeat_key_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) MaterialTheme.colorScheme.primaryContainer else containerColor)
            .border(
                width = 1.dp,
                color = if (isPressed) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    currentOnTrigger()
                    val repeatJob = scope.launch {
                        delay(300L)
                        while (true) {
                            currentOnTrigger()
                            delay(65L)
                        }
                    }
                    waitForUpOrCancellation()
                    repeatJob.cancel()
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer else iconTint,
            modifier = Modifier.size(21.dp)
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
