package com.liangji.bodymeasure.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DeviceIdStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : DeviceIdProvider {
    override suspend fun getOrCreate(): String {
        val existing = dataStore.data.first()[DEVICE_ID]
        if (existing != null) return existing

        val generated = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            if (preferences[DEVICE_ID] == null) {
                preferences[DEVICE_ID] = generated
            }
        }
        return dataStore.data.first()[DEVICE_ID] ?: generated
    }

    private companion object {
        val DEVICE_ID = stringPreferencesKey("device_id")
    }
}
