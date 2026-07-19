package ru.quasaris.characters.master.backend

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppScaleManager(private val context: Context) {
    companion object {
        private val SCALE_FACTOR_KEY = floatPreferencesKey("app_scale_factor")
    }

    val scaleFactor: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[SCALE_FACTOR_KEY] ?: 1.0f
        }

    suspend fun setScaleFactor(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[SCALE_FACTOR_KEY] = scale
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
