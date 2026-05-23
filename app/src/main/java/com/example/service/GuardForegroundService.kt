package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.SecurityRepository
import kotlinx.coroutines.*
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.net.InetAddress
import java.net.NetworkInterface

class GuardForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: SecurityRepository
    private lateinit var preferencesManager: PreferencesManager

    private var portScanningJob: Job? = null
    private var cameraMonitoringJob: Job? = null
    private var fileMonitoringJob: Job? = null

    // For file monitoring
    private val observedDirectories = mutableListOf<FileObserverWrapper>()

    override fun onCreate() {
        super.onCreate()
        repository = SecurityRepository(AppDatabase.getDatabase(applicationContext))
        preferencesManager = PreferencesManager(applicationContext)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Bousseta Guard Active", "إجراء نظام المراقبة النشطة لحمايتك"))

        observePreferences()
    }

    private fun observePreferences() {
        serviceScope.launch {
            preferencesManager.isPortMonitorEnabled.collect { enabled ->
                if (enabled) startPortMonitor() else stopPortMonitor()
            }
        }
        serviceScope.launch {
            preferencesManager.isCameraMonitorEnabled.collect { enabled ->
                if (enabled) startCameraMonitor() else stopCameraMonitor()
            }
        }
        serviceScope.launch {
            preferencesManager.isFileMonitorEnabled.collect { enabled ->
                if (enabled) startFileMonitor() else stopFileMonitor()
            }
        }
    }

    private fun startPortMonitor() {
        portScanningJob?.cancel()
        portScanningJob = serviceScope.launch {
            repository.addLog("PORT", "بدء مراقبة المنافذ", "تم تشغيل نظام فحص ومراقبة المنافذ المفتوحة بنجاح", "LOW")
            val commonPorts = listOf(21, 22, 23, 80, 443, 5555, 8080)
            while (isActive) {
                for (port in commonPorts) {
                    if (!isActive) break
                    try {
                        // Check if port is open locally by trying to bind or connect
                        val serverSocket = ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))
                        serverSocket.close()
                    } catch (e: Exception) {
                        // If we can't bind to local interface, it might already be in use / open!
                        val detectedMsg = "تم الكشف عن منفذ مفتوح قيد الاستخدام أو مفتوح خفية: $port"
                        val severity = if (port == 5555) "HIGH" else "MEDIUM"
                        repository.addLog(
                            type = "PORT",
                            title = "منفذ مفتوح مشبوه: $port",
                            details = "$detectedMsg. هذا قد يشير إلى محاولة تحكم عن بعد (CWE-276 / ADB backdoor).",
                            severity = severity
                        )
                        Log.w("GuardService", "Suspicious open port found: $port")
                    }
                }
                delay(15000) // Scan every 15 seconds
            }
        }
    }

    private fun stopPortMonitor() {
        portScanningJob?.cancel()
        portScanningJob = null
    }

    private fun startCameraMonitor() {
        cameraMonitoringJob?.cancel()
        cameraMonitoringJob = serviceScope.launch {
            repository.addLog("CAMERA", "بدء مراقبة الكاميرا", "نظام الكشف عن الاستخدام غير المصرح به للكاميرا نشط الآن", "LOW")
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            
            // Build direct monitoring loop (heuristic check) or camera availability callback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                var previousCameraStates = mutableMapOf<String, Boolean>()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.registerAvailabilityCallback(object : CameraManager.AvailabilityCallback() {
                        override fun onCameraAvailable(cameraId: String) {
                            super.onCameraAvailable(cameraId)
                            previousCameraStates[cameraId] = true
                        }

                        override fun onCameraUnavailable(cameraId: String) {
                            super.onCameraUnavailable(cameraId)
                            // Availability went to false - someone opened the camera!
                            previousCameraStates[cameraId] = false
                            serviceScope.launch {
                                repository.addLog(
                                    type = "CAMERA",
                                    title = "محاولة وصول للكاميرا (CWE-284)",
                                    details = "تم رصد محاولة وصول نشطة أو استخدام للكاميرا رقم $cameraId بدون إشعار مسبق. سجل التجسس نشط الآن.",
                                    severity = "HIGH"
                                )
                                showDirectAlert("تحذير تجسس الكاميرا", "تم كشف دخول غير مصرح به للكاميرا!")
                            }
                        }
                    }, Handler(Looper.getMainLooper()))
                }
            }
        }
    }

    private fun stopCameraMonitor() {
        cameraMonitoringJob?.cancel()
        cameraMonitoringJob = null
    }

    @Suppress("DEPRECATION")
    private fun startFileMonitor() {
        stopFileMonitor()
        fileMonitoringJob = serviceScope.launch {
            repository.addLog("FILE", "بدء حماية تسريب الملفات", "نظام مراقبة inotify FileObserver نشط لمجلدات الصور والمستندات", "LOW")

            val pathsToObserve = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            )

            for (dir in pathsToObserve) {
                if (dir.exists()) {
                    try {
                        val observer = FileObserverWrapper(dir.absolutePath) { event, path ->
                            serviceScope.launch {
                                val action = when (event) {
                                    FileObserver.ACCESS -> "قراءة"
                                    FileObserver.MODIFY -> "تعديل"
                                    FileObserver.CREATE -> "إنشاء"
                                    FileObserver.DELETE -> "حذف"
                                    FileObserver.MOVED_TO -> "نقل إلى"
                                    FileObserver.MOVED_FROM -> "نقل من"
                                    else -> "وصول"
                                }
                                repository.addLog(
                                    type = "FILE",
                                    title = "الوصول إلى ملفات خاصة (CWE-927)",
                                    details = "تم الكشف عن عملية $action للملف ($path) في المجلد المُعقّم (${dir.name}). تم رصدها وحمايتها وتوثيقها.",
                                    severity = "MEDIUM"
                                )
                            }
                        }
                        observer.startWatching()
                        observedDirectories.add(observer)
                    } catch (e: Exception) {
                        Log.e("GuardService", "Failed to observe directory: ${dir.absolutePath}", e)
                    }
                }
            }
        }
    }

    private fun stopFileMonitor() {
        fileMonitoringJob?.cancel()
        fileMonitoringJob = null
        for (observer in observedDirectories) {
            observer.stopWatching()
        }
        observedDirectories.clear()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bousseta Guard Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showDirectAlert(title: String, text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()
        notificationManager.notify(NOTIFICATION_ALERT_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        stopPortMonitor()
        stopCameraMonitor()
        stopFileMonitor()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "BoussetaGuardChannel"
        const val NOTIFICATION_ID = 10001
        const val NOTIFICATION_ALERT_ID = 10002
    }
}

@Suppress("DEPRECATION")
class FileObserverWrapper(path: String, val onEventTriggered: (Int, String?) -> Unit) : FileObserver(path, ALL_EVENTS) {
    override fun onEvent(event: Int, path: String?) {
        val mask = event and ALL_EVENTS
        if (mask != 0 && path != null) {
            onEventTriggered(mask, path)
        }
    }
}
