package com.example.ui

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClipboardItem
import com.example.data.EnterKeyBehavior
import com.example.data.HapticStrength
import com.example.data.KeyHighlightMode
import com.example.data.KeyboardPreferences
import com.example.data.PreferencesRepository
import com.example.data.RgbGradientPreset
import com.example.engine.EditorTextEngine
import com.example.engine.TextEditResult
import com.example.ime.ImeSystemHelper
import com.example.ime.ImeSystemStatus
import com.example.model.SyntaxLayout
import com.example.ui.components.KeyboardActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class KeyboardViewModel(application: Application) : AndroidViewModel(application), KeyboardActions {

    private val repository = PreferencesRepository(application.applicationContext)

    private val _preferences = MutableStateFlow(KeyboardPreferences())
    val preferences: StateFlow<KeyboardPreferences> = _preferences.asStateFlow()
    private var hasLocalMutation = false

    private val _clipboardItems = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clipboardItems: StateFlow<List<ClipboardItem>> = _clipboardItems.asStateFlow()

    private val initialSnippet = SyntaxLayout.KOTLIN_C.starterSnippet
    private val _editorValue = MutableStateFlow(
        TextFieldValue(
            text = initialSnippet,
            selection = TextRange(initialSnippet.length)
        )
    )
    val editorValue: StateFlow<TextFieldValue> = _editorValue.asStateFlow()

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _pairFeedbackBadge = MutableStateFlow<String?>(null)
    val pairFeedbackBadge: StateFlow<String?> = _pairFeedbackBadge.asStateFlow()

    private val _imeStatus = MutableStateFlow(ImeSystemHelper.getStatus(application))
    val imeStatus: StateFlow<ImeSystemStatus> = _imeStatus.asStateFlow()

    private val _openSettingsRequested = MutableStateFlow(false)
    val openSettingsRequested: StateFlow<Boolean> = _openSettingsRequested.asStateFlow()

    private var badgeJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = repository.preferencesFlow.first()
            if (!hasLocalMutation) {
                _preferences.value = stored
            }
        }
        viewModelScope.launch {
            repository.clipboardHistoryFlow.collect { storedClips ->
                _clipboardItems.value = storedClips
            }
        }
    }

    fun refreshImeStatus() {
        _imeStatus.value = ImeSystemHelper.getStatus(getApplication())
    }

    fun onEditorValueChange(newValue: TextFieldValue) {
        if (newValue.text != _editorValue.value.text) {
            pushUndo(_editorValue.value)
        }
        _editorValue.value = newValue
    }

    fun loadStarterSnippet(layout: SyntaxLayout = _preferences.value.syntaxLayout) {
        pushUndo(_editorValue.value)
        val snippet = layout.starterSnippet
        _editorValue.value = TextFieldValue(
            text = snippet,
            selection = TextRange(snippet.length)
        )
        showBadge("Loaded ${layout.shortLabel} snippet")
    }

    fun clearEditor() {
        if (_editorValue.value.text.isNotEmpty()) {
            pushUndo(_editorValue.value)
        }
        _editorValue.value = TextFieldValue("", TextRange.Zero)
        showBadge("Editor cleared")
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            _editorValue.value = undoStack.removeLast()
            _canUndo.value = undoStack.isNotEmpty()
            showBadge("Undid last edit")
        }
    }

    fun recordClipboardCopy(text: String, notifyBadge: Boolean = true) {
        if (text.isBlank()) return
        val existing = _clipboardItems.value.firstOrNull { it.text == text }
        val updated = _clipboardItems.value.filterNot { it.text == text }.toMutableList()
        updated.add(
            0,
            ClipboardItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                text = text,
                timestampMs = System.currentTimeMillis(),
                isPinned = existing?.isPinned ?: false
            )
        )
        _clipboardItems.value = PreferencesRepository.sortAndTrimClipboard(updated)
        viewModelScope.launch {
            repository.addClipboardItem(text)
        }
        if (notifyBadge) {
            showBadge("Copied to history")
        }
    }

    fun selectSyntaxLayout(layout: SyntaxLayout, loadSampleIfUntouched: Boolean = false) {
        val currentLayout = _preferences.value.syntaxLayout
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(syntaxLayout = layout)
        viewModelScope.launch {
            repository.setSyntaxLayout(layout)
        }
        if (loadSampleIfUntouched && _editorValue.value.text == currentLayout.starterSnippet) {
            val snippet = layout.starterSnippet
            _editorValue.value = TextFieldValue(snippet, TextRange(snippet.length))
        }
        showBadge("Layout: ${layout.displayName}")
    }

    fun setAutoCloseBrackets(enabled: Boolean) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(autoCloseBrackets = enabled)
        viewModelScope.launch { repository.setAutoCloseBrackets(enabled) }
    }

    fun setAutoCloseAngleBrackets(enabled: Boolean) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(autoCloseAngleBrackets = enabled)
        viewModelScope.launch { repository.setAutoCloseAngleBrackets(enabled) }
    }

    fun setSmartPairBackspace(enabled: Boolean) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(smartPairBackspace = enabled)
        viewModelScope.launch { repository.setSmartPairBackspace(enabled) }
    }

    fun setSmartIndentOnEnter(enabled: Boolean) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(smartIndentOnEnter = enabled)
        viewModelScope.launch { repository.setSmartIndentOnEnter(enabled) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(hapticEnabled = enabled)
        viewModelScope.launch { repository.setHapticEnabled(enabled) }
    }

    fun setHapticStrength(strength: HapticStrength) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(hapticStrength = strength)
        viewModelScope.launch { repository.setHapticStrength(strength) }
    }

    fun setShowSyntaxStrip(show: Boolean) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(showSyntaxStrip = show)
        viewModelScope.launch { repository.setShowSyntaxStrip(show) }
    }

    fun setShowKeyHints(show: Boolean) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(showKeyHints = show)
        viewModelScope.launch { repository.setShowKeyHints(show) }
    }

    fun setDarkMode(dark: Boolean) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(darkMode = dark)
        viewModelScope.launch { repository.setDarkMode(dark) }
    }

    fun setHighlightMode(mode: KeyHighlightMode) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(highlightMode = mode)
        viewModelScope.launch { repository.setHighlightMode(mode) }
    }

    fun setStaticHighlightColor(argb: Long) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(staticHighlightColorArgb = argb)
        viewModelScope.launch { repository.setStaticHighlightColor(argb) }
    }

    fun setGradientPreset(preset: RgbGradientPreset) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(gradientPreset = preset)
        viewModelScope.launch { repository.setGradientPreset(preset) }
    }

    fun setEnterKeyBehavior(behavior: EnterKeyBehavior) {
        hasLocalMutation = true
        _preferences.value = _preferences.value.copy(enterKeyBehavior = behavior)
        viewModelScope.launch { repository.setEnterKeyBehavior(behavior) }
    }

    override fun onCommitToken(token: String) {
        val current = _editorValue.value
        val result = EditorTextEngine.insertToken(
            text = current.text,
            selectionStart = current.selection.start,
            selectionEnd = current.selection.end,
            token = token,
            prefs = _preferences.value
        )
        applyEditResult(current, result)
    }

    override fun onBackspace() {
        val current = _editorValue.value
        val result = EditorTextEngine.backspace(
            text = current.text,
            selectionStart = current.selection.start,
            selectionEnd = current.selection.end,
            prefs = _preferences.value
        )
        applyEditResult(current, result)
    }

    override fun onEnter() {
        val current = _editorValue.value
        val result = EditorTextEngine.enter(
            text = current.text,
            selectionStart = current.selection.start,
            selectionEnd = current.selection.end,
            prefs = _preferences.value
        )
        applyEditResult(current, result)
    }

    override fun onNewline() {
        val current = _editorValue.value
        val result = EditorTextEngine.newline(
            text = current.text,
            selectionStart = current.selection.start,
            selectionEnd = current.selection.end
        )
        applyEditResult(current, result)
    }

    override fun onSpace() {
        onCommitToken(" ")
    }

    override fun onTab() {
        val current = _editorValue.value
        val result = EditorTextEngine.insertTab(
            text = current.text,
            selectionStart = current.selection.start,
            selectionEnd = current.selection.end,
            prefs = _preferences.value
        )
        applyEditResult(current, result)
    }

    override fun onCursorLeft() {
        val current = _editorValue.value
        val result = EditorTextEngine.moveCursorLeft(
            text = current.text,
            selectionStart = current.selection.start,
            selectionEnd = current.selection.end
        )
        _editorValue.value = current.copy(
            selection = TextRange(result.selectionStart, result.selectionEnd)
        )
    }

    override fun onCursorRight() {
        val current = _editorValue.value
        val result = EditorTextEngine.moveCursorRight(
            text = current.text,
            selectionStart = current.selection.start,
            selectionEnd = current.selection.end
        )
        _editorValue.value = current.copy(
            selection = TextRange(result.selectionStart, result.selectionEnd)
        )
    }

    override fun onToggleSyntaxLayout() {
        val next = _preferences.value.syntaxLayout.next()
        selectSyntaxLayout(next, loadSampleIfUntouched = true)
    }

    override fun onToggleAutoClose() {
        val next = !_preferences.value.autoCloseBrackets
        setAutoCloseBrackets(next)
        showBadge(if (next) "Auto-close pairs ON" else "Auto-close pairs OFF")
    }

    override fun onToggleHaptics() {
        val next = !_preferences.value.hapticEnabled
        setHapticEnabled(next)
        showBadge(if (next) "Haptic feedback ON" else "Haptic feedback OFF")
    }

    override fun onOpenSettings() {
        _openSettingsRequested.value = true
    }

    fun requestOpenSettings() {
        _openSettingsRequested.value = true
    }

    fun consumeOpenSettingsRequest() {
        _openSettingsRequested.value = false
    }

    override fun onPasteClipboardItem(text: String) {
        val current = _editorValue.value
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val newText = current.text.substring(0, start) + text + current.text.substring(end)
        val cursor = start + text.length
        applyEditResult(
            previous = current,
            result = TextEditResult(
                text = newText,
                selectionStart = cursor,
                selectionEnd = cursor,
                feedbackBadge = "Pasted snippet"
            )
        )
    }

    override fun onTogglePinClipboardItem(id: String) {
        val updated = _clipboardItems.value.map { item ->
            if (item.id == id) item.copy(isPinned = !item.isPinned) else item
        }
        _clipboardItems.value = PreferencesRepository.sortAndTrimClipboard(updated)
        viewModelScope.launch { repository.togglePinClipboardItem(id) }
    }

    override fun onDeleteClipboardItem(id: String) {
        _clipboardItems.value = _clipboardItems.value.filterNot { it.id == id }
        viewModelScope.launch { repository.deleteClipboardItem(id) }
    }

    override fun onClearUnpinnedClipboard() {
        _clipboardItems.value = _clipboardItems.value.filter { it.isPinned }
        viewModelScope.launch { repository.clearClipboardHistory(keepPinned = true) }
    }

    private fun applyEditResult(previous: TextFieldValue, result: TextEditResult) {
        if (result.text != previous.text) {
            pushUndo(previous)
        }
        _editorValue.value = TextFieldValue(
            text = result.text,
            selection = TextRange(result.selectionStart, result.selectionEnd)
        )
        result.feedbackBadge?.let { showBadge(it) }
    }

    private fun pushUndo(snapshot: TextFieldValue) {
        if (undoStack.lastOrNull()?.text == snapshot.text) return
        if (undoStack.size >= 30) {
            undoStack.removeFirst()
        }
        undoStack.addLast(snapshot)
        _canUndo.value = true
    }

    private fun showBadge(message: String) {
        _pairFeedbackBadge.value = message
        badgeJob?.cancel()
        badgeJob = viewModelScope.launch {
            delay(1800L)
            _pairFeedbackBadge.value = null
        }
    }
}
