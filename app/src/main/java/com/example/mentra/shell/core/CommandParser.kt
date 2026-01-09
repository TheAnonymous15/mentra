package com.example.mentra.shell.core

import com.example.mentra.shell.models.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses text input into ShellCommand objects
 * Handles tokenization and basic command structure
 */
@Singleton
class CommandParser @Inject constructor() {

    /**
     * Parse text input into a ShellCommand
     */
    fun parse(input: String): ShellCommand {
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return ShellCommand(
                raw = input,
                verb = "",
                target = null,
                entity = null
            )
        }

        // Tokenize
        val tokens = tokenize(trimmed)

        if (tokens.isEmpty()) {
            return ShellCommand(
                raw = input,
                verb = "",
                target = null,
                entity = null
            )
        }

        // Extract verb (first token)
        val verb = tokens[0].lowercase()

        // Extract target and entity
        val (target, entity, params) = extractParts(tokens.drop(1))

        return ShellCommand(
            raw = input,
            verb = verb,
            target = target,
            entity = entity,
            params = params
        )
    }

    /**
     * Tokenize input string
     * Handles quotes for multi-word arguments
     */
    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var quoteChar = '"'

        for (i in input.indices) {
            val char = input[i]

            when {
                // Start or end quotes
                (char == '"' || char == '\'') -> {
                    if (inQuotes && char == quoteChar) {
                        // End quote
                        inQuotes = false
                        if (current.isNotEmpty()) {
                            tokens.add(current.toString())
                            current = StringBuilder()
                        }
                    } else if (!inQuotes) {
                        // Start quote
                        inQuotes = true
                        quoteChar = char
                    } else {
                        // Quote inside different quote type
                        current.append(char)
                    }
                }

                // Whitespace (separator when not in quotes)
                char.isWhitespace() -> {
                    if (inQuotes) {
                        current.append(char)
                    } else if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current = StringBuilder()
                    }
                }

                // Regular character
                else -> {
                    current.append(char)
                }
            }
        }

        // Add last token
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }

        return tokens
    }

    /**
     * Extract target, entity, and parameters from tokens
     */
    private fun extractParts(tokens: List<String>): Triple<String?, String?, Map<String, String>> {
        if (tokens.isEmpty()) {
            return Triple(null, null, emptyMap())
        }

        val params = mutableMapOf<String, String>()
        val nonParamTokens = mutableListOf<String>()

        // Separate parameters (--key=value or --key value) from regular tokens
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            if (token.startsWith("--")) {
                // Parameter
                val key = token.substring(2)
                if (key.contains("=")) {
                    // --key=value format
                    val parts = key.split("=", limit = 2)
                    params[parts[0]] = parts.getOrNull(1) ?: ""
                } else if (i + 1 < tokens.size && !tokens[i + 1].startsWith("--")) {
                    // --key value format
                    params[key] = tokens[i + 1]
                    i++
                } else {
                    // --key (boolean flag)
                    params[key] = "true"
                }
            } else {
                nonParamTokens.add(token)
            }
            i++
        }

        // First non-param token is target, rest is entity
        val target = nonParamTokens.firstOrNull()
        val entity = if (nonParamTokens.size > 1) {
            nonParamTokens.drop(1).joinToString(" ")
        } else {
            null
        }

        return Triple(target, entity, params)
    }

    /**
     * Parse multiple commands (separated by ; or &&)
     */
    fun parseMultiple(input: String): List<ShellCommand> {
        // Split by ; or &&
        val commands = input.split(Regex(";|&&"))
        return commands.map { parse(it) }
    }

    /**
     * Validate command syntax
     */
    fun validate(command: ShellCommand): Boolean {
        // Empty verb is invalid
        if (command.verb.isEmpty()) {
            return false
        }

        // Check for required components based on verb
        return when (command.verb) {
            "open", "launch", "start" -> command.target != null
            "call" -> command.target != null
            "message", "sms" -> command.target != null && command.entity != null
            "play" -> command.target != null || command.entity != null
            else -> true // Most commands are valid without strict requirements
        }
    }
}

