package com.example.model

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.pm.ApplicationInfo

class AppViewModel(
    private val repository: SecurityRepository,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModel() {

    // Scanner state
    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress = _scanProgress.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scanStatus = MutableStateFlow("مستعد للبدء")
    val scanStatus = _scanStatus.asStateFlow()

    val allLogs: StateFlow<List<SecurityLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeThreats: StateFlow<List<ThreatItem>> = repository.activeThreats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allThreats: StateFlow<List<ThreatItem>> = repository.allThreats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isPortMonitorEnabled = preferencesManager.isPortMonitorEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isCameraMonitorEnabled = preferencesManager.isCameraMonitorEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isFileMonitorEnabled = preferencesManager.isFileMonitorEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isRealtimeAntivirusEnabled = preferencesManager.isRealtimeAntivirusEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setPortMonitor(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setPortMonitorEnabled(enabled)
        val stateText = if (enabled) "تشغيل" else "إيقاف"
        repository.addLog("GENERAL", "${stateText} مراقبة المنافذ", "قام المستخدم بـ ${stateText} مراقب المنافذ من الإعدادات", "LOW")
    }

    fun setCameraMonitor(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setCameraMonitorEnabled(enabled)
        val stateText = if (enabled) "تشغيل" else "إيقاف"
        repository.addLog("GENERAL", "${stateText} مراقبة الكاميرا", "قام المستخدم بـ ${stateText} مراقب حماية الكاميرا والخصوصية", "LOW")
    }

    fun setFileMonitor(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setFileMonitorEnabled(enabled)
        val stateText = if (enabled) "تشغيل" else "إيقاف"
        repository.addLog("GENERAL", "${stateText} حماية الملفات", "قام المستخدم بـ ${stateText} حظر ومراقبة تسريب البيانات والملفات الخاصة", "LOW")
    }

    fun setRealtimeAntivirus(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setRealtimeAntivirusEnabled(enabled)
        val stateText = if (enabled) "تشغيل" else "إيقاف"
        repository.addLog("GENERAL", "${stateText} مكافحة الفيروسات", "قام المستخدم بـ ${stateText} نظام حماية البرمجيات الخبيثة المباشر", "LOW")
    }

    fun clearLogs() = viewModelScope.launch {
        repository.clearLogs()
    }

    fun ignoreThreat(threat: ThreatItem) = viewModelScope.launch {
        repository.updateThreat(threat.copy(status = "IGNORED"))
        repository.addLog("GENERAL", "تجاهل تهديد", "تم استبعاد التهديد الخاص بالتطبيق (${threat.appName}) بناءً على إرادة المستخدم", "LOW")
    }

    fun removeThreat(threat: ThreatItem) = viewModelScope.launch {
        repository.deleteThreatByPackage(threat.packageName)
        repository.addLog("GENERAL", "إزالة تهديد", "تم معالجة وحذف التهديد الفعال للتطبيق (${threat.appName})", "LOW")
    }

    fun startSecurityScan() = viewModelScope.launch {
        if (_isScanning.value) return@launch
        _isScanning.value = true
        _scanProgress.value = 0f
        _scanStatus.value = "جاري تهيئة قاعدة بيانات التواقيع البسيطة..."

        repository.addLog("GENERAL", "بدء الفحص اليدوي المباشر", "تم تفعيل محرك الفحص السلوكي الشامل Bousseta Engine", "LOW")

        val pm = context.packageManager
        val installedPackages = withContext(Dispatchers.IO) {
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }

        val totalSize = installedPackages.size
        var processed = 0

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

            _scanStatus.value = "جاري فحص: $appLabel"
            processed++
            _scanProgress.value = processed.toFloat() / totalSize.toFloat()

            // Heuristic malware check
            var threatScore = 0
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (!isSystem) {
                // Heuristic 1: Malicious word matching
                for (word in maliciousKeywords) {
                    if (appLabel.lowercase().contains(word) || packageName.lowercase().contains(word)) {
                        threatScore += 3
                    }
                }

                // Heuristic 2: Suspicious permission check
                val requestedPermissions = pkg.requestedPermissions ?: emptyArray()
                for (permission in highlySuspiciousPermissions) {
                    if (requestedPermissions.contains(permission)) {
                        threatScore += 4
                    }
                }

                // Heuristic 3: FakeBank checks (CWE-284 overlay trojans)
                if (appLabel.lowercase().contains("bank") && !packageName.contains("bank") && !packageName.contains("wallet")) {
                    threatScore += 4
                }

                if (threatScore >= 4) {
                    val severity = if (threatScore >= 7) "CRITICAL" else "HIGH"
                    val description = "تطبيق يحمل ترقية غير مصرح بها أو يطلب صلاحيات قد تؤدي إلى هجوم تصيد FakeBank وتروجانات الوصول البعيد (CWE-284)."
                    
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
                        title = "تهديد نشط: $appLabel",
                        details = "$description الحزمة: $packageName. تم رصده وتوثيقه للحظر.",
                        severity = severity
                    )
                }
            }

            kotlinx.coroutines.delay(20) // Provide smooth progress animation visualizer
        }

        _isScanning.value = false
        _scanStatus.value = "اكتمل الفحص الشامل بنجاح"
        repository.addLog("GENERAL", "اكتمل الفحص بالكامل", "انتهى فحص الأمن الوقائي لمضاد الفيروسات Bousseta Engine.", "LOW")
    }

    fun simulateIntrusion(attackType: String) = viewModelScope.launch {
        when (attackType) {
            "CAMERA" -> {
                repository.addThreat(
                    ThreatItem(
                        packageName = "com.attacker.spycam",
                        appName = "طفيلي الكاميرا المخفي (SpyCam Test)",
                        detectionType = "HEURISTIC",
                        severity = "CRITICAL",
                        status = "THREAT",
                        description = "تم رصد محاولة تشغيل الكاميرا في الخلفية دون علم المستخدم (CWE-284). تم إحباط البث الخارجي فورياً لحماية خصوصيتك السمعية والبصرية."
                    )
                )
                repository.addLog(
                    type = "CAMERA",
                    title = "إحباط هجوم تجسس كاميرا",
                    details = "تم الكشف عن تطبيق 'SpyCam Test' يحاول فتح الكاميرا سراً. تم تفعيل لغة الرصد والخصوصية وتشتيت الاتصال بنجاح بالدقة الكاملة قبل التقاط أي صور.",
                    severity = "CRITICAL"
                )
            }
            "ADB" -> {
                repository.addThreat(
                    ThreatItem(
                        packageName = "com.attacker.adbbackdoor",
                        appName = "مستغل الثغرات الخلفية (Port 5555 Root)",
                        detectionType = "PORT_MONITOR",
                        severity = "CRITICAL",
                        status = "THREAT",
                        description = "تم اكتشاف منفذ مفتوح غير محمي (CWE-276) يتيح حقن الأوامر والتحكم بالهاتف بالكامل. تم العزل وتصحيح الحماية تلقائياً."
                    )
                )
                repository.addLog(
                    type = "PORT",
                    title = "صد تسلل عبر منفذ مفتوح",
                    details = "تم الكشف عن محاولة تمكين اتصال ADB بعيد على المنفذ 5555. تم الحظر وإغلاق المنفذ وحجب الـ IP المتسلل تلقائياً لمنع أي تلاعب ببياناتك.",
                    severity = "CRITICAL"
                )
            }
            "FILE" -> {
                repository.addThreat(
                    ThreatItem(
                        packageName = "com.attacker.fileleaker",
                        appName = "تطبيق تصيد وسرقة الصور والملفات",
                        detectionType = "FILE_MONITOR",
                        severity = "HIGH",
                        status = "THREAT",
                        description = "رصد سلوك مريب يحاول قراءة مجلد DCIM بصورة مكررة وبدء نقل خارجي للملفات والصور الشخصية (CWE-921 / CWE-927)."
                    )
                )
                repository.addLog(
                    type = "FILE",
                    title = "منع سرقة ملفات DCIM والصور",
                    details = "تم حظر محاولة نقل 14 ملف مشفر إلى خادم خارجي مجهول المصدر من قبل حزمة 'com.attacker.fileleaker'. تم تجميد نشاط التطبيق.",
                    severity = "HIGH"
                )
            }
        }
    }
}

class AppViewModelFactory(
    private val repository: SecurityRepository,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository, preferencesManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
