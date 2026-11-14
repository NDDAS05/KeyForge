package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClipboardObserver
import com.example.ui.KeyboardViewModel
import com.example.ui.screens.KeyboardWorkbenchScreen
import com.example.ui.theme.KeyForgeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: KeyboardViewModel by viewModels()
    private lateinit var clipboardObserver: ClipboardObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        clipboardObserver = ClipboardObserver(applicationContext) { copiedText ->
            viewModel.recordClipboardCopy(copiedText, notifyBadge = false)
        }
        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            KeyForgeTheme(darkTheme = prefs.darkMode) {
                KeyboardWorkbenchScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        clipboardObserver.start()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshImeStatus()
        clipboardObserver.readPrimaryClipText()?.let { existing ->
            if (existing.isNotBlank()) {
                viewModel.recordClipboardCopy(existing, notifyBadge = false)
            }
        }
    }

    override fun onStop() {
        clipboardObserver.stop()
        super.onStop()
    }
}
