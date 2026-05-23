package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threats")
data class ThreatItem(
    @PrimaryKey val packageName: String,
    val appName: String,
    val detectionType: String, // SIGNATURE, HEURISTIC, SUSPICIOUS_PORT, PERSISTENT_CAMERA
    val severity: String, // MEDIUM, HIGH, CRITICAL
    val status: String, // THREAT, IGNORED, REMOVED
    val detectedAt: Long = System.currentTimeMillis(),
    val description: String
)
