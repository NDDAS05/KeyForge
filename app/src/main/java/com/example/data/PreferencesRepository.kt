package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SyntaxLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.keyForgeDataStore: DataStore<Preferences> by preferencesDataStore(name = "keyforge_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val SYNTAX_LAYOUT = stringPreferencesKey("syntax_layout")
        val AUTO_CLOSE_BRACKETS = booleanPreferencesKey("auto_close_brackets")
        val AUTO_CLOSE_ANGLE_BRACKETS = booleanPreferencesKey("auto_close_angle_brackets")
        val SMART_PAIR_BACKSPACE = booleanPreferencesKey("smart_pair_backspace")
        val SMART_INDENT_ON_ENTER = booleanPreferencesKey("smart_indent_on_enter")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val HAPTIC_STRENGTH = stringPreferencesKey("haptic_strength")
        val SHOW_SYNTAX_STRIP = booleanPreferencesKey("show_syntax_strip")
        val SHOW_KEY_HINTS = booleanPreferencesKey("show_key_hints")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val HIGHLIGHT_MODE = stringPreferencesKey("highlight_mode")
        val STATIC_HIGHLIGHT_COLOR = longPreferencesKey("static_highlight_color")
        val GRADIENT_PRESET = stringPreferencesKey("gradient_preset")
        val ENTER_KEY_BEHAVIOR = stringPreferencesKey("enter_key_behavior")
        val CLIPBOARD_HISTORY = stringPreferencesKey("clipboard_history_json")
    }

    val preferencesFlow: Flow<KeyboardPreferences> = context.keyForgeDataStore.data.map { prefs ->
        KeyboardPreferences(
            syntaxLayout = SyntaxLayout.fromId(prefs[Keys.SYNTAX_LAYOUT]),
            autoCloseBrackets = prefs[Keys.AUTO_CLOSE_BRACKETS] ?: true,
            autoCloseAngleBrackets = prefs[Keys.AUTO_CLOSE_ANGLE_BRACKETS] ?: true,
            smartPairBackspace = prefs[Keys.SMART_PAIR_BACKSPACE] ?: true,
            smartIndentOnEnter = prefs[Keys.SMART_INDENT_ON_ENTER] ?: true,
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            hapticStrength = HapticStrength.fromId(prefs[Keys.HAPTIC_STRENGTH]),
            showSyntaxStrip = prefs[Keys.SHOW_SYNTAX_STRIP] ?: true,
            showKeyHints = prefs[Keys.SHOW_KEY_HINTS] ?: true,
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            highlightMode = KeyHighlightMode.fromId(prefs[Keys.HIGHLIGHT_MODE]),
            staticHighlightColorArgb = prefs[Keys.STATIC_HIGHLIGHT_COLOR] ?: 0xFF22D3EE,
            gradientPreset = RgbGradientPreset.fromId(prefs[Keys.GRADIENT_PRESET]),
            enterKeyBehavior = EnterKeyBehavior.fromId(prefs[Keys.ENTER_KEY_BEHAVIOR])
        )
    }

    val clipboardHistoryFlow: Flow<List<ClipboardItem>> = context.keyForgeDataStore.data.map { prefs ->
        decodeClipboardHistory(prefs[Keys.CLIPBOARD_HISTORY])
    }

    suspend fun setSyntaxLayout(layout: SyntaxLayout) {
        context.keyForgeDataStore.edit { it[Keys.SYNTAX_LAYOUT] = layout.id }
    }

    suspend fun setAutoCloseBrackets(enabled: Boolean) {
        context.keyForgeDataStore.edit { it[Keys.AUTO_CLOSE_BRACKETS] = enabled }
    }

    suspend fun setAutoCloseAngleBrackets(enabled: Boolean) {
        context.keyForgeDataStore.edit { it[Keys.AUTO_CLOSE_ANGLE_BRACKETS] = enabled }
    }

    suspend fun setSmartPairBackspace(enabled: Boolean) {
        context.keyForgeDataStore.edit { it[Keys.SMART_PAIR_BACKSPACE] = enabled }
    }

    suspend fun setSmartIndentOnEnter(enabled: Boolean) {
        context.keyForgeDataStore.edit { it[Keys.SMART_INDENT_ON_ENTER] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.keyForgeDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setHapticStrength(strength: HapticStrength) {
        context.keyForgeDataStore.edit { it[Keys.HAPTIC_STRENGTH] = strength.id }
    }

    suspend fun setShowSyntaxStrip(show: Boolean) {
        context.keyForgeDataStore.edit { it[Keys.SHOW_SYNTAX_STRIP] = show }
    }

    suspend fun setShowKeyHints(show: Boolean) {
        context.keyForgeDataStore.edit { it[Keys.SHOW_KEY_HINTS] = show }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.keyForgeDataStore.edit { it[Keys.DARK_MODE] = dark }
    }

    suspend fun setHighlightMode(mode: KeyHighlightMode) {
        context.keyForgeDataStore.edit { it[Keys.HIGHLIGHT_MODE] = mode.id }
    }

    suspend fun setStaticHighlightColor(argb: Long) {
        context.keyForgeDataStore.edit { it[Keys.STATIC_HIGHLIGHT_COLOR] = argb }
    }

    suspend fun setGradientPreset(preset: RgbGradientPreset) {
        context.keyForgeDataStore.edit { it[Keys.GRADIENT_PRESET] = preset.id }
    }

    suspend fun setEnterKeyBehavior(behavior: EnterKeyBehavior) {
        context.keyForgeDataStore.edit { it[Keys.ENTER_KEY_BEHAVIOR] = behavior.id }
    }

    suspend fun addClipboardItem(text: String) {
        if (text.isBlank()) return
        context.keyForgeDataStore.edit { prefs ->
            val current = decodeClipboardHistory(prefs[Keys.CLIPBOARD_HISTORY]).toMutableList()
            val existing = current.firstOrNull { it.text == text }
            current.removeAll { it.text == text }
            val updatedItem = ClipboardItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                text = text,
                timestampMs = System.currentTimeMillis(),
                isPinned = existing?.isPinned ?: false
            )
            current.add(0, updatedItem)
            val sorted = sortAndTrimClipboard(current)
            prefs[Keys.CLIPBOARD_HISTORY] = encodeClipboardHistory(sorted)
        }
    }

    suspend fun togglePinClipboardItem(id: String) {
        context.keyForgeDataStore.edit { prefs ->
            val current = decodeClipboardHistory(prefs[Keys.CLIPBOARD_HISTORY]).map { item ->
                if (item.id == id) item.copy(isPinned = !item.isPinned) else item
            }
            prefs[Keys.CLIPBOARD_HISTORY] = encodeClipboardHistory(sortAndTrimClipboard(current))
        }
    }

    suspend fun deleteClipboardItem(id: String) {
        context.keyForgeDataStore.edit { prefs ->
            val current = decodeClipboardHistory(prefs[Keys.CLIPBOARD_HISTORY])
                .filterNot { it.id == id }
            prefs[Keys.CLIPBOARD_HISTORY] = encodeClipboardHistory(current)
        }
    }

    suspend fun clearClipboardHistory(keepPinned: Boolean = true) {
        context.keyForgeDataStore.edit { prefs ->
            val remaining = if (keepPinned) {
                decodeClipboardHistory(prefs[Keys.CLIPBOARD_HISTORY]).filter { it.isPinned }
            } else {
                emptyList()
            }
            prefs[Keys.CLIPBOARD_HISTORY] = encodeClipboardHistory(remaining)
        }
    }

    companion object {
        private const val MAX_UNPINNED_ITEMS = 20

        fun sortAndTrimClipboard(items: List<ClipboardItem>): List<ClipboardItem> {
            val pinned = items.filter { it.isPinned }.sortedByDescending { it.timestampMs }
            val unpinned = items.filterNot { it.isPinned }
                .sortedByDescending { it.timestampMs }
                .take(MAX_UNPINNED_ITEMS)
            return pinned + unpinned
        }

        fun encodeClipboardHistory(items: List<ClipboardItem>): String {
            val array = JSONArray()
            items.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("text", item.text)
                obj.put("ts", item.timestampMs)
                obj.put("pinned", item.isPinned)
                array.put(obj)
            }
            return array.toString()
        }

        fun decodeClipboardHistory(raw: String?): List<ClipboardItem> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val text = obj.optString("text", "")
                        if (text.isNotEmpty()) {
                            add(
                                ClipboardItem(
                                    id = obj.optString("id", UUID.randomUUID().toString()),
                                    text = text,
                                    timestampMs = obj.optLong("ts", 0L),
                                    isPinned = obj.optBoolean("pinned", false)
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
