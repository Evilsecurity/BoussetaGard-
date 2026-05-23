package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // CAMERA, MIC, PORT, FILE, MALWARE, GENERAL
    val title: String,
    val details: String,
    val severity: String // LOW, MEDIUM, HIGH, CRITICAL
)
