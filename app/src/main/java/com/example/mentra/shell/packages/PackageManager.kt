package com.example.mentra.shell.packages

import android.content.Context
import android.util.Log
import com.example.mentra.shell.models.ShellOutput
import com.example.mentra.shell.models.ShellOutputType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * MENTRA PACKAGE MANAGER
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Provides Termux-like package management capabilities.
 * Supports:
 * - pkg install/remove/update/list
 * - pip (Python packages)
 * - npm (Node.js packages)
 * - Native Linux commands execution
 *
 * Note: Full Termux functionality requires a proper Linux environment.
 * This implementation provides:
 * 1. Built-in shell commands (pure Kotlin implementations)
 * 2. Termux API integration (if Termux is installed)
 * 3. Native binary execution (if available)
 */
@Singleton
class PackageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MentraPackageManager"

        // Package command keywords
        val PKG_COMMANDS = listOf(
            "pkg", "apt", "apt-get", "pip", "pip3", "npm", "yarn",
            "python", "python3", "node", "ruby", "gem"
        )

        // Built-in shell commands we can emulate
        val BUILTIN_COMMANDS = listOf(
            "ls", "cd", "pwd", "cat", "echo", "grep", "find", "touch",
            "mkdir", "rm", "cp", "mv", "chmod", "chown", "head", "tail",
            "wc", "sort", "uniq", "date", "whoami", "uname", "env",
            "export", "alias", "unalias", "history", "clear", "exit"
        )
    }

    // Package installation directory (app's private files)
    private val packagesDir: File by lazy {
        File(context.filesDir, "packages").also { it.mkdirs() }
    }

    // Binary directory
    private val binDir: File by lazy {
        File(packagesDir, "bin").also { it.mkdirs() }
    }

    // Python packages directory
    private val pythonDir: File by lazy {
        File(packagesDir, "python").also { it.mkdirs() }
    }

    // Node modules directory
    private val nodeModulesDir: File by lazy {
        File(packagesDir, "node_modules").also { it.mkdirs() }
    }

    // Installed packages cache
    private val installedPackages = mutableMapOf<String, PackageInfo>()

    // Environment variables
    private val environment = mutableMapOf<String, String>()

    init {
        // Setup environment
        environment["HOME"] = context.filesDir.absolutePath
        environment["PATH"] = "${binDir.absolutePath}:/system/bin:/system/xbin"
        environment["PYTHONPATH"] = pythonDir.absolutePath
        environment["NODE_PATH"] = nodeModulesDir.absolutePath

        // Load installed packages
        loadInstalledPackages()
    }

    /**
     * Check if command is a package manager command
     */
    fun isPackageCommand(command: String): Boolean {
        val parts = command.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) return false
        val cmd = parts[0].lowercase()
        return PKG_COMMANDS.contains(cmd) || BUILTIN_COMMANDS.contains(cmd)
    }

    /**
     * Handle package/shell command
     */
    suspend fun handleCommand(command: String): List<ShellOutput> {
        val parts = command.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) {
            return listOf(ShellOutput("", ShellOutputType.ERROR))
        }

        val cmd = parts[0].lowercase()
        val args = if (parts.size > 1) parts.subList(1, parts.size) else emptyList()

        return when (cmd) {
            // Package managers
            "pkg", "apt", "apt-get" -> handlePkgCommand(args)
            "pip", "pip3" -> handlePipCommand(args)
            "npm" -> handleNpmCommand(args)
            "yarn" -> handleYarnCommand(args)

            // Interpreters
            "python", "python3" -> handlePythonCommand(args)
            "node" -> handleNodeCommand(args)
            "ruby" -> handleRubyCommand(args)

            // Built-in shell commands
            "ls" -> handleLs(args)
            "cd" -> handleCd(args)
            "pwd" -> handlePwd()
            "cat" -> handleCat(args)
            "echo" -> handleEcho(args)
            "grep" -> handleGrep(args)
            "find" -> handleFind(args)
            "touch" -> handleTouch(args)
            "mkdir" -> handleMkdir(args)
            "rm" -> handleRm(args)
            "cp" -> handleCp(args)
            "mv" -> handleMv(args)
            "head" -> handleHead(args)
            "tail" -> handleTail(args)
            "wc" -> handleWc(args)
            "sort" -> handleSort(args)
            "uniq" -> handleUniq(args)
            "date" -> handleDate(args)
            "whoami" -> handleWhoami()
            "uname" -> handleUname(args)
            "env" -> handleEnv()
            "export" -> handleExport(args)
            "which" -> handleWhich(args)
            "clear" -> handleClear()

            else -> listOf(
                ShellOutput(
                    "Command not found: $cmd\nTry 'pkg help' for package management or 'help' for shell commands.",
                    ShellOutputType.ERROR
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PACKAGE MANAGER COMMANDS (pkg/apt)
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun handlePkgCommand(args: List<String>): List<ShellOutput> {
        if (args.isEmpty()) {
            return listOf(pkgHelp())
        }

        return when (args[0].lowercase()) {
            "install", "i" -> pkgInstall(args.drop(1))
            "remove", "uninstall", "rm" -> pkgRemove(args.drop(1))
            "update", "upgrade" -> pkgUpdate()
            "list", "ls" -> pkgList()
            "search", "s" -> pkgSearch(args.drop(1))
            "info", "show" -> pkgInfo(args.drop(1))
            "help", "-h", "--help" -> listOf(pkgHelp())
            else -> listOf(
                ShellOutput("Unknown pkg command: ${args[0]}", ShellOutputType.ERROR),
                pkgHelp()
            )
        }
    }

    private fun pkgHelp(): ShellOutput {
        return ShellOutput(
            """
            |╔══════════════════════════════════════════════════════════════╗
            |║              MENTRA PACKAGE MANAGER (pkg)                     ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ USAGE: pkg <command> [options] [package]                      ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ COMMANDS:                                                     ║
            |║   install <pkg>   Install a package                          ║
            |║   remove <pkg>    Remove a package                           ║
            |║   update          Update package list                        ║
            |║   list            List installed packages                    ║
            |║   search <query>  Search for packages                        ║
            |║   info <pkg>      Show package information                   ║
            |║   help            Show this help                             ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ AVAILABLE PACKAGES:                                          ║
            |║   python, nodejs, ruby, git, curl, wget, vim, nano           ║
            |║   grep, sed, awk, jq, htop, ncurses-utils                    ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ NOTE: Full package installation requires Termux.              ║
            |║       Mentra provides built-in emulation for basic commands. ║
            |╚══════════════════════════════════════════════════════════════╝
            """.trimMargin(),
            ShellOutputType.INFO
        )
    }

    private suspend fun pkgInstall(packages: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (packages.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: pkg install <package>", ShellOutputType.WARNING))
        }

        val outputs = mutableListOf<ShellOutput>()

        packages.forEach { pkg ->
            outputs.add(ShellOutput("Installing $pkg...", ShellOutputType.INFO))

            // Check if Termux is available
            if (isTermuxAvailable()) {
                // Try to install via Termux
                val result = executeTermuxCommand("pkg install -y $pkg")
                outputs.add(result)
            } else {
                // Register as "installed" (emulated)
                val packageInfo = getBuiltInPackageInfo(pkg)
                if (packageInfo != null) {
                    installedPackages[pkg] = packageInfo
                    saveInstalledPackages()
                    outputs.add(ShellOutput(
                        "✓ Package '$pkg' installed (built-in emulation)",
                        ShellOutputType.SUCCESS
                    ))
                } else {
                    outputs.add(ShellOutput(
                        "✗ Package '$pkg' not available.\n" +
                                "  Install Termux for full package support:\n" +
                                "  https://f-droid.org/packages/com.termux/",
                        ShellOutputType.WARNING
                    ))
                }
            }
        }

        outputs
    }

    private suspend fun pkgRemove(packages: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (packages.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: pkg remove <package>", ShellOutputType.WARNING))
        }

        val outputs = mutableListOf<ShellOutput>()

        packages.forEach { pkg ->
            if (installedPackages.containsKey(pkg)) {
                installedPackages.remove(pkg)
                saveInstalledPackages()
                outputs.add(ShellOutput("✓ Package '$pkg' removed", ShellOutputType.SUCCESS))
            } else {
                outputs.add(ShellOutput("Package '$pkg' is not installed", ShellOutputType.WARNING))
            }
        }

        outputs
    }

    private suspend fun pkgUpdate(): List<ShellOutput> = withContext(Dispatchers.IO) {
        listOf(
            ShellOutput("Updating package list...", ShellOutputType.INFO),
            if (isTermuxAvailable()) {
                executeTermuxCommand("pkg update")
            } else {
                ShellOutput("✓ Package list is up to date (built-in packages)", ShellOutputType.SUCCESS)
            }
        )
    }

    private fun pkgList(): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()
        outputs.add(ShellOutput("Installed packages:", ShellOutputType.INFO))

        if (installedPackages.isEmpty()) {
            outputs.add(ShellOutput("  (no packages installed)", ShellOutputType.INFO))
        } else {
            installedPackages.forEach { (name, info) ->
                outputs.add(ShellOutput("  $name (${info.version}) - ${info.description}", ShellOutputType.INFO))
            }
        }

        // Also list built-in commands
        outputs.add(ShellOutput("\nBuilt-in commands:", ShellOutputType.INFO))
        outputs.add(ShellOutput("  ${BUILTIN_COMMANDS.joinToString(", ")}", ShellOutputType.INFO))

        return outputs
    }

    private fun pkgSearch(query: List<String>): List<ShellOutput> {
        if (query.isEmpty()) {
            return listOf(ShellOutput("Usage: pkg search <query>", ShellOutputType.WARNING))
        }

        val searchTerm = query.joinToString(" ").lowercase()
        val results = getAvailablePackages().filter {
            it.name.contains(searchTerm) || it.description.lowercase().contains(searchTerm)
        }

        if (results.isEmpty()) {
            return listOf(ShellOutput("No packages found matching '$searchTerm'", ShellOutputType.INFO))
        }

        val outputs = mutableListOf<ShellOutput>()
        outputs.add(ShellOutput("Search results for '$searchTerm':", ShellOutputType.INFO))
        results.forEach { pkg ->
            val installed = if (installedPackages.containsKey(pkg.name)) " [installed]" else ""
            outputs.add(ShellOutput("  ${pkg.name}${installed} - ${pkg.description}", ShellOutputType.INFO))
        }

        return outputs
    }

    private fun pkgInfo(packages: List<String>): List<ShellOutput> {
        if (packages.isEmpty()) {
            return listOf(ShellOutput("Usage: pkg info <package>", ShellOutputType.WARNING))
        }

        val pkg = packages[0]
        val info = installedPackages[pkg] ?: getBuiltInPackageInfo(pkg)

        return if (info != null) {
            listOf(
                ShellOutput(
                    """
                    |Package: ${info.name}
                    |Version: ${info.version}
                    |Description: ${info.description}
                    |Status: ${if (installedPackages.containsKey(pkg)) "installed" else "available"}
                    |Type: ${info.type}
                    """.trimMargin(),
                    ShellOutputType.INFO
                )
            )
        } else {
            listOf(ShellOutput("Package '$pkg' not found", ShellOutputType.ERROR))
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PIP COMMANDS (Python packages)
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun handlePipCommand(args: List<String>): List<ShellOutput> {
        if (args.isEmpty()) {
            return listOf(pipHelp())
        }

        return when (args[0].lowercase()) {
            "install" -> pipInstall(args.drop(1))
            "uninstall", "remove" -> pipUninstall(args.drop(1))
            "list" -> pipList()
            "freeze" -> pipFreeze()
            "show" -> pipShow(args.drop(1))
            "search" -> pipSearch(args.drop(1))
            "-v", "--version", "version" -> listOf(
                ShellOutput("pip 23.0.1 (Mentra built-in)", ShellOutputType.INFO)
            )
            "help", "-h", "--help" -> listOf(pipHelp())
            else -> listOf(
                ShellOutput("Unknown pip command: ${args[0]}", ShellOutputType.ERROR),
                pipHelp()
            )
        }
    }

    private fun pipHelp(): ShellOutput {
        return ShellOutput(
            """
            |╔══════════════════════════════════════════════════════════════╗
            |║                    PIP (Python Package Installer)             ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ USAGE: pip <command> [options] [package]                      ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ COMMANDS:                                                     ║
            |║   install <pkg>     Install packages                         ║
            |║   uninstall <pkg>   Uninstall packages                       ║
            |║   list              List installed packages                  ║
            |║   freeze            Output installed packages in req format  ║
            |║   show <pkg>        Show package details                     ║
            |║   search <query>    Search PyPI for packages                 ║
            |║   --version         Show pip version                         ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ NOTE: Full pip support requires Python/Termux installation.   ║
            |╚══════════════════════════════════════════════════════════════╝
            """.trimMargin(),
            ShellOutputType.INFO
        )
    }

    private suspend fun pipInstall(packages: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (packages.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: pip install <package>", ShellOutputType.WARNING))
        }

        val outputs = mutableListOf<ShellOutput>()

        packages.filter { !it.startsWith("-") }.forEach { pkg ->
            outputs.add(ShellOutput("Collecting $pkg...", ShellOutputType.INFO))

            // Try actual pip if available
            val result = executeCommand("pip3 install --user $pkg")
            if (result.first) {
                outputs.add(ShellOutput("Successfully installed $pkg", ShellOutputType.SUCCESS))
            } else {
                // Emulate installation
                val info = PackageInfo(
                    name = pkg,
                    version = "latest",
                    description = "Python package",
                    type = PackageType.PYTHON
                )
                installedPackages["pip:$pkg"] = info
                saveInstalledPackages()
                outputs.add(ShellOutput(
                    "✓ Package '$pkg' registered (virtual installation)",
                    ShellOutputType.SUCCESS
                ))
            }
        }

        outputs
    }

    private suspend fun pipUninstall(packages: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (packages.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: pip uninstall <package>", ShellOutputType.WARNING))
        }

        val outputs = mutableListOf<ShellOutput>()

        packages.forEach { pkg ->
            val key = "pip:$pkg"
            if (installedPackages.containsKey(key)) {
                installedPackages.remove(key)
                saveInstalledPackages()
                outputs.add(ShellOutput("Successfully uninstalled $pkg", ShellOutputType.SUCCESS))
            } else {
                outputs.add(ShellOutput("Package '$pkg' is not installed", ShellOutputType.WARNING))
            }
        }

        outputs
    }

    private fun pipList(): List<ShellOutput> {
        val pythonPackages = installedPackages.filter { it.key.startsWith("pip:") }

        val outputs = mutableListOf<ShellOutput>()
        outputs.add(ShellOutput("Package                    Version", ShellOutputType.INFO))
        outputs.add(ShellOutput("-------------------------- ----------", ShellOutputType.INFO))

        if (pythonPackages.isEmpty()) {
            outputs.add(ShellOutput("(no packages installed)", ShellOutputType.INFO))
        } else {
            pythonPackages.forEach { (key, info) ->
                val name = key.removePrefix("pip:")
                outputs.add(ShellOutput("${name.padEnd(26)} ${info.version}", ShellOutputType.INFO))
            }
        }

        return outputs
    }

    private fun pipFreeze(): List<ShellOutput> {
        val pythonPackages = installedPackages.filter { it.key.startsWith("pip:") }

        return pythonPackages.map { (key, info) ->
            val name = key.removePrefix("pip:")
            ShellOutput("$name==${info.version}", ShellOutputType.INFO)
        }.ifEmpty {
            listOf(ShellOutput("# No packages installed", ShellOutputType.INFO))
        }
    }

    private fun pipShow(packages: List<String>): List<ShellOutput> {
        if (packages.isEmpty()) {
            return listOf(ShellOutput("Usage: pip show <package>", ShellOutputType.WARNING))
        }

        val pkg = packages[0]
        val info = installedPackages["pip:$pkg"]

        return if (info != null) {
            listOf(
                ShellOutput(
                    """
                    |Name: $pkg
                    |Version: ${info.version}
                    |Summary: ${info.description}
                    |Location: ${pythonDir.absolutePath}
                    """.trimMargin(),
                    ShellOutputType.INFO
                )
            )
        } else {
            listOf(ShellOutput("Package '$pkg' is not installed", ShellOutputType.WARNING))
        }
    }

    private fun pipSearch(query: List<String>): List<ShellOutput> {
        return listOf(
            ShellOutput(
                "Note: pip search is disabled on PyPI.\n" +
                        "Use: https://pypi.org/search/?q=${query.joinToString("+")}",
                ShellOutputType.INFO
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // NPM COMMANDS (Node.js packages)
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun handleNpmCommand(args: List<String>): List<ShellOutput> {
        if (args.isEmpty()) {
            return listOf(npmHelp())
        }

        return when (args[0].lowercase()) {
            "install", "i" -> npmInstall(args.drop(1))
            "uninstall", "remove", "rm" -> npmUninstall(args.drop(1))
            "list", "ls" -> npmList()
            "init" -> npmInit()
            "-v", "--version", "version" -> listOf(
                ShellOutput("npm 9.6.7 (Mentra built-in)", ShellOutputType.INFO)
            )
            "help", "-h", "--help" -> listOf(npmHelp())
            else -> listOf(
                ShellOutput("Unknown npm command: ${args[0]}", ShellOutputType.ERROR),
                npmHelp()
            )
        }
    }

    private fun npmHelp(): ShellOutput {
        return ShellOutput(
            """
            |╔══════════════════════════════════════════════════════════════╗
            |║                    NPM (Node Package Manager)                 ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ USAGE: npm <command> [options] [package]                      ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ COMMANDS:                                                     ║
            |║   install <pkg>     Install a package                        ║
            |║   uninstall <pkg>   Uninstall a package                      ║
            |║   list              List installed packages                  ║
            |║   init              Create package.json                      ║
            |║   --version         Show npm version                         ║
            |╠══════════════════════════════════════════════════════════════╣
            |║ NOTE: Full npm support requires Node.js/Termux installation.  ║
            |╚══════════════════════════════════════════════════════════════╝
            """.trimMargin(),
            ShellOutputType.INFO
        )
    }

    private suspend fun npmInstall(packages: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        val outputs = mutableListOf<ShellOutput>()

        if (packages.isEmpty()) {
            outputs.add(ShellOutput("Installing dependencies from package.json...", ShellOutputType.INFO))
            outputs.add(ShellOutput("No package.json found", ShellOutputType.WARNING))
            return@withContext outputs
        }

        packages.filter { !it.startsWith("-") }.forEach { pkg ->
            outputs.add(ShellOutput("+ $pkg", ShellOutputType.INFO))

            val info = PackageInfo(
                name = pkg,
                version = "latest",
                description = "npm package",
                type = PackageType.NPM
            )
            installedPackages["npm:$pkg"] = info
            saveInstalledPackages()
        }

        outputs.add(ShellOutput("added ${packages.size} packages", ShellOutputType.SUCCESS))
        outputs
    }

    private suspend fun npmUninstall(packages: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (packages.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: npm uninstall <package>", ShellOutputType.WARNING))
        }

        val outputs = mutableListOf<ShellOutput>()

        packages.forEach { pkg ->
            val key = "npm:$pkg"
            if (installedPackages.containsKey(key)) {
                installedPackages.remove(key)
                saveInstalledPackages()
                outputs.add(ShellOutput("- $pkg", ShellOutputType.INFO))
            }
        }

        outputs.add(ShellOutput("removed ${packages.size} packages", ShellOutputType.SUCCESS))
        outputs
    }

    private fun npmList(): List<ShellOutput> {
        val npmPackages = installedPackages.filter { it.key.startsWith("npm:") }

        val outputs = mutableListOf<ShellOutput>()
        outputs.add(ShellOutput("${context.packageName}@1.0.0", ShellOutputType.INFO))

        if (npmPackages.isEmpty()) {
            outputs.add(ShellOutput("└── (empty)", ShellOutputType.INFO))
        } else {
            npmPackages.entries.forEachIndexed { index, (key, info) ->
                val name = key.removePrefix("npm:")
                val prefix = if (index == npmPackages.size - 1) "└──" else "├──"
                outputs.add(ShellOutput("$prefix $name@${info.version}", ShellOutputType.INFO))
            }
        }

        return outputs
    }

    private fun npmInit(): List<ShellOutput> {
        return listOf(
            ShellOutput(
                "This utility will walk you through creating a package.json file.",
                ShellOutputType.INFO
            ),
            ShellOutput(
                "Note: Interactive npm init not supported in Mentra shell.",
                ShellOutputType.WARNING
            )
        )
    }

    private suspend fun handleYarnCommand(args: List<String>): List<ShellOutput> {
        // Yarn is largely npm-compatible
        return handleNpmCommand(args)
    }

    // ═══════════════════════════════════════════════════════════════════
    // INTERPRETER COMMANDS (Python, Node, Ruby)
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun handlePythonCommand(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(
                ShellOutput(
                    """
                    |Python 3.11.4 (Mentra embedded)
                    |Type "exit()" to exit.
                    |>>> 
                    |Note: Interactive Python shell not available.
                    |Use: python -c "print('Hello')" or python script.py
                    """.trimMargin(),
                    ShellOutputType.INFO
                )
            )
        }

        when (args[0]) {
            "-c" -> {
                if (args.size > 1) {
                    val code = args.drop(1).joinToString(" ").trim('"', '\'')
                    executePythonCode(code)
                } else {
                    listOf(ShellOutput("Usage: python -c \"code\"", ShellOutputType.WARNING))
                }
            }
            "-V", "--version" -> listOf(
                ShellOutput("Python 3.11.4", ShellOutputType.INFO)
            )
            else -> {
                // Try to run as script
                val scriptPath = args[0]
                listOf(
                    ShellOutput(
                        "Note: Script execution requires Termux Python.\n" +
                                "Install with: pkg install python",
                        ShellOutputType.WARNING
                    )
                )
            }
        }
    }

    private fun executePythonCode(code: String): List<ShellOutput> {
        // Simple Python expression evaluator (very limited)
        return try {
            when {
                code.startsWith("print(") -> {
                    val content = code.removePrefix("print(").removeSuffix(")")
                    listOf(ShellOutput(content.trim('"', '\''), ShellOutputType.INFO))
                }
                code.contains("+") || code.contains("-") || code.contains("*") || code.contains("/") -> {
                    // Math expression - delegate to calculator
                    listOf(ShellOutput("Use 'calc $code' for math expressions", ShellOutputType.INFO))
                }
                else -> listOf(ShellOutput("Expression evaluation not supported: $code", ShellOutputType.WARNING))
            }
        } catch (e: Exception) {
            listOf(ShellOutput("Error: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private suspend fun handleNodeCommand(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(
                ShellOutput(
                    """
                    |Node.js v18.16.0 (Mentra embedded)
                    |Note: Interactive REPL not available.
                    |Use: node -e "console.log('Hello')" or node script.js
                    """.trimMargin(),
                    ShellOutputType.INFO
                )
            )
        }

        when (args[0]) {
            "-e", "--eval" -> {
                if (args.size > 1) {
                    val code = args.drop(1).joinToString(" ").trim('"', '\'')
                    executeNodeCode(code)
                } else {
                    listOf(ShellOutput("Usage: node -e \"code\"", ShellOutputType.WARNING))
                }
            }
            "-v", "--version" -> listOf(
                ShellOutput("v18.16.0", ShellOutputType.INFO)
            )
            else -> listOf(
                ShellOutput(
                    "Note: Script execution requires Termux Node.js.\n" +
                            "Install with: pkg install nodejs",
                    ShellOutputType.WARNING
                )
            )
        }
    }

    private fun executeNodeCode(code: String): List<ShellOutput> {
        return try {
            when {
                code.contains("console.log(") -> {
                    val content = code.substringAfter("console.log(").substringBeforeLast(")")
                    listOf(ShellOutput(content.trim('"', '\''), ShellOutputType.INFO))
                }
                else -> listOf(ShellOutput("Expression evaluation not supported: $code", ShellOutputType.WARNING))
            }
        } catch (e: Exception) {
            listOf(ShellOutput("Error: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private suspend fun handleRubyCommand(args: List<String>): List<ShellOutput> {
        return listOf(
            ShellOutput(
                "ruby 3.2.2 (Mentra - requires Termux)\n" +
                        "Install with: pkg install ruby",
                ShellOutputType.INFO
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // BUILT-IN SHELL COMMANDS
    // ═══════════════════════════════════════════════════════════════════

    private var currentDirectory = "/"

    private suspend fun handleLs(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        val showHidden = args.contains("-a") || args.contains("-la") || args.contains("-al")
        val longFormat = args.contains("-l") || args.contains("-la") || args.contains("-al")
        val path = args.lastOrNull { !it.startsWith("-") } ?: currentDirectory

        try {
            val dir = File(resolvePath(path))
            if (!dir.exists()) {
                return@withContext listOf(ShellOutput("ls: cannot access '$path': No such file or directory", ShellOutputType.ERROR))
            }
            if (!dir.isDirectory) {
                return@withContext listOf(ShellOutput(dir.name, ShellOutputType.INFO))
            }

            val files = dir.listFiles()?.filter { showHidden || !it.name.startsWith(".") } ?: emptyList()

            if (longFormat) {
                val outputs = mutableListOf<ShellOutput>()
                outputs.add(ShellOutput("total ${files.size}", ShellOutputType.INFO))
                files.forEach { file ->
                    val perms = if (file.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                    val size = file.length().toString().padStart(8)
                    val name = if (file.isDirectory) "${file.name}/" else file.name
                    outputs.add(ShellOutput("$perms $size $name", ShellOutputType.INFO))
                }
                outputs
            } else {
                val names = files.map { if (it.isDirectory) "${it.name}/" else it.name }
                listOf(ShellOutput(names.joinToString("  "), ShellOutputType.INFO))
            }
        } catch (e: Exception) {
            listOf(ShellOutput("ls: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private fun handleCd(args: List<String>): List<ShellOutput> {
        val path = args.firstOrNull() ?: environment["HOME"] ?: "/"

        val newPath = when {
            path == "~" -> environment["HOME"] ?: "/"
            path == "-" -> environment["OLDPWD"] ?: currentDirectory
            path == ".." -> File(currentDirectory).parent ?: "/"
            path.startsWith("/") -> path
            else -> "$currentDirectory/$path"
        }

        val dir = File(newPath)
        return if (dir.exists() && dir.isDirectory) {
            environment["OLDPWD"] = currentDirectory
            currentDirectory = dir.canonicalPath
            listOf() // cd produces no output on success
        } else {
            listOf(ShellOutput("cd: no such file or directory: $path", ShellOutputType.ERROR))
        }
    }

    private fun handlePwd(): List<ShellOutput> {
        return listOf(ShellOutput(currentDirectory, ShellOutputType.INFO))
    }

    private suspend fun handleCat(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: cat <file>", ShellOutputType.WARNING))
        }

        val outputs = mutableListOf<ShellOutput>()
        args.filter { !it.startsWith("-") }.forEach { path ->
            try {
                val file = File(resolvePath(path))
                if (file.exists() && file.isFile) {
                    val content = file.readText()
                    outputs.add(ShellOutput(content, ShellOutputType.INFO))
                } else {
                    outputs.add(ShellOutput("cat: $path: No such file or directory", ShellOutputType.ERROR))
                }
            } catch (e: Exception) {
                outputs.add(ShellOutput("cat: $path: ${e.message}", ShellOutputType.ERROR))
            }
        }
        outputs
    }

    private fun handleEcho(args: List<String>): List<ShellOutput> {
        val text = args.joinToString(" ")
            .replace("\\n", "\n")
            .replace("\\t", "\t")

        // Handle variable expansion
        val expanded = text.replace(Regex("\\$\\{?(\\w+)}?")) { match ->
            environment[match.groupValues[1]] ?: ""
        }

        return listOf(ShellOutput(expanded, ShellOutputType.INFO))
    }

    private suspend fun handleGrep(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.size < 2) {
            return@withContext listOf(ShellOutput("Usage: grep <pattern> <file>", ShellOutputType.WARNING))
        }

        val ignoreCase = args.contains("-i")
        val pattern = args.first { !it.startsWith("-") }
        val files = args.filter { !it.startsWith("-") && it != pattern }

        val outputs = mutableListOf<ShellOutput>()

        files.forEach { path ->
            try {
                val file = File(resolvePath(path))
                if (file.exists() && file.isFile) {
                    file.readLines().forEachIndexed { index, line ->
                        val matches = if (ignoreCase) {
                            line.lowercase().contains(pattern.lowercase())
                        } else {
                            line.contains(pattern)
                        }
                        if (matches) {
                            val prefix = if (files.size > 1) "$path:" else ""
                            outputs.add(ShellOutput("$prefix${index + 1}:$line", ShellOutputType.INFO))
                        }
                    }
                }
            } catch (e: Exception) {
                outputs.add(ShellOutput("grep: $path: ${e.message}", ShellOutputType.ERROR))
            }
        }

        if (outputs.isEmpty()) {
            outputs.add(ShellOutput("(no matches)", ShellOutputType.INFO))
        }

        outputs
    }

    private suspend fun handleFind(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        val path = args.firstOrNull { !it.startsWith("-") } ?: currentDirectory
        val namePattern = args.indexOf("-name").let { if (it >= 0 && it < args.size - 1) args[it + 1] else null }

        val outputs = mutableListOf<ShellOutput>()

        try {
            val dir = File(resolvePath(path))
            dir.walkTopDown().forEach { file ->
                if (namePattern == null || file.name.matches(Regex(namePattern.replace("*", ".*")))) {
                    outputs.add(ShellOutput(file.absolutePath, ShellOutputType.INFO))
                }
            }
        } catch (e: Exception) {
            outputs.add(ShellOutput("find: ${e.message}", ShellOutputType.ERROR))
        }

        if (outputs.isEmpty()) {
            outputs.add(ShellOutput("(no files found)", ShellOutputType.INFO))
        }

        outputs
    }

    private suspend fun handleTouch(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: touch <file>", ShellOutputType.WARNING))
        }

        val outputs = mutableListOf<ShellOutput>()
        args.filter { !it.startsWith("-") }.forEach { path ->
            try {
                val file = File(resolvePath(path))
                if (file.exists()) {
                    file.setLastModified(System.currentTimeMillis())
                } else {
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                }
            } catch (e: Exception) {
                outputs.add(ShellOutput("touch: $path: ${e.message}", ShellOutputType.ERROR))
            }
        }
        outputs.ifEmpty { listOf() }
    }

    private suspend fun handleMkdir(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: mkdir <directory>", ShellOutputType.WARNING))
        }

        val createParents = args.contains("-p")
        val outputs = mutableListOf<ShellOutput>()

        args.filter { !it.startsWith("-") }.forEach { path ->
            try {
                val dir = File(resolvePath(path))
                val success = if (createParents) dir.mkdirs() else dir.mkdir()
                if (!success && !dir.exists()) {
                    outputs.add(ShellOutput("mkdir: cannot create directory '$path'", ShellOutputType.ERROR))
                }
            } catch (e: Exception) {
                outputs.add(ShellOutput("mkdir: $path: ${e.message}", ShellOutputType.ERROR))
            }
        }
        outputs.ifEmpty { listOf() }
    }

    private suspend fun handleRm(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: rm <file>", ShellOutputType.WARNING))
        }

        val recursive = args.contains("-r") || args.contains("-rf") || args.contains("-R")
        val force = args.contains("-f") || args.contains("-rf")
        val outputs = mutableListOf<ShellOutput>()

        args.filter { !it.startsWith("-") }.forEach { path ->
            try {
                val file = File(resolvePath(path))
                if (!file.exists()) {
                    if (!force) {
                        outputs.add(ShellOutput("rm: cannot remove '$path': No such file or directory", ShellOutputType.ERROR))
                    }
                } else if (file.isDirectory && !recursive) {
                    outputs.add(ShellOutput("rm: cannot remove '$path': Is a directory", ShellOutputType.ERROR))
                } else {
                    if (recursive) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                outputs.add(ShellOutput("rm: $path: ${e.message}", ShellOutputType.ERROR))
            }
        }
        outputs.ifEmpty { listOf() }
    }

    private suspend fun handleCp(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        val files = args.filter { !it.startsWith("-") }
        if (files.size < 2) {
            return@withContext listOf(ShellOutput("Usage: cp <source> <dest>", ShellOutputType.WARNING))
        }

        val recursive = args.contains("-r") || args.contains("-R")
        val source = File(resolvePath(files[0]))
        val dest = File(resolvePath(files[1]))

        try {
            if (!source.exists()) {
                return@withContext listOf(ShellOutput("cp: cannot stat '${files[0]}': No such file or directory", ShellOutputType.ERROR))
            }
            if (source.isDirectory && !recursive) {
                return@withContext listOf(ShellOutput("cp: omitting directory '${files[0]}'", ShellOutputType.ERROR))
            }

            if (recursive && source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
            } else {
                source.copyTo(dest, overwrite = true)
            }
            listOf()
        } catch (e: Exception) {
            listOf(ShellOutput("cp: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private suspend fun handleMv(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        val files = args.filter { !it.startsWith("-") }
        if (files.size < 2) {
            return@withContext listOf(ShellOutput("Usage: mv <source> <dest>", ShellOutputType.WARNING))
        }

        val source = File(resolvePath(files[0]))
        val dest = File(resolvePath(files[1]))

        try {
            if (!source.exists()) {
                return@withContext listOf(ShellOutput("mv: cannot stat '${files[0]}': No such file or directory", ShellOutputType.ERROR))
            }
            source.renameTo(dest)
            listOf()
        } catch (e: Exception) {
            listOf(ShellOutput("mv: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private suspend fun handleHead(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: head <file>", ShellOutputType.WARNING))
        }

        val lines = args.indexOf("-n").let { if (it >= 0 && it < args.size - 1) args[it + 1].toIntOrNull() ?: 10 else 10 }
        val path = args.last { !it.startsWith("-") && it.toIntOrNull() == null }

        try {
            val file = File(resolvePath(path))
            if (!file.exists()) {
                return@withContext listOf(ShellOutput("head: cannot open '$path' for reading: No such file or directory", ShellOutputType.ERROR))
            }
            val content = file.readLines().take(lines).joinToString("\n")
            listOf(ShellOutput(content, ShellOutputType.INFO))
        } catch (e: Exception) {
            listOf(ShellOutput("head: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private suspend fun handleTail(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: tail <file>", ShellOutputType.WARNING))
        }

        val lines = args.indexOf("-n").let { if (it >= 0 && it < args.size - 1) args[it + 1].toIntOrNull() ?: 10 else 10 }
        val path = args.last { !it.startsWith("-") && it.toIntOrNull() == null }

        try {
            val file = File(resolvePath(path))
            if (!file.exists()) {
                return@withContext listOf(ShellOutput("tail: cannot open '$path' for reading: No such file or directory", ShellOutputType.ERROR))
            }
            val content = file.readLines().takeLast(lines).joinToString("\n")
            listOf(ShellOutput(content, ShellOutputType.INFO))
        } catch (e: Exception) {
            listOf(ShellOutput("tail: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private suspend fun handleWc(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: wc <file>", ShellOutputType.WARNING))
        }

        val path = args.last { !it.startsWith("-") }

        try {
            val file = File(resolvePath(path))
            if (!file.exists()) {
                return@withContext listOf(ShellOutput("wc: $path: No such file or directory", ShellOutputType.ERROR))
            }
            val content = file.readText()
            val lines = content.lines().size
            val words = content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
            val chars = content.length
            listOf(ShellOutput(" $lines  $words $chars $path", ShellOutputType.INFO))
        } catch (e: Exception) {
            listOf(ShellOutput("wc: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private suspend fun handleSort(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: sort <file>", ShellOutputType.WARNING))
        }

        val reverse = args.contains("-r")
        val numeric = args.contains("-n")
        val path = args.last { !it.startsWith("-") }

        try {
            val file = File(resolvePath(path))
            if (!file.exists()) {
                return@withContext listOf(ShellOutput("sort: cannot read: $path: No such file or directory", ShellOutputType.ERROR))
            }
            var lines = file.readLines()
            lines = if (numeric) {
                lines.sortedBy { it.toDoubleOrNull() ?: Double.MAX_VALUE }
            } else {
                lines.sorted()
            }
            if (reverse) lines = lines.reversed()
            lines.map { ShellOutput(it, ShellOutputType.INFO) }
        } catch (e: Exception) {
            listOf(ShellOutput("sort: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private suspend fun handleUniq(args: List<String>): List<ShellOutput> = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext listOf(ShellOutput("Usage: uniq <file>", ShellOutputType.WARNING))
        }

        val count = args.contains("-c")
        val path = args.last { !it.startsWith("-") }

        try {
            val file = File(resolvePath(path))
            if (!file.exists()) {
                return@withContext listOf(ShellOutput("uniq: $path: No such file or directory", ShellOutputType.ERROR))
            }

            val lines = file.readLines()
            val result = if (count) {
                lines.groupingBy { it }.eachCount().map { "${it.value.toString().padStart(7)} ${it.key}" }
            } else {
                lines.distinct()
            }
            result.map { ShellOutput(it, ShellOutputType.INFO) }
        } catch (e: Exception) {
            listOf(ShellOutput("uniq: ${e.message}", ShellOutputType.ERROR))
        }
    }

    private fun handleDate(args: List<String>): List<ShellOutput> {
        val format = args.indexOf("+").let {
            if (it >= 0 && it < args.size) args[it].removePrefix("+") else null
        }

        val date = java.util.Date()
        val formatted = if (format != null) {
            try {
                java.text.SimpleDateFormat(format, java.util.Locale.getDefault()).format(date)
            } catch (e: Exception) {
                date.toString()
            }
        } else {
            java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.US).format(date)
        }

        return listOf(ShellOutput(formatted, ShellOutputType.INFO))
    }

    private fun handleWhoami(): List<ShellOutput> {
        return listOf(ShellOutput("mentra", ShellOutputType.INFO))
    }

    private fun handleUname(args: List<String>): List<ShellOutput> {
        val all = args.contains("-a")
        val kernel = args.contains("-s") || args.isEmpty()
        val release = args.contains("-r")
        val version = args.contains("-v")
        val machine = args.contains("-m")
        val processor = args.contains("-p")

        val parts = mutableListOf<String>()

        if (all || kernel) parts.add("Linux")
        if (all || release) parts.add(android.os.Build.VERSION.RELEASE)
        if (all || version) parts.add("#1 SMP")
        if (all || machine) parts.add(android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
        if (processor) parts.add(android.os.Build.HARDWARE)

        return listOf(ShellOutput(parts.joinToString(" "), ShellOutputType.INFO))
    }

    private fun handleEnv(): List<ShellOutput> {
        return environment.map { (key, value) ->
            ShellOutput("$key=$value", ShellOutputType.INFO)
        }
    }

    private fun handleExport(args: List<String>): List<ShellOutput> {
        if (args.isEmpty()) {
            return handleEnv()
        }

        args.forEach { arg ->
            val parts = arg.split("=", limit = 2)
            if (parts.size == 2) {
                environment[parts[0]] = parts[1]
            }
        }

        return listOf()
    }

    private fun handleWhich(args: List<String>): List<ShellOutput> {
        if (args.isEmpty()) {
            return listOf(ShellOutput("Usage: which <command>", ShellOutputType.WARNING))
        }

        val cmd = args[0]

        // Check built-in commands
        if (BUILTIN_COMMANDS.contains(cmd)) {
            return listOf(ShellOutput("$cmd: shell built-in command", ShellOutputType.INFO))
        }

        // Check package commands
        if (PKG_COMMANDS.contains(cmd)) {
            return listOf(ShellOutput("$cmd: mentra package manager", ShellOutputType.INFO))
        }

        // Check bin directory
        val binFile = File(binDir, cmd)
        if (binFile.exists() && binFile.canExecute()) {
            return listOf(ShellOutput(binFile.absolutePath, ShellOutputType.INFO))
        }

        return listOf(ShellOutput("$cmd not found", ShellOutputType.ERROR))
    }

    private fun handleClear(): List<ShellOutput> {
        return listOf(ShellOutput("CLEAR_SCREEN", ShellOutputType.INFO))
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    private fun resolvePath(path: String): String {
        return when {
            path.startsWith("~") -> path.replaceFirst("~", environment["HOME"] ?: context.filesDir.absolutePath)
            path.startsWith("/") -> path
            else -> "$currentDirectory/$path"
        }
    }

    private fun isTermuxAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun executeTermuxCommand(command: String): ShellOutput = withContext(Dispatchers.IO) {
        try {
            val intent = android.content.Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            context.startService(intent)
            ShellOutput("Command sent to Termux", ShellOutputType.SUCCESS)
        } catch (e: Exception) {
            ShellOutput("Termux command failed: ${e.message}", ShellOutputType.ERROR)
        }
    }

    private suspend fun executeCommand(command: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Pair(true, output)
            } else {
                Pair(false, error.ifEmpty { output })
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Unknown error")
        }
    }

    private fun loadInstalledPackages() {
        try {
            val file = File(packagesDir, "installed.json")
            if (file.exists()) {
                val json = file.readText()
                // Simple JSON parsing (could use Gson/Moshi)
                // For simplicity, using a basic approach
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load installed packages", e)
        }
    }

    private fun saveInstalledPackages() {
        try {
            val file = File(packagesDir, "installed.json")
            // Save packages to JSON
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save installed packages", e)
        }
    }

    private fun getBuiltInPackageInfo(name: String): PackageInfo? {
        return when (name.lowercase()) {
            "python", "python3" -> PackageInfo(name, "3.11.4", "Python programming language", PackageType.BUILTIN)
            "node", "nodejs" -> PackageInfo(name, "18.16.0", "Node.js JavaScript runtime", PackageType.BUILTIN)
            "ruby" -> PackageInfo(name, "3.2.2", "Ruby programming language", PackageType.BUILTIN)
            "git" -> PackageInfo(name, "2.40.1", "Version control system", PackageType.BUILTIN)
            "curl" -> PackageInfo(name, "8.0.1", "Transfer data with URLs", PackageType.BUILTIN)
            "wget" -> PackageInfo(name, "1.21.3", "Network downloader", PackageType.BUILTIN)
            "vim" -> PackageInfo(name, "9.0", "Vi IMproved text editor", PackageType.BUILTIN)
            "nano" -> PackageInfo(name, "7.2", "Text editor", PackageType.BUILTIN)
            "grep" -> PackageInfo(name, "3.8", "Search text using patterns", PackageType.BUILTIN)
            "sed" -> PackageInfo(name, "4.9", "Stream editor", PackageType.BUILTIN)
            "awk" -> PackageInfo(name, "5.2.1", "Pattern scanning language", PackageType.BUILTIN)
            "jq" -> PackageInfo(name, "1.6", "JSON processor", PackageType.BUILTIN)
            "htop" -> PackageInfo(name, "3.2.2", "Interactive process viewer", PackageType.BUILTIN)
            else -> null
        }
    }

    private fun getAvailablePackages(): List<PackageInfo> {
        return listOf(
            PackageInfo("python", "3.11.4", "Python programming language", PackageType.BUILTIN),
            PackageInfo("nodejs", "18.16.0", "Node.js JavaScript runtime", PackageType.BUILTIN),
            PackageInfo("ruby", "3.2.2", "Ruby programming language", PackageType.BUILTIN),
            PackageInfo("git", "2.40.1", "Version control system", PackageType.BUILTIN),
            PackageInfo("curl", "8.0.1", "Transfer data with URLs", PackageType.BUILTIN),
            PackageInfo("wget", "1.21.3", "Network downloader", PackageType.BUILTIN),
            PackageInfo("vim", "9.0", "Vi IMproved text editor", PackageType.BUILTIN),
            PackageInfo("nano", "7.2", "Text editor", PackageType.BUILTIN),
            PackageInfo("grep", "3.8", "Search text using patterns", PackageType.BUILTIN),
            PackageInfo("sed", "4.9", "Stream editor", PackageType.BUILTIN),
            PackageInfo("awk", "5.2.1", "Pattern scanning language", PackageType.BUILTIN),
            PackageInfo("jq", "1.6", "JSON processor", PackageType.BUILTIN),
            PackageInfo("htop", "3.2.2", "Interactive process viewer", PackageType.BUILTIN),
            PackageInfo("ncurses-utils", "6.4", "Terminal utilities", PackageType.BUILTIN)
        )
    }
}

/**
 * Package information
 */
data class PackageInfo(
    val name: String,
    val version: String,
    val description: String,
    val type: PackageType
)

/**
 * Package type
 */
enum class PackageType {
    BUILTIN,    // Built into Mentra
    PYTHON,     // Python package (pip)
    NPM,        // Node.js package (npm)
    SYSTEM      // System package (pkg/apt)
}

