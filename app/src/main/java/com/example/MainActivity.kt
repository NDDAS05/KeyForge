package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KeyboardViewModel
import com.example.ui.screens.KeyboardWorkbenchScreen
import com.example.ui.theme.KeyForgeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: KeyboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            KeyForgeTheme(darkTheme = prefs.darkMode) {
                KeyboardWorkbenchScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshImeStatus()
    }
}
