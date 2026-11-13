package com.example.ime

import android.inputmethodservice.InputMethodService
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection
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
    private val preferencesState = MutableStateFlow(KeyboardPreferences())

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

        serviceScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                preferencesState.value = prefs
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
                KeyForgeTheme(darkTheme = prefs.darkMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        CodeKeyboard(
                            preferences = prefs,
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
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
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

            // Overtype / skip closing delimiter if cursor already sits right before it
            if (selected.isNullOrEmpty() &&
                prefs.autoCloseBrackets &&
                token in closingDelimiters &&
                ic.getTextAfterCursor(1, 0)?.toString() == token
            ) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
                return
            }

            // Auto-close pair and place cursor between delimiters
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
    }
}
