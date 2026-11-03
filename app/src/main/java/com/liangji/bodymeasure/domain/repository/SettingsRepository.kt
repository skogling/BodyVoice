package com.liangji.bodymeasure.domain.repository

import com.liangji.bodymeasure.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    val lastMetricId: Flow<String?>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLastMetricId(metricId: String)
}
