package com.liangji.bodymeasure.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    override val themeMode: Flow<ThemeMode> = preferences.map { stored ->
        val value = stored[THEME_MODE]
        ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
    }

    override val lastMetricId: Flow<String?> = preferences.map { it[LAST_METRIC_ID] }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setLastMetricId(metricId: String) {
        dataStore.edit { it[LAST_METRIC_ID] = metricId }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_METRIC_ID = stringPreferencesKey("last_metric_id")
    }
}
