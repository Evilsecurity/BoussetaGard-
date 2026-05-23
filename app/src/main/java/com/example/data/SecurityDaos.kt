package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SecurityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLog)

    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM security_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
}

@Dao
interface ThreatDao {
    @Query("SELECT * FROM threats WHERE status = 'THREAT' ORDER BY detectedAt DESC")
    fun getAllActiveThreats(): Flow<List<ThreatItem>>

    @Query("SELECT * FROM threats ORDER BY detectedAt DESC")
    fun getAllThreats(): Flow<List<ThreatItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreat(threat: ThreatItem)

    @Update
    suspend fun updateThreat(threat: ThreatItem)

    @Query("DELETE FROM threats WHERE packageName = :packageName")
    suspend fun deleteThreatByPackage(packageName: String)
}
