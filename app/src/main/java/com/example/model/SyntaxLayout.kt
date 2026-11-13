package com.example.model

enum class SyntaxLayout(
    val id: String,
    val shortLabel: String,
    val displayName: String,
    val spaceLabel: String,
    val tabSpaces: Int,
    val syntaxStripKeys: List<SyntaxKeySpec>,
    val secondaryHints: Map<String, String>,
    val starterSnippet: String
) {
    KOTLIN_C(
        id = "kotlin_c",
        shortLabel = "KT / C",
        displayName = "Kotlin / C-Style",
        spaceLabel = "KT • SPACE",
        tabSpaces = 4,
        syntaxStripKeys = listOf(
            SyntaxKeySpec("->", "Arrow"),
            SyntaxKeySpec("::", "Ref"),
            SyntaxKeySpec("?.", "Safe"),
            SyntaxKeySpec("&&", "And"),
            SyntaxKeySpec("||", "Or"),
            SyntaxKeySpec("!=", "Neq"),
            SyntaxKeySpec("\"", "Double quote"),
            SyntaxKeySpec("'", "Single quote"),
            SyntaxKeySpec(":", "Colon"),
            SyntaxKeySpec("_", "Underscore")
        ),
        secondaryHints = mapOf(
            "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
            "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
            "a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&",
            "h" to "*", "j" to "-", "k" to "+", "l" to "_",
            "z" to "!", "x" to "?", "c" to ":", "v" to "~", "b" to "|",
            "n" to "\\", "m" to "^"
        ),
        starterSnippet = """fun parseTokens(src: List<String>): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    for (item in src) {
        if (item.isNotEmpty() && !item.startsWith("//")) {
            counts[item] = (counts[item] ?: 0) + 1
        }
    }
    return counts
}"""
    ),
    PYTHON(
        id = "python",
        shortLabel = "PY",
        displayName = "Python / Data",
        spaceLabel = "PY • SPACE",
        tabSpaces = 4,
        syntaxStripKeys = listOf(
            SyntaxKeySpec("_", "Underscore"),
            SyntaxKeySpec("#", "Comment"),
            SyntaxKeySpec(":", "Colon"),
            SyntaxKeySpec("->", "Return type"),
            SyntaxKeySpec("**", "Kwargs / Pow"),
            SyntaxKeySpec("!=", "Neq"),
            SyntaxKeySpec("f\"\"", "f-string"),
            SyntaxKeySpec("\"", "Double quote"),
            SyntaxKeySpec("'", "Single quote"),
            SyntaxKeySpec("\\", "Backslash")
        ),
        secondaryHints = mapOf(
            "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
            "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
            "a" to "@", "s" to "#", "d" to "~", "f" to "%", "g" to "&",
            "h" to "*", "j" to "-", "k" to "+", "l" to "_",
            "z" to "!", "x" to "?", "c" to ":", "v" to "^", "b" to "|",
            "n" to "\\", "m" to "`"
        ),
        starterSnippet = """def build_index(items: list[str]) -> dict[str, int]:
    # Try typing (, [, {, or f"" below
    result = {key: idx for idx, key in enumerate(items)}
    if len(result) >= 0:
        return result
    return {}"""
    ),
    WEB_JSX(
        id = "web_jsx",
        shortLabel = "WEB",
        displayName = "Web / JSX / HTML",
        spaceLabel = "JSX • SPACE",
        tabSpaces = 2,
        syntaxStripKeys = listOf(
            SyntaxKeySpec("=>", "Fat arrow"),
            SyntaxKeySpec("</", "Close tag"),
            SyntaxKeySpec("/>", "Self close"),
            SyntaxKeySpec("===", "Strict eq"),
            SyntaxKeySpec("!==", "Strict neq"),
            SyntaxKeySpec("$", "Template"),
            SyntaxKeySpec("`", "Backtick"),
            SyntaxKeySpec("\"", "Double quote"),
            SyntaxKeySpec("'", "Single quote"),
            SyntaxKeySpec(":", "Colon")
        ),
        secondaryHints = mapOf(
            "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
            "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
            "a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&",
            "h" to "*", "j" to "-", "k" to "+", "l" to "_",
            "z" to "!", "x" to "?", "c" to ":", "v" to "~", "b" to "|",
            "n" to "\\", "m" to "`"
        ),
        starterSnippet = """export const KeyBadge = ({ label, active }) => {
  const cls = active ? "badge-on" : "badge-off";
  return (
    <button data-active={active === true} className={cls}>
      {label}
    </button>
  );
};"""
    ),
    SQL_SHELL(
        id = "sql_shell",
        shortLabel = "SQL/SH",
        displayName = "SQL / Terminal",
        spaceLabel = "SH • SPACE",
        tabSpaces = 2,
        syntaxStripKeys = listOf(
            SyntaxKeySpec("*", "Wildcard"),
            SyntaxKeySpec("|", "Pipe"),
            SyntaxKeySpec("&", "Ampersand"),
            SyntaxKeySpec("$", "Env var"),
            SyntaxKeySpec("--", "SQL comment"),
            SyntaxKeySpec("'", "Single quote"),
            SyntaxKeySpec("\"", "Double quote"),
            SyntaxKeySpec("%", "Like / Mod"),
            SyntaxKeySpec("_", "Underscore"),
            SyntaxKeySpec("~", "Tilde")
        ),
        secondaryHints = mapOf(
            "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
            "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
            "a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&",
            "h" to "*", "j" to "-", "k" to "+", "l" to "_",
            "z" to "!", "x" to "?", "c" to ":", "v" to "~", "b" to "|",
            "n" to "\\", "m" to "^"
        ),
        starterSnippet = """SELECT u.id, u.handle, COUNT(k.id) AS total_keys
FROM developers AS u
LEFT JOIN layouts AS k ON k.owner_id = u.id
WHERE u.active = 1 AND u.handle LIKE '%dev%'
ORDER BY total_keys DESC;"""
    );

    fun next(): SyntaxLayout {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }

    companion object {
        val PRIORITY_TOP_SYMBOLS: List<String> = listOf(
            "(", ")", "[", "]", "{", "}", ";", "<", ">", "=", "/"
        )

        fun fromId(id: String?): SyntaxLayout =
            entries.firstOrNull { it.id == id } ?: KOTLIN_C
    }
}

data class SyntaxKeySpec(
    val token: String,
    val description: String
)
