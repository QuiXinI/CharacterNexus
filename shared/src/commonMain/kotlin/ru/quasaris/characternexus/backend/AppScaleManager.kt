package ru.quasaris.characternexus.backend

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppScaleManager(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val SCALE_FACTOR_KEY = floatPreferencesKey("app_scale_factor")
    }

    val scaleFactor: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[SCALE_FACTOR_KEY] ?: 1.0f
        }

    suspend fun setScaleFactor(scale: Float) {
        dataStore.edit {
            it[SCALE_FACTOR_KEY] = scale
        }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }
}
