package com.purplebank.simulator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "purplebank_prefs")

/** Preferências locais (DataStore): ocultar valores e PIN do painel (somente hash SHA-256). */
class Prefs(private val ctx: Context) {
    private val hideKey = booleanPreferencesKey("hide_values")
    private val pinKey = stringPreferencesKey("pin_hash")

    val hideValues: Flow<Boolean> = ctx.dataStore.data.map { it[hideKey] ?: false }

    suspend fun setHide(v: Boolean) { ctx.dataStore.edit { it[hideKey] = v } }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = ctx.dataStore.data.first()[pinKey] ?: hash(DEFAULT_PIN)
        return stored == hash(pin)
    }

    suspend fun setPin(pin: String) { ctx.dataStore.edit { it[pinKey] = hash(pin) } }

    companion object {
        const val DEFAULT_PIN = "1234"
        fun hash(pin: String): String =
            MessageDigest.getInstance("SHA-256").digest("purplebank:$pin".toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
