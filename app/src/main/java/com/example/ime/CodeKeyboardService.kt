package com.example.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.ClipboardItem
import com.example.data.ClipboardObserver
import com.example.data.KeyboardPreferences
import com.example.data.PreferencesRepository
import com.example.engine.EditorTextEngine
import com.example.haptics.KeyboardHaptics
import com.example.model.SyntaxLayout
import com.example.ui.components.CodeKeyboard
import com.example.ui.components.KeyboardActions
import com.example.ui.theme.KeyForgeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CodeKeyboardService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var haptics: KeyboardHaptics
    private lateinit var clipboardObserver: ClipboardObserver
    private val preferencesState = MutableStateFlow(KeyboardPreferences())
    private val clipboardState = MutableStateFlow<List<ClipboardItem>>(emptyList())

    private val matchingPairs = setOf(
        "(" to ")",
        "[" to "]",
        "{" to "}",
        "<" to ">",
        "\"" to "\"",
        "'" to "'",
        "`" to "`"
    )

    private val closingDelimiters = setOf(")", "]", "}", ">", "\"", "'", "`")

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        preferencesRepository = PreferencesRepository(applicationContext)
        haptics = KeyboardHaptics(applicationContext)
        clipboardObserver = ClipboardObserver(applicationContext) { clipText ->
            serviceScope.launch {
                preferencesRepository.addClipboardItem(clipText)
            }
        }

        serviceScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                preferencesState.value = prefs
            }
        }
        serviceScope.launch {
            preferencesRepository.clipboardHistoryFlow.collect { items ->
                clipboardState.value = items
            }
        }
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CodeKeyboardService)
            setViewTreeViewModelStoreOwner(this@CodeKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@CodeKeyboardService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val prefs by preferencesState.collectAsState()
                val clips by clipboardState.collectAsState()
                KeyForgeTheme(darkTheme = prefs.darkMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        CodeKeyboard(
                            preferences = prefs,
                            clipboardItems = clips,
                            haptics = haptics,
                            actions = imeKeyboardActions
                        )
                    }
                }
            }
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        clipboardObserver.start()
        clipboardObserver.readPrimaryClipText()?.let { clipText ->
            if (clipText.isNotBlank()) {
                serviceScope.launch { preferencesRepository.addClipboardItem(clipText) }
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onWindowHidden() {
        clipboardObserver.stop()
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        clipboardObserver.stop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    private val imeKeyboardActions = object : KeyboardActions {
        override fun onCommitToken(token: String) {
            val ic = currentInputConnection ?: return
            val prefs = preferencesState.value
            val selected = ic.getSelectedText(0)?.toString()

            if (token == "f\"\"") {
                ic.beginBatchEdit()
                ic.commitText("f\"", 1)
                ic.commitText("\"", 0)
                ic.endBatchEdit()
                return
            }

            val closing = EditorTextEngine.closingFor(token, prefs)
            if (!selected.isNullOrEmpty() && closing != null) {
                ic.commitText("$token$selected$closing", 1)
                return
            }

            if (selected.isNullOrEmpty() &&
                prefs.autoCloseBrackets &&
                token in closingDelimiters &&
                ic.getTextAfterCursor(1, 0)?.toString() == token
            ) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
                return
            }

            if (closing != null) {
                ic.beginBatchEdit()
                ic.commitText(token, 1)
                ic.commitText(closing, 0)
                ic.endBatchEdit()
                return
            }

            ic.commitText(token, 1)
        }

        override fun onBackspace() {
            val ic = currentInputConnection ?: return
            val prefs = preferencesState.value
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
                return
            }

            if (prefs.smartPairBackspace) {
                val before = ic.getTextBeforeCursor(1, 0)?.toString()
                val after = ic.getTextAfterCursor(1, 0)?.toString()
                if (before != null && after != null && (before to after) in matchingPairs) {
                    ic.deleteSurroundingText(1, 1)
                    return
                }
            }
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }

        override fun onEnter() {
            val ic = currentInputConnection ?: return
            val editorInfo = currentInputEditorInfo
            if (editorInfo != null) {
                val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
                val noEnterAction = (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                val isMultiline = (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
                if (!noEnterAction &&
                    !isMultiline &&
                    action != EditorInfo.IME_ACTION_NONE &&
                    action != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    ic.performEditorAction(action)
                    return
                }
            }

            val prefs = preferencesState.value
            if (!prefs.smartIndentOnEnter) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                return
            }

            val beforeLine = ic.getTextBeforeCursor(120, 0)?.toString()
                ?.substringAfterLast('\n') ?: ""
            val baseIndent = beforeLine.takeWhile { it == ' ' || it == '\t' }
            val indentUnit = " ".repeat(prefs.syntaxLayout.tabSpaces)
            val charBefore = beforeLine.trimEnd().lastOrNull()
            val charAfter = ic.getTextAfterCursor(1, 0)?.toString()?.firstOrNull()

            val isEmptyBlock = (charBefore == '{' && charAfter == '}') ||
                (charBefore == '(' && charAfter == ')') ||
                (charBefore == '[' && charAfter == ']')

            if (isEmptyBlock) {
                val innerIndent = baseIndent + indentUnit
                ic.beginBatchEdit()
                ic.commitText("\n$innerIndent", 1)
                ic.commitText("\n$baseIndent", 0)
                ic.endBatchEdit()
            } else {
                val shouldIncrease = when (charBefore) {
                    '{', '(', '[' -> true
                    ':' -> prefs.syntaxLayout == SyntaxLayout.PYTHON
                    else -> false
                }
                val nextIndent = if (shouldIncrease) baseIndent + indentUnit else baseIndent
                ic.commitText("\n$nextIndent", 1)
            }
        }

        override fun onNewline() {
            currentInputConnection?.commitText("\n", 1)
        }

        override fun onSpace() {
            currentInputConnection?.commitText(" ", 1)
        }

        override fun onTab() {
            val spaces = " ".repeat(preferencesState.value.syntaxLayout.tabSpaces)
            currentInputConnection?.commitText(spaces, 1)
        }

        override fun onCursorLeft() {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
        }

        override fun onCursorRight() {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
        }

        override fun onToggleSyntaxLayout() {
            val next = preferencesState.value.syntaxLayout.next()
            serviceScope.launch { preferencesRepository.setSyntaxLayout(next) }
        }

        override fun onToggleAutoClose() {
            val next = !preferencesState.value.autoCloseBrackets
            serviceScope.launch { preferencesRepository.setAutoCloseBrackets(next) }
        }

        override fun onToggleHaptics() {
            val next = !preferencesState.value.hapticEnabled
            serviceScope.launch { preferencesRepository.setHapticEnabled(next) }
        }

        override fun onOpenSettings() {
            val intent = Intent(this@CodeKeyboardService, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
            }
            startActivity(intent)
        }

        override fun onPasteClipboardItem(text: String) {
            currentInputConnection?.commitText(text, 1)
        }

        override fun onTogglePinClipboardItem(id: String) {
            serviceScope.launch { preferencesRepository.togglePinClipboardItem(id) }
        }

        override fun onDeleteClipboardItem(id: String) {
            serviceScope.launch { preferencesRepository.deleteClipboardItem(id) }
        }

        override fun onClearUnpinnedClipboard() {
            serviceScope.launch { preferencesRepository.clearClipboardHistory(keepPinned = true) }
        }
    }
}
