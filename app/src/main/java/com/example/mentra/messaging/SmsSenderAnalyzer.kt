package com.example.mentra.messaging

/**
 * SMS Sender Analyzer
 * Detects unreplyable SMS senders like banks, services, etc.
 *
 * Features:
 * - Detect shortcodes
 * - Detect alphanumeric senders
 * - Known service providers
 * - Banks & financial institutions
 * - Marketing/promotional senders
 */
object SmsSenderAnalyzer {

    // Common patterns for unreplyable senders
    private val shortCodePatterns = listOf(
        Regex("^\\d{4,6}$"),           // 4-6 digit shortcodes
        Regex("^[A-Z]{2,}\\d{2,}$"),   // Like "AD123"
    )

    // Alphanumeric sender patterns (usually unreplyable)
    private val alphanumericPattern = Regex("^[A-Za-z][A-Za-z0-9\\-_\\.\\s]{1,10}$")

    // Known unreplyable sender keywords
    private val unreplyableKeywords = listOf(
        // Banks & Financial
        "MPESA", "M-PESA", "MSHWARI", "KCB", "EQUITY", "COOP", "ABSA", "STANBIC",
        "NCBA", "DTB", "BARCLAYS", "STANDARD", "CHASE", "CITI", "BANK", "FINANCE",
        "LOAN", "CREDIT", "FULIZA", "HUSTLER", "TALA", "BRANCH", "ZENKA", "OPESA",

        // Telecom
        "SAFARICOM", "AIRTEL", "TELKOM", "FAIBA", "JAMII", "JTL",

        // Services
        "JUMIA", "UBER", "BOLT", "GLOVO", "NETFLIX", "SPOTIFY", "GOOGLE", "APPLE",
        "AMAZON", "PAYPAL", "STRIPE", "VISA", "MASTERCARD", "AMEX",

        // Government & Utilities
        "KPLC", "NAIROBI", "WATER", "NHIF", "NSSF", "KRA", "ECITIZEN", "HUDUMA",
        "NTSA", "KEBS", "EPRA", "HELB",

        // Delivery & Logistics
        "DHL", "FEDEX", "G4S", "WELLS", "FARGO", "SENDY", "LORI",

        // Common prefixes for services
        "INFO", "ALERT", "NOTIFY", "UPDATE", "PROMO", "AD-", "VM-", "TM-", "DM-",
        "BW-", "JD-", "HP-", "BZ-", "TX-", "LM-", "CP-", "VK-", "BP-", "CB-"
    )

    // Promotional/Marketing patterns
    private val promotionalPatterns = listOf(
        Regex("(?i)^promo", RegexOption.IGNORE_CASE),
        Regex("(?i)^ad-", RegexOption.IGNORE_CASE),
        Regex("(?i)^info", RegexOption.IGNORE_CASE),
        Regex("(?i)^alert", RegexOption.IGNORE_CASE),
    )

    /**
     * Check if sender is unreplyable
     */
    fun isUnreplyable(sender: String): Boolean {
        val trimmedSender = sender.trim().uppercase()

        // Check shortcode patterns
        if (shortCodePatterns.any { it.matches(sender.trim()) }) {
            return true
        }

        // Check alphanumeric pattern (not a phone number)
        if (alphanumericPattern.matches(sender.trim()) && !isPhoneNumber(sender)) {
            return true
        }

        // Check known unreplyable keywords
        if (unreplyableKeywords.any { trimmedSender.contains(it) }) {
            return true
        }

        // Check promotional patterns
        if (promotionalPatterns.any { it.containsMatchIn(sender) }) {
            return true
        }

        // Check if it's NOT a valid phone number (contains letters)
        if (sender.any { it.isLetter() }) {
            return true
        }

        return false
    }

    /**
     * Check if string is a valid phone number
     */
    private fun isPhoneNumber(input: String): Boolean {
        val cleaned = input.replace(Regex("[\\s\\-\\(\\)\\+]"), "")
        return cleaned.length >= 9 && cleaned.all { it.isDigit() }
    }

    /**
     * Get sender type for UI display
     */
    fun getSenderType(sender: String): SenderType {
        val trimmedSender = sender.trim().uppercase()

        return when {
            // Banks
            listOf("MPESA", "M-PESA", "MSHWARI", "KCB", "EQUITY", "COOP", "ABSA",
                   "STANBIC", "NCBA", "DTB", "BARCLAYS", "STANDARD", "BANK",
                   "FULIZA", "TALA", "BRANCH").any { trimmedSender.contains(it) } -> SenderType.BANK

            // Telecom
            listOf("SAFARICOM", "AIRTEL", "TELKOM", "FAIBA").any { trimmedSender.contains(it) } -> SenderType.TELECOM

            // Shopping/Delivery
            listOf("JUMIA", "UBER", "BOLT", "GLOVO", "DHL", "FEDEX", "SENDY").any { trimmedSender.contains(it) } -> SenderType.SHOPPING

            // Government
            listOf("KPLC", "NHIF", "NSSF", "KRA", "ECITIZEN", "HUDUMA", "NTSA").any { trimmedSender.contains(it) } -> SenderType.GOVERNMENT

            // Promotional
            promotionalPatterns.any { it.containsMatchIn(sender) } ||
                listOf("PROMO", "AD-", "INFO", "ALERT").any { trimmedSender.startsWith(it) } -> SenderType.PROMOTIONAL

            // Shortcode
            shortCodePatterns.any { it.matches(sender.trim()) } -> SenderType.SHORTCODE

            // Service (catch-all for alphanumeric)
            sender.any { it.isLetter() } -> SenderType.SERVICE

            // Regular contact
            else -> SenderType.CONTACT
        }
    }

    /**
     * Get icon suggestion for sender type
     */
    fun getIconForSenderType(type: SenderType): String {
        return when (type) {
            SenderType.BANK -> "💰"
            SenderType.TELECOM -> "📱"
            SenderType.SHOPPING -> "🛒"
            SenderType.GOVERNMENT -> "🏛️"
            SenderType.PROMOTIONAL -> "📢"
            SenderType.SHORTCODE -> "📟"
            SenderType.SERVICE -> "🏢"
            SenderType.CONTACT -> "👤"
        }
    }

    /**
     * Get color for sender type
     */
    fun getColorForSenderType(type: SenderType): Long {
        return when (type) {
            SenderType.BANK -> 0xFF4CAF50        // Green
            SenderType.TELECOM -> 0xFF2196F3     // Blue
            SenderType.SHOPPING -> 0xFFFF9800    // Orange
            SenderType.GOVERNMENT -> 0xFF9C27B0  // Purple
            SenderType.PROMOTIONAL -> 0xFFE91E63 // Pink
            SenderType.SHORTCODE -> 0xFF607D8B   // Blue Grey
            SenderType.SERVICE -> 0xFF00BCD4     // Cyan
            SenderType.CONTACT -> 0xFF4EC9B0     // Teal (default)
        }
    }
}

/**
 * Sender type enum
 */
enum class SenderType {
    CONTACT,      // Regular contact (replyable)
    BANK,         // Bank/Financial (unreplyable)
    TELECOM,      // Telecom provider (unreplyable)
    SHOPPING,     // Shopping/Delivery (unreplyable)
    GOVERNMENT,   // Government services (unreplyable)
    PROMOTIONAL,  // Marketing/Promo (unreplyable)
    SHORTCODE,    // Shortcode (unreplyable)
    SERVICE       // Generic service (unreplyable)
}

