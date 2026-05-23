package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bousseta_guard_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        val KEY_PORT_MONITOR = booleanPreferencesKey("port_monitor_enabled")
        val KEY_CAMERA_MONITOR = booleanPreferencesKey("camera_monitor_enabled")
        val KEY_FILE_MONITOR = booleanPreferencesKey("file_monitor_enabled")
        val KEY_REALTIME_ANTIVIRUS = booleanPreferencesKey("realtime_antivirus_enabled")
    }

    val isPortMonitorEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_PORT_MONITOR] ?: true }
    val isCameraMonitorEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_CAMERA_MONITOR] ?: true }
    val isFileMonitorEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_FILE_MONITOR] ?: true }
    val isRealtimeAntivirusEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_REALTIME_ANTIVIRUS] ?: true }

    suspend fun setPortMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PORT_MONITOR] = enabled }
    }

    suspend fun setCameraMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CAMERA_MONITOR] = enabled }
    }

    suspend fun setFileMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FILE_MONITOR] = enabled }
    }

    suspend fun setRealtimeAntivirusEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_REALTIME_ANTIVIRUS] = enabled }
    }
}
