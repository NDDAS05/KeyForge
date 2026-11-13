package com.example.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.data.HapticStrength

enum class HapticEventType {
    KEY_TAP,
    SYMBOL_TAP,
    MODIFIER_TAP,
    CURSOR_STEP,
    AUTO_PAIR
}

class KeyboardHaptics(private val context: Context) {

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun trigger(
        view: View?,
        enabled: Boolean,
        strength: HapticStrength,
        eventType: HapticEventType = HapticEventType.KEY_TAP
    ) {
        if (!enabled) return

        val feedbackConstant = when (eventType) {
            HapticEventType.CURSOR_STEP -> HapticFeedbackConstants.CLOCK_TICK
            HapticEventType.AUTO_PAIR -> HapticFeedbackConstants.CONTEXT_CLICK
            HapticEventType.MODIFIER_TAP -> HapticFeedbackConstants.VIRTUAL_KEY
            HapticEventType.KEY_TAP,
            HapticEventType.SYMBOL_TAP -> HapticFeedbackConstants.KEYBOARD_TAP
        }
        view?.performHapticFeedback(
            feedbackConstant,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )

        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val durationMultiplier = when (eventType) {
            HapticEventType.AUTO_PAIR -> 1.25f
            HapticEventType.MODIFIER_TAP -> 1.15f
            HapticEventType.CURSOR_STEP -> 0.8f
            HapticEventType.SYMBOL_TAP -> 1.1f
            HapticEventType.KEY_TAP -> 1.0f
        }
        val duration = (strength.durationMs * durationMultiplier).toLong().coerceAtLeast(6L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (vib.hasAmplitudeControl()) {
                    strength.amplitude.coerceIn(1, 255)
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                vib.vibrate(VibrationEffect.createOneShot(duration, amp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(duration)
            }
        } catch (_: SecurityException) {
            // Gracefully ignore if device denies vibration in restricted states
        }
    }
}
