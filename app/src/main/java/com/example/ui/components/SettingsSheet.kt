package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HapticStrength
import com.example.data.KeyboardPreferences
import com.example.haptics.HapticEventType
import com.example.haptics.KeyboardHaptics
import com.example.ime.ImeSystemHelper
import com.example.ime.ImeSystemStatus
import com.example.model.SyntaxLayout
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSettingsSheet(
    preferences: KeyboardPreferences,
    imeStatus: ImeSystemStatus,
    haptics: KeyboardHaptics,
    onDismiss: () -> Unit,
    onSelectLayout: (SyntaxLayout) -> Unit,
    onToggleAutoClose: (Boolean) -> Unit,
    onToggleAngleBrackets: (Boolean) -> Unit,
    onToggleSmartBackspace: (Boolean) -> Unit,
    onToggleSmartIndent: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectHapticStrength: (HapticStrength) -> Unit,
    onToggleSyntaxStrip: (Boolean) -> Unit,
    onToggleKeyHints: (Boolean) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "KeyForge Keyboard Studio",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // System IME Activation Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (imeStatus.isEnabledInSystem) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.Keyboard
                            },
                            contentDescription = null,
                            tint = if (imeStatus.isCurrentDefaultIme) {
                                MaterialTheme.colorScheme.tertiary
                            } else if (imeStatus.isEnabledInSystem) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    imeStatus.isCurrentDefaultIme -> "Active System Keyboard"
                                    imeStatus.isEnabledInSystem -> "Enabled in Android Settings"
                                    else -> "Use System-Wide"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Enable KeyForge in Android Settings to type in Termux, editors, and browsers.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { ImeSystemHelper.openSystemInputMethodSettings(context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_enable_ime_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("1. Enable")
                        }

                        Button(
                            onClick = { ImeSystemHelper.showInputMethodPicker(context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_pick_ime_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Keyboard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("2. Select")
                        }
                    }
                }
            }

            // Syntax-Specific Layout Profiles
            SectionHeader("Syntax-Specific Character Layout")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SyntaxLayout.entries.forEach { layout ->
                    val selected = layout == preferences.syntaxLayout
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectLayout(layout) }
                            .testTag("settings_layout_${layout.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = layout.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${layout.tabSpaces}-sp Tab",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = layout.syntaxStripKeys.joinToString("  ") { it.token },
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Haptic Feedback Section
            SectionHeader("Haptic Feedback")
            SettingToggleRow(
                title = "Keypress Haptics",
                subtitle = "Tactile vibration pulse on keycaps and cursor steps",
                checked = preferences.hapticEnabled,
                onCheckedChange = { enabled ->
                    onToggleHaptics(enabled)
                    if (enabled) {
                        haptics.trigger(view, true, preferences.hapticStrength, HapticEventType.AUTO_PAIR)
                    }
                },
                testTag = "settings_haptic_switch"
            )

            if (preferences.hapticEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HapticStrength.entries.forEach { strength ->
                        FilterChip(
                            selected = preferences.hapticStrength == strength,
                            onClick = {
                                onSelectHapticStrength(strength)
                                haptics.trigger(view, true, strength, HapticEventType.KEY_TAP)
                            },
                            label = { Text(strength.label, maxLines = 1) },
                            leadingIcon = if (preferences.hapticStrength == strength) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Vibration,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("settings_haptic_${strength.id}")
                        )
                    }
                }
            }

            HorizontalDivider()

            // Smart Bracket & Editing Logic
            SectionHeader("Auto-Closing & Editor Behavior")
            SettingToggleRow(
                title = "Auto-close () [] {} \"\" ''",
                subtitle = "Inserts matching pair and positions cursor between delimiters",
                checked = preferences.autoCloseBrackets,
                onCheckedChange = onToggleAutoClose,
                testTag = "settings_autoclose_switch"
            )
            SettingToggleRow(
                title = "Auto-close angle brackets <>",
                subtitle = "Useful for Kotlin generics, C++ templates, and HTML/JSX tags",
                checked = preferences.autoCloseAngleBrackets,
                onCheckedChange = onToggleAngleBrackets,
                testTag = "settings_angle_brackets_switch"
            )
            SettingToggleRow(
                title = "Smart pair backspace",
                subtitle = "Deletes both delimiters when backspacing inside empty () [] {}",
                checked = preferences.smartPairBackspace,
                onCheckedChange = onToggleSmartBackspace,
                testTag = "settings_smart_backspace_switch"
            )
            SettingToggleRow(
                title = "Smart auto-indent on Enter",
                subtitle = "Expands {|} blocks and preserves indentation across newlines",
                checked = preferences.smartIndentOnEnter,
                onCheckedChange = onToggleSmartIndent,
                testTag = "settings_smart_indent_switch"
            )

            HorizontalDivider()

            // Appearance & Layout Density
            SectionHeader("Keyboard Deck")
            SettingToggleRow(
                title = "Syntax-specific operator strip",
                subtitle = "Show dynamic operator row below the dedicated top symbol row",
                checked = preferences.showSyntaxStrip,
                onCheckedChange = onToggleSyntaxStrip,
                testTag = "settings_syntax_strip_switch"
            )
            SettingToggleRow(
                title = "Long-press symbol hints",
                subtitle = "Display secondary digits and symbols on QWERTY keycaps",
                checked = preferences.showKeyHints,
                onCheckedChange = onToggleKeyHints,
                testTag = "settings_key_hints_switch"
            )
            SettingToggleRow(
                title = "Obsidian Dark IDE Theme",
                subtitle = "High-contrast dark surface optimized for coding",
                checked = preferences.darkMode,
                onCheckedChange = onToggleDarkMode,
                testTag = "settings_dark_mode_switch"
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
