package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.haptics.KeyboardHaptics
import com.example.ime.ImeSystemHelper
import com.example.ui.KeyboardViewModel
import com.example.ui.components.CodeKeyboard
import com.example.ui.components.CodeSandboxEditor
import com.example.ui.components.KeyboardSettingsSheet
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun KeyboardWorkbenchScreen(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = remember(context) { KeyboardHaptics(context) }

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val clipboardItems by viewModel.clipboardItems.collectAsStateWithLifecycle()
    val editorValue by viewModel.editorValue.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val feedbackBadge by viewModel.pairFeedbackBadge.collectAsStateWithLifecycle()
    val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
    val openSettingsRequested by viewModel.openSettingsRequested.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(openSettingsRequested) {
        if (openSettingsRequested) {
            showSettings = true
            viewModel.consumeOpenSettingsRequest()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
        ) {
            val isExpandedWide = maxWidth >= 840.dp && maxHeight >= 480.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                WorkbenchTopBar(
                    isImeEnabled = imeStatus.isEnabledInSystem,
                    isImeDefault = imeStatus.isCurrentDefaultIme,
                    activeLayoutName = preferences.syntaxLayout.displayName,
                    onOpenImeSetup = {
                        if (imeStatus.isEnabledInSystem) {
                            ImeSystemHelper.showInputMethodPicker(context)
                        } else {
                            showSettings = true
                        }
                    },
                    onOpenSettings = { showSettings = true }
                )

                if (isExpandedWide) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CodeSandboxEditor(
                            value = editorValue,
                            onValueChange = viewModel::onEditorValueChange,
                            activeLayout = preferences.syntaxLayout,
                            onSelectLayout = { viewModel.selectSyntaxLayout(it, loadSampleIfUntouched = true) },
                            canUndo = canUndo,
                            onUndo = viewModel::undo,
                            onResetSnippet = { viewModel.loadStarterSnippet() },
                            onCopyToHistory = { text -> viewModel.recordClipboardCopy(text) },
                            onClear = viewModel::clearEditor,
                            feedbackBadge = feedbackBadge,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            CodeKeyboard(
                                preferences = preferences,
                                clipboardItems = clipboardItems,
                                haptics = haptics,
                                actions = viewModel,
                                modifier = Modifier
                                    .widthIn(max = 600.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CodeSandboxEditor(
                            value = editorValue,
                            onValueChange = viewModel::onEditorValueChange,
                            activeLayout = preferences.syntaxLayout,
                            onSelectLayout = { viewModel.selectSyntaxLayout(it, loadSampleIfUntouched = true) },
                            canUndo = canUndo,
                            onUndo = viewModel::undo,
                            onResetSnippet = { viewModel.loadStarterSnippet() },
                            onCopyToHistory = { text -> viewModel.recordClipboardCopy(text) },
                            onClear = viewModel::clearEditor,
                            feedbackBadge = feedbackBadge,
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxSize()
                        )
                    }

                    CodeKeyboard(
                        preferences = preferences,
                        clipboardItems = clipboardItems,
                        haptics = haptics,
                        actions = viewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }

    if (showSettings) {
        KeyboardSettingsSheet(
            preferences = preferences,
            imeStatus = imeStatus,
            haptics = haptics,
            onDismiss = { showSettings = false },
            onSelectLayout = { viewModel.selectSyntaxLayout(it, loadSampleIfUntouched = true) },
            onToggleAutoClose = viewModel::setAutoCloseBrackets,
            onToggleAngleBrackets = viewModel::setAutoCloseAngleBrackets,
            onToggleSmartBackspace = viewModel::setSmartPairBackspace,
            onToggleSmartIndent = viewModel::setSmartIndentOnEnter,
            onToggleHaptics = viewModel::setHapticEnabled,
            onSelectHapticStrength = viewModel::setHapticStrength,
            onSelectHighlightMode = viewModel::setHighlightMode,
            onSelectStaticColor = viewModel::setStaticHighlightColor,
            onSelectGradientPreset = viewModel::setGradientPreset,
            onSelectEnterBehavior = viewModel::setEnterKeyBehavior,
            onToggleSyntaxStrip = viewModel::setShowSyntaxStrip,
            onToggleKeyHints = viewModel::setShowKeyHints,
            onToggleDarkMode = viewModel::setDarkMode
        )
    }
}

@Composable
private fun WorkbenchTopBar(
    isImeEnabled: Boolean,
    isImeDefault: Boolean,
    activeLayoutName: String,
    onOpenImeSetup: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = if (isLandscape) 2.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "{ }",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = activeLayoutName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val statusColor = when {
                isImeDefault -> MaterialTheme.colorScheme.tertiary
                isImeEnabled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }
            val statusLabel = when {
                isImeDefault -> "IME Active"
                isImeEnabled -> "Switch IME"
                else -> "Enable IME"
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenImeSetup)
                    .testTag("ime_status_pill"),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Keyboard,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("open_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
