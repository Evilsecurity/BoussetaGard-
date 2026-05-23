package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.SecurityRepository
import com.example.model.AppViewModel
import com.example.model.AppViewModelFactory
import com.example.service.GuardForegroundService
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.worker.ScheduledScanWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AppViewModel

    private val requestMultiPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && audioGranted) {
            Toast.makeText(this, "تم تفعيل كافة صلاحيات الخصوصية والمراقبة بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "بعض الميزات تحتاج هذه الصلاحيات للعمل بالشكل المطلوب", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = SecurityRepository(db)
        val preferencesManager = PreferencesManager(applicationContext)
        
        val factory = AppViewModelFactory(repository, preferencesManager, applicationContext)
        viewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]

        // Start safety Foreground Service
        startSecurityService()

        // Schedule periodic scanning using WorkManager
        schedulePeriodicScanning()

        // Request basic run permissions
        requestDevicePermissions()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        GuardBottomNavigation(
                            currentRoute = currentRoute,
                            onNavigate = { screen ->
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToScan = { navController.navigate(Screen.Scan.route) }
                            )
                        }
                        composable(Screen.Scan.route) {
                            ScanScreen(viewModel = viewModel)
                        }
                        composable(Screen.Logs.route) {
                            LogsScreen(viewModel = viewModel)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(viewModel = viewModel)
                        }
                        composable(Screen.About.route) {
                            AboutScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun startSecurityService() {
        val serviceIntent = Intent(this, GuardForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun schedulePeriodicScanning() {
        val scanRequest = PeriodicWorkRequestBuilder<ScheduledScanWorker>(
            4, TimeUnit.HOURS // Scan device every 4 hours automatically
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "BoussetaGuardPeriodicScan",
            ExistingPeriodicWorkPolicy.KEEP,
            scanRequest
        )
    }

    private fun requestDevicePermissions() {
        val permissionsNeeded = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            requestMultiPermissions.launch(ungranted.toTypedArray())
        }
    }
}
