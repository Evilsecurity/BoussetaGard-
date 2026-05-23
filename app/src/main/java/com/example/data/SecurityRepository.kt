package com.example.data

import kotlinx.coroutines.flow.Flow

class SecurityRepository(private val db: AppDatabase) {
    val allLogs: Flow<List<SecurityLog>> = db.securityLogDao().getAllLogs()
    val activeThreats: Flow<List<ThreatItem>> = db.threatDao().getAllActiveThreats()
    val allThreats: Flow<List<ThreatItem>> = db.threatDao().getAllThreats()

    suspend fun addLog(type: String, title: String, details: String, severity: String) {
        val log = SecurityLog(
            type = type,
            title = title,
            details = details,
            severity = severity
        )
        db.securityLogDao().insertLog(log)
    }

    suspend fun clearLogs() {
        db.securityLogDao().clearLogs()
    }

    suspend fun deleteLogById(id: Int) {
        db.securityLogDao().deleteLogById(id)
    }

    suspend fun addThreat(threat: ThreatItem) {
        db.threatDao().insertThreat(threat)
    }

    suspend fun updateThreat(threat: ThreatItem) {
        db.threatDao().updateThreat(threat)
    }

    suspend fun deleteThreatByPackage(packageName: String) {
        db.threatDao().deleteThreatByPackage(packageName)
    }
}
