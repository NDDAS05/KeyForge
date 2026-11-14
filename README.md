# KeyForge

A custom Android programming keyboard and live code workbench built with **Kotlin** and **Jetpack Compose**, designed for fast mobile coding with one-tap symbol access, smart bracket pairing, cursor navigation, clipboard history, and customizable RGB keypress feedback.

---

## Features

### Programming-First Key Layout
- **Dedicated Top Symbol Row**: One-tap access to `(`, `)`, `[`, `]`, `{`, `}`, `;`, `<`, `>`, `=`, and `/` with color-coded brackets and operators.
- **Syntax-Specific Layout Toggle**: Switch with one tap between four language-tuned operator strips, tab widths (`2` or `4` spaces), and long-press keycap hints:
  - **Kotlin / C-Style (`KT/C`)**: `->`, `::`, `?.`, `&&`, `||`, `!=`, `"`, `'`, `:`, `_`
  - **Python / Data (`PY`)**: `_`, `#`, `:`, `->`, `**`, `!=`, `f""`, `"`, `'`, `\`
  - **Web / JSX / HTML (`WEB`)**: `=>`, `</`, `/>`, `===`, `!==`, `$`, `` ` ``, `"`, `'`, `:`
  - **SQL / Terminal (`SQL/SH`)**: `*`, `|`, `&`, `$`, `--`, `'`, `"`, `%`, `_`, `~`
- **QWERTY + `123` / Extended Symbols Layer**: Includes Shift / Caps Lock, long-press secondary hints (`1–0` and syntax operators), and a full numeric/symbol layer.

### Smart Bracket & Cursor Engine
- **Auto-Closing Pairs**: Automatically inserts matching `()`, `[]`, `{}`, `""`, `''`, `` `` ``, and configurable `<>` pairs while placing the cursor directly between the delimiters.
- **Selection Wrapping**: Selecting text and tapping an opening bracket or quote wraps the selection automatically.
- **Overtype Skip & Smart Pair Backspace**: Re-typing a closing delimiter steps the cursor past it; pressing Backspace inside an empty pair (`(|)`) deletes both characters together.
- **Left / Right Cursor Navigation**: Dedicated `◀` and `▶` buttons in the top control strip with hold-to-repeat stepping.
- **Interchangeable Enter / Newline Key**: Supports both **Smart Enter** (block expansion for `{|}`, auto-indentation, and IME editor actions) and **Raw Newline (`\n`)**, with a Settings toggle to swap **Tap: Enter • Hold: Newline** and **Tap: Newline • Hold: Enter**.

### Clipboard History, RGB Highlights & Haptics
- **Clipboard History Drawer**: Accessible directly from the keyboard control bar; captures copied snippets, persists up to 20 recent items (plus unlimited pinned items) via DataStore, and supports one-tap paste, pin/unpin, delete, and clear.
- **Customizable Keypress RGB Highlights**:
  - **Off**: Standard Material 3 surface highlight.
  - **Static**: Fixed RGB color via quick swatches or fine-grained Red / Green / Blue sliders (`0–255`).
  - **Dynamic**: Steps to a new vibrant RGB hue on every individual keypress.
  - **Gradient**: Multi-stop RGB gradient fills and borders (`Cyber Prism`, `Aurora`, `Solar Flare`, `Neon Pulse`).
- **Haptic Feedback Studio**: Toggleable tactile vibration with **Soft Tap**, **Crisp Click**, and **Mechanical Tactile** profiles.

### Dual Mode: In-App Code Sandbox + System Keyboard (IME)
- **In-App Code Workbench**: Syntax-highlighted `JetBrains Mono` editor with line numbers, cursor line/column telemetry, starter snippets per language, undo, copy, and live auto-pair badges.
- **System-Wide Android IME (`CodeKeyboardService`)**: Enable **KeyForge** in Android *Settings → System → Keyboard* to use it in Termux, code editors, browsers, and other apps.

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material Design 3, Edge-to-Edge
- **Typography**: Bundled `JetBrains Mono` & `Plus Jakarta Sans`
- **Persistence**: Jetpack DataStore Preferences
- **IME**: Android `InputMethodService` hosting Compose UI
- **Testing**: JUnit 4, Robolectric, Compose UI Test

---

## Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                        # App entry point & clipboard lifecycle observer
├── data/
│   ├── ClipboardObserver.kt               # System ClipboardManager listener & copy helper
│   ├── KeyboardPreferences.kt             # Settings, RGB, Enter/Newline & clipboard models
│   └── PreferencesRepository.kt           # DataStore persistence layer
├── engine/
│   └── EditorTextEngine.kt                # Auto-close, wrapping, smart backspace, indent & cursor engine
├── haptics/
│   └── KeyboardHaptics.kt                 # Tactile View & Vibrator feedback controller
├── ime/
│   ├── CodeKeyboardService.kt             # Android system InputMethodService
│   └── ImeSystemHelper.kt                 # System IME status check & settings/picker launcher
├── model/
│   └── SyntaxLayout.kt                    # Language-specific symbol strips, hints & starter snippets
└── ui/
    ├── KeyboardViewModel.kt               # Sandbox state, undo stack & preference orchestration
    ├── components/
    │   ├── ClipboardHistoryPanel.kt       # Inline clipboard history drawer
    │   ├── CodeKeyboard.kt                # Reusable mechanical programming keyboard
    │   ├── CodeSandboxEditor.kt           # Syntax-highlighted editor with line gutter
    │   ├── RgbHighlightController.kt      # Static, dynamic & gradient RGB press rendering
    │   └── SettingsSheet.kt               # Modal settings studio
    ├── screens/
    │   └── KeyboardWorkbenchScreen.kt     # Adaptive compact/expanded main workspace
    └── theme/                             # Obsidian Dark & Daylight Paper IDE themes
```

---

## Getting Started

### Prerequisites
- Android Studio (or Android SDK Platform 36 & Build Tools)
- JDK 21 (recommended; matches CI workflow)
- Gradle

### Build Debug APK
```bash
gradle assembleDebug
```
The generated APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit & Robolectric UI Tests
```bash
gradle :app:testDebugUnitTest
```

### Enable as System Keyboard on Android
1. Open **KeyForge** and tap **Enable IME** (or open Settings via the tune icon).
2. Tap **1. Enable** to toggle **KeyForge Code Keyboard** on in Android's *On-screen keyboard* settings.
3. Tap **2. Select** to switch your active input method to **KeyForge**.

---

## CI/CD

This repository includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`) that runs on pushes to `main`/`master` and manual `workflow_dispatch` triggers to generate a debug keystore, build `app-debug.apk`, and upload it as a build artifact.
