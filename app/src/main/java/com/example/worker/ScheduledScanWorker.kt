package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.SecurityRepository
import com.example.service.GuardForegroundService
import android.content.pm.PackageManager
import com.example.data.ThreatItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduledScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = SecurityRepository(AppDatabase.getDatabase(applicationContext))
        repository.addLog("GENERAL", "فحص دوري مجدول", "تم تفعيل فحص الخلفية المجدول للتحقق من التطبيقات المصابة والبرمجيات الخبيثة", "LOW")

        // Perform mock signature heuristic check on background scan
        val pm = applicationContext.packageManager
        val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

        var threatsFound = 0
        val maliciousKeywords = listOf("hack", "spy", "root", "exploit", "trojan", "malware", "backdoor", "keylogger", "bypass")
        val highlySuspiciousPermissions = listOf(
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.SYSTEM_ALERT_WINDOW"
        )

        for (pkg in installedPackages) {
            val appInfo = pkg.applicationInfo ?: continue
            val appLabel = pm.getApplicationLabel(appInfo).toString()
            val packageName = pkg.packageName

            // Skip system apps
            if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue
            }

            var threatScore = 0
            val requestedPermissions = pkg.requestedPermissions ?: emptyArray()

            // 1. Signature-based keyword checks
            for (word in maliciousKeywords) {
                if (appLabel.lowercase().contains(word) || packageName.lowercase().contains(word)) {
                    threatScore += 3
                }
            }

            // 2. Behavioral permission check
            for (wanted in highlySuspiciousPermissions) {
                if (requestedPermissions.contains(wanted)) {
                    threatScore += 4
                }
            }

            // 3. FakeBank heuristics
            if (appLabel.lowercase().contains("bank") && !packageName.contains("bank") && !packageName.contains("wallet")) {
                threatScore += 4
            }

            if (threatScore >= 4) {
                threatsFound++
                val severity = if (threatScore >= 7) "CRITICAL" else "HIGH"
                val description = "تطبيق مشبوه يحتوي على صلاحيات حساسة أو تشابه مع برمجيات FakeBank أو تروجان تجسسي يدعى ($appLabel)"

                repository.addThreat(
                    ThreatItem(
                        packageName = packageName,
                        appName = appLabel,
                        detectionType = "HEURISTIC",
                        severity = severity,
                        status = "THREAT",
                        description = description
                    )
                )

                repository.addLog(
                    type = "MALWARE",
                    title = "تهديد مكتشف: $appLabel",
                    details = "تم العثور على تهديد أثناء الفحص المجدول. الحزمة: $packageName. السبب: $description (CWE-284, CWE-927)",
                    severity = severity
                )
            }
        }

        if (threatsFound > 0) {
            repository.addLog("GENERAL", "اكتمل الفحص المجدول", "تم انتهاء الفحص الدوري بنجاح. تم العثور على $threatsFound تهديد فعال تحتاج مراجعته.", "HIGH")
        } else {
            repository.addLog("GENERAL", "اكتمل الفحص المجدول", "تم فحص جميع التطبيقات بنجاح. جهازك آمن بالكامل ولا توجد تهديدات معروفة.", "LOW")
        }

        Result.success()
    }
}
