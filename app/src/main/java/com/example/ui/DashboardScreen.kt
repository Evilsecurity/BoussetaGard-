package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: AppViewModel, onNavigateToScan: () -> Unit) {
    val activeThreats by viewModel.activeThreats.collectAsState()
    val logs by viewModel.allLogs.collectAsState()
    val isPortMonitorEnabled by viewModel.isPortMonitorEnabled.collectAsState()
    val isCameraMonitorEnabled by viewModel.isCameraMonitorEnabled.collectAsState()
    val isFileMonitorEnabled by viewModel.isFileMonitorEnabled.collectAsState()

    val totalLogsCount = logs.size
    val isSystemSecure = activeThreats.isEmpty()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bousseta Guard 🛡️", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Security Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemSecure) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isSystemSecure) Icons.Default.GppGood else Icons.Default.GppBad,
                        contentDescription = null,
                        tint = if (isSystemSecure) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isSystemSecure) "حالة الأمان: محمي" else "تحذير: مخاطر أمنية مكتشفة!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemSecure) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Text(
                        text = if (isSystemSecure) "جميع دروع المراقبة النشطة تعمل في الخلفية بكفاءة" else "تم رصد ${activeThreats.size} تهديدات نشطة تؤثر على خصوصية الهاتف والبيانات",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = if (isSystemSecure) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onNavigateToScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSystemSecure) Color(0xFF2E7D32) else Color(0xFFC62828)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isSystemSecure) "فحص الآن" else "مراجعة وحذف التهديدات", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Stats Stats Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "التهديدات الفعالة",
                    value = "${activeThreats.size}",
                    color = if (isSystemSecure) Color(0xFF2E7D32) else Color(0xFFC62828),
                    icon = Icons.Default.Security
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "إجمالي السجلات",
                    value = "$totalLogsCount",
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.ListAlt
                )
            }

            // Vulnerability Coverages Title
            Text(
                "تغطية وحماية نقاط الضعف النشطة (CWE):",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            // Dynamic interactive list of vulnerability coverages
            CweItem(
                id = "V-01",
                name = "استغلال صلاحية الكاميرا (CWE-284)",
                description = "مراقبة live واستقصاء استخدام الكاميرا ومقاطعة عمليات التجسس فورياً.",
                active = isCameraMonitorEnabled
            )
            CweItem(
                id = "V-02",
                name = "منافذ خلفية مفتوحة (CWE-276)",
                description = "مسح دوري للمنافذ (مثل 5555 لـ ADB) وكشف التسلل والتحكم عن بُعد.",
                active = isPortMonitorEnabled
            )
            CweItem(
                id = "V-03 & V-04",
                name = "تسريب وسرقة الملفات (CWE-921 / CWE-927)",
                description = "مراقبة مستمرة للمجلدات الخاصة (DCIM, Documents, Downloads...) لحذر النقل الخارجي.",
                active = isFileMonitorEnabled
            )
            CweItem(
                id = "V-05",
                name = "ميكروفون خلفي (CWE-1119)",
                description = "رصد عمليات التجسس وبث الصوت غير المصرح به بدون إشعار المستخدم.",
                active = isCameraMonitorEnabled
            )
            CweItem(
                id = "V-07",
                name = "تطبيقات تصيدية (FakeBank/Trojans)",
                description = "فحص سلوكي مستمر للكشف عن محاولات سلب كلمات المرور أو انتحال واجهات البنوك.",
                active = true
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun CweItem(
    id: String,
    name: String,
    description: String,
    active: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (active) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = id,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (active) Color(0xFF2E7D32) else Color(0xFFC62828), shape = RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (active) "فعالة" else "معطلة",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}
