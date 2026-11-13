package com.example.ime

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

data class ImeSystemStatus(
    val isEnabledInSystem: Boolean,
    val isCurrentDefaultIme: Boolean
)

object ImeSystemHelper {

    fun getStatus(context: Context): ImeSystemStatus {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return ImeSystemStatus(isEnabledInSystem = false, isCurrentDefaultIme = false)

        val expectedComponent = ComponentName(context, CodeKeyboardService::class.java)
        val enabled = imm.enabledInputMethodList.any { info ->
            info.component == expectedComponent ||
                info.packageName == context.packageName &&
                info.serviceName == CodeKeyboardService::class.java.name
        }

        val defaultImeId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        val isDefault = defaultImeId == expectedComponent.flattenToShortString() ||
            defaultImeId == expectedComponent.flattenToString()

        return ImeSystemStatus(
            isEnabledInSystem = enabled,
            isCurrentDefaultIme = isDefault
        )
    }

    fun openSystemInputMethodSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun showInputMethodPicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }
}
