package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val isPortMonitorEnabled by viewModel.isPortMonitorEnabled.collectAsState()
    val isCameraMonitorEnabled by viewModel.isCameraMonitorEnabled.collectAsState()
    val isFileMonitorEnabled by viewModel.isFileMonitorEnabled.collectAsState()
    val isRealtimeAntivirusEnabled by viewModel.isRealtimeAntivirusEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات المراقبة النشطة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "تحكم بإعدادات وموديولات المراقبة الفورية للهاتف:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            MonitorSwitchRow(
                title = "مراقبة المنافذ المفتوحة (Open Ports)",
                description = "كشف وفحص كافة المنافذ المفتوحة والاتصالات المشبوهة (CWE-276 Backdoors).",
                icon = Icons.Default.Router,
                checked = isPortMonitorEnabled,
                onCheckedChange = { viewModel.setPortMonitor(it) }
            )

            MonitorSwitchRow(
                title = "مراقب حماية الكاميرا والخصوصية",
                description = "كشف محاولات تشغيل الكاميرا في الخلفية بدون علمك وتنبيهك فوراً للتجسس خفية (CWE-284).",
                icon = Icons.Default.CameraAlt,
                checked = isCameraMonitorEnabled,
                onCheckedChange = { viewModel.setCameraMonitor(it) }
            )

            MonitorSwitchRow(
                title = "حماية تسريب الملفات والبيانات",
                description = "مراقبة الوصول إلى الصور والمستندات والتحميلات لحظر النسخ والتسريب الخارجي (CWE-927).",
                icon = Icons.Default.FileOpen,
                checked = isFileMonitorEnabled,
                onCheckedChange = { viewModel.setFileMonitor(it) }
            )

            MonitorSwitchRow(
                title = "الدرع الواقي الفوري للهاتف",
                description = "فحص متواصل في الخلفية للملفات والتطبيقات المضافة لحمايتك من تهديدات برمجيات الفدية والتروجانات.",
                icon = Icons.Default.GppGood,
                checked = isRealtimeAntivirusEnabled,
                onCheckedChange = { viewModel.setRealtimeAntivirus(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرشادات أمنية هامة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "لضمان عمل موديولات الحماية بأقصى كفاءة في الخلفية ومنع نظام الأندرويد من إغلاق تطبيق Bousseta Guard لتوفير الطاقة، يرجى تعطيل تحسين البطارية للخدمة الدائمة من إعدادات النظام وتفعيل الصلاحيات الكاملة للوصول للملفات واستخدام الكاميرا والميكروفون.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MonitorSwitchRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.77f),
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
