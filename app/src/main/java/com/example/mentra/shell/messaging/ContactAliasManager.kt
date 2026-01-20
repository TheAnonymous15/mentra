package com.example.mentra.shell.messaging

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mentra.messaging.Contact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * CONTACT ALIAS MANAGER
 * Manages relationship aliases for contacts (wife, mom, boss, etc.)
 * ═══════════════════════════════════════════════════════════════════
 */

private val Context.aliasDataStore: DataStore<Preferences> by preferencesDataStore(name = "contact_aliases")

@Singleton
class ContactAliasManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    companion object {
        private val ALIASES_KEY = stringPreferencesKey("contact_aliases")

        // Common alias suggestions
        val SUGGESTED_ALIASES = listOf(
            AliasInfo("wife", "👰", "Your spouse"),
            AliasInfo("husband", "🤵", "Your spouse"),
            AliasInfo("mom", "👩", "Your mother"),
            AliasInfo("mother", "👩", "Your mother"),
            AliasInfo("dad", "👨", "Your father"),
            AliasInfo("father", "👨", "Your father"),
            AliasInfo("son", "👦", "Your son"),
            AliasInfo("daughter", "👧", "Your daughter"),
            AliasInfo("brother", "👦", "Your brother"),
            AliasInfo("bro", "👦", "Your brother"),
            AliasInfo("sister", "👧", "Your sister"),
            AliasInfo("sis", "👧", "Your sister"),
            AliasInfo("boss", "💼", "Your boss/manager"),
            AliasInfo("bestie", "🤝", "Best friend"),
            AliasInfo("bff", "🤝", "Best friend forever"),
            AliasInfo("girlfriend", "💕", "Your girlfriend"),
            AliasInfo("gf", "💕", "Your girlfriend"),
            AliasInfo("boyfriend", "💙", "Your boyfriend"),
            AliasInfo("bf", "💙", "Your boyfriend"),
            AliasInfo("babe", "❤️", "Term of endearment"),
            AliasInfo("honey", "🍯", "Term of endearment"),
            AliasInfo("love", "💖", "Term of endearment"),
            AliasInfo("grandma", "👵", "Your grandmother"),
            AliasInfo("grandmother", "👵", "Your grandmother"),
            AliasInfo("grandpa", "👴", "Your grandfather"),
            AliasInfo("grandfather", "👴", "Your grandfather"),
            AliasInfo("uncle", "👨", "Your uncle"),
            AliasInfo("aunt", "👩", "Your aunt"),
            AliasInfo("cousin", "🧑", "Your cousin"),
            AliasInfo("partner", "💑", "Your partner"),
            AliasInfo("spouse", "💑", "Your spouse"),
            AliasInfo("home", "🏠", "Home number"),
            AliasInfo("work", "💼", "Work contact"),
            AliasInfo("doctor", "👨‍⚕️", "Your doctor"),
            AliasInfo("emergency", "🚨", "Emergency contact")
        )
    }

    /**
     * Get all saved aliases
     */
    suspend fun getAllAliases(): Map<String, ContactAlias> {
        return context.aliasDataStore.data.map { preferences ->
            val json = preferences[ALIASES_KEY] ?: "{}"
            val type = object : TypeToken<Map<String, ContactAlias>>() {}.type
            gson.fromJson<Map<String, ContactAlias>>(json, type) ?: emptyMap()
        }.first()
    }

    /**
     * Get contact by alias
     */
    suspend fun getContactByAlias(alias: String): Contact? {
        val aliases = getAllAliases()
        val contactAlias = aliases[alias.lowercase()] ?: return null

        return Contact(
            id = contactAlias.contactId,
            name = contactAlias.contactName,
            phoneNumbers = listOf(contactAlias.phoneNumber),
            photoUri = contactAlias.photoUri
        )
    }

    /**
     * Check if an alias exists
     */
    suspend fun hasAlias(alias: String): Boolean {
        return getAllAliases().containsKey(alias.lowercase())
    }

    /**
     * Set/update an alias for a contact
     */
    suspend fun setAlias(alias: String, contact: Contact, phoneNumber: String) {
        context.aliasDataStore.edit { preferences ->
            val currentAliases = getAllAliases().toMutableMap()

            currentAliases[alias.lowercase()] = ContactAlias(
                alias = alias.lowercase(),
                contactId = contact.id,
                contactName = contact.name,
                phoneNumber = phoneNumber,
                photoUri = contact.photoUri
            )

            preferences[ALIASES_KEY] = gson.toJson(currentAliases)
        }
    }

    /**
     * Remove an alias
     */
    suspend fun removeAlias(alias: String) {
        context.aliasDataStore.edit { preferences ->
            val currentAliases = getAllAliases().toMutableMap()
            currentAliases.remove(alias.lowercase())
            preferences[ALIASES_KEY] = gson.toJson(currentAliases)
        }
    }

    /**
     * Get all aliases for a specific contact
     */
    suspend fun getAliasesForContact(contactId: String): List<String> {
        return getAllAliases()
            .filter { it.value.contactId == contactId }
            .keys
            .toList()
    }

    /**
     * Get suggested alias info
     */
    fun getSuggestedAliasInfo(alias: String): AliasInfo? {
        return SUGGESTED_ALIASES.find { it.alias.equals(alias, ignoreCase = true) }
    }

    /**
     * Search aliases
     */
    suspend fun searchAliases(query: String): List<Pair<String, ContactAlias>> {
        val lowercaseQuery = query.lowercase()
        return getAllAliases()
            .filter { (alias, contactAlias) ->
                alias.contains(lowercaseQuery) ||
                contactAlias.contactName.lowercase().contains(lowercaseQuery)
            }
            .toList()
    }
}

/**
 * Stored alias information
 */
data class ContactAlias(
    val alias: String,
    val contactId: String,
    val contactName: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

/**
 * Suggested alias information
 */
data class AliasInfo(
    val alias: String,
    val emoji: String,
    val description: String
)

