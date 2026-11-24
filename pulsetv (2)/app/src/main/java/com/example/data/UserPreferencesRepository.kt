package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.config.FirebaseConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pulseTvDataStore: DataStore<Preferences> by preferencesDataStore(name = "pulsetv_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorite_channel_ids")
        val RECENTLY_WATCHED = stringPreferencesKey("recently_watched_ids")
        val LAST_READ_INBOX_MS = longPreferencesKey("last_read_inbox_ms")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
    }

    val favoriteIds: Flow<Set<String>> = context.pulseTvDataStore.data.map { prefs ->
        prefs[Keys.FAVORITES] ?: emptySet()
    }

    val recentIds: Flow<List<String>> = context.pulseTvDataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENTLY_WATCHED].orEmpty()
        if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    val lastReadInboxMs: Flow<Long> = context.pulseTvDataStore.data.map { prefs ->
        prefs[Keys.LAST_READ_INBOX_MS] ?: 0L
    }

    val adminPin: Flow<String> = context.pulseTvDataStore.data.map { prefs ->
        prefs[Keys.ADMIN_PIN]?.takeIf { it.length in 4..16 && it.all(Char::isDigit) }
            ?: FirebaseConfig.DEFAULT_ADMIN_PIN
    }

    suspend fun toggleFavorite(channelId: String) {
        if (channelId.isBlank()) return
        context.pulseTvDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (channelId in current) current - channelId else current + channelId
        }
    }

    suspend fun recordWatched(channelId: String) {
        if (channelId.isBlank()) return
        context.pulseTvDataStore.edit { prefs ->
            val current = prefs[Keys.RECENTLY_WATCHED].orEmpty()
                .split(",")
                .filter { it.isNotBlank() && it != channelId }
            val updated = (listOf(channelId) + current).take(12)
            prefs[Keys.RECENTLY_WATCHED] = updated.joinToString(",")
        }
    }

    suspend fun clearRecentlyWatched() {
        context.pulseTvDataStore.edit { prefs ->
            prefs.remove(Keys.RECENTLY_WATCHED)
        }
    }

    suspend fun markInboxRead(latestTimestampMs: Long) {
        context.pulseTvDataStore.edit { prefs ->
            val current = prefs[Keys.LAST_READ_INBOX_MS] ?: 0L
            if (latestTimestampMs > current) {
                prefs[Keys.LAST_READ_INBOX_MS] = latestTimestampMs
            }
        }
    }

    suspend fun updateAdminPin(newPin: String) {
        if (newPin.length in 4..16 && newPin.all { it.isDigit() }) {
            context.pulseTvDataStore.edit { prefs ->
                prefs[Keys.ADMIN_PIN] = newPin
            }
        }
    }
}
