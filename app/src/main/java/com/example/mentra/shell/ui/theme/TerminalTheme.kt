package com.example.mentra.shell.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ZSH-like terminal color scheme
 * Professional, eye-friendly colors for extended terminal use
 */
object TerminalColors {

    // Background colors
    val Background = Color(0xFF1E1E1E)           // Dark gray (VS Code dark)
    val BackgroundAlt = Color(0xFF252526)         // Slightly lighter
    val Selection = Color(0xFF264F78)             // Blue selection

    // Text colors (base)
    val Text = Color(0xFFD4D4D4)                  // Light gray text
    val TextBright = Color(0xFFFFFFFF)            // White for emphasis
    val TextDim = Color(0xFF808080)               // Dimmed text

    // Prompt colors
    val PromptUser = Color(0xFF4EC9B0)            // Cyan (user@host)
    val PromptPath = Color(0xFF569CD6)            // Blue (current path)
    val PromptSymbol = Color(0xFF6A9955)          // Green ($ or #)
    val PromptGit = Color(0xFFDCDCAA)             // Yellow (git branch)

    // Status colors (semantic)
    val Success = Color(0xFF4EC9B0)               // Cyan/Green
    val Error = Color(0xFFF48771)                 // Soft red
    val Warning = Color(0xFFDCDCAA)               // Yellow
    val Info = Color(0xFF569CD6)                  // Blue

    // Command output colors
    val Command = Color(0xFFCE9178)               // Orange (command text)
    val Argument = Color(0xFF9CDCFE)              // Light blue (arguments)
    val Flag = Color(0xFFD7BA7D)                  // Gold (--flags)
    val String = Color(0xFFCE9178)                // Orange (quoted strings)
    val Number = Color(0xFFB5CEA8)                // Light green (numbers)
    val Comment = Color(0xFF6A9955)               // Green (comments)

    // File type colors (like ls -la)
    val Directory = Color(0xFF569CD6)             // Blue
    val Executable = Color(0xFF4EC9B0)            // Green
    val Symlink = Color(0xFFC586C0)               // Purple
    val Archive = Color(0xFFD7BA7D)               // Gold
    val Image = Color(0xFFDCDCAA)                 // Yellow
    val Audio = Color(0xFFB5CEA8)                 // Light green
    val Video = Color(0xFF9CDCFE)                 // Light blue

    // System operation colors
    val Critical = Color(0xFFF14C4C)              // Bright red (shutdown, reboot)
    val Privileged = Color(0xFFC586C0)            // Purple (root operations)
    val Network = Color(0xFF4FC1FF)               // Bright blue
    val Storage = Color(0xFFDCDCAA)               // Yellow
    val Process = Color(0xFF4EC9B0)               // Cyan

    // Syntax highlighting
    val Keyword = Color(0xFFC586C0)               // Purple
    val Function = Color(0xFFDCDCAA)              // Yellow
    val Variable = Color(0xFF9CDCFE)              // Light blue
    val Operator = Color(0xFFD4D4D4)              // White

    // Special indicators
    val Cursor = Color(0xFFFFFFFF)                // White cursor
    val LineNumber = Color(0xFF858585)            // Gray line numbers
    val Border = Color(0xFF3E3E3E)                // Border color
}

/**
 * Text formatting styles
 */
object TerminalStyles {
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"
    const val ITALIC = "\u001B[3m"
    const val UNDERLINE = "\u001B[4m"
    const val BLINK = "\u001B[5m"
    const val REVERSE = "\u001B[7m"
    const val HIDDEN = "\u001B[8m"
    const val STRIKETHROUGH = "\u001B[9m"
    const val RESET = "\u001B[0m"
}

/**
 * Terminal icons (Unicode)
 */
object TerminalIcons {
    const val SUCCESS = "✓"
    const val ERROR = "✗"
    const val WARNING = "⚠"
    const val INFO = "ℹ"
    const val RUNNING = "●"
    const val ARROW = "→"
    const val PROMPT = "❯"
    const val ROOT = "#"
    const val USER = "$"
    const val GIT_BRANCH = ""
    const val FOLDER = ""
    const val FILE = ""
    const val LOCK = ""
    const val NETWORK = ""
    const val BATTERY = ""
    const val CPU = ""
    const val MEMORY = ""
}

/**
 * Command type classification for color coding
 */
enum class CommandType {
    SYSTEM,          // System operations (color: Info)
    CRITICAL,        // Shutdown, reboot (color: Critical)
    NETWORK,         // WiFi, data (color: Network)
    STORAGE,         // Files, storage (color: Storage)
    APP,             // App operations (color: Process)
    QUERY,           // Info queries (color: Info)
    SETTINGS,        // Settings changes (color: Warning)
    PRIVILEGED,      // Root operations (color: Privileged)
    BUILT_IN,        // Shell built-ins (color: Command)
    UNKNOWN          // Unknown (color: TextDim)
}

/**
 * Output classification for formatting
 */
enum class OutputType {
    SUCCESS,         // Green, with ✓
    ERROR,           // Red, with ✗
    WARNING,         // Yellow, with ⚠
    INFO,            // Blue, with ℹ
    DATA,            // White, plain
    PROMPT,          // Formatted prompt
    COMMAND,         // User command (highlighted)
    SYSTEM           // System message (dimmed)
}

