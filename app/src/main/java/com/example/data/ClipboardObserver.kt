package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardObserver(
    context: Context,
    private val onClipCaptured: (String) -> Unit
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        readPrimaryClipText()?.let { text ->
            if (text.isNotBlank()) {
                onClipCaptured(text)
            }
        }
    }

    fun start() {
        clipboardManager?.addPrimaryClipChangedListener(listener)
    }

    fun stop() {
        clipboardManager?.removePrimaryClipChangedListener(listener)
    }

    fun readPrimaryClipText(): String? {
        val clip = clipboardManager?.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0)?.text?.toString()
    }

    fun copyText(text: String, label: String = "KeyForge") {
        clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
