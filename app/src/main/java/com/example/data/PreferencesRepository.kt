package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SyntaxLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
            darkMode = prefs[Keys.DARK_MODE] ?: true
        )
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
}
