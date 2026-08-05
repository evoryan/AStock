package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FlowDiagram(
    tenantName: String,
    dbName: String,
    accentColor: Color,
    onShowGuideClick: () -> Unit
) {
    var activeStep by remember { mutableStateOf(1) }

    // Automatic pulsing step animator for the interactive flow
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            activeStep = if (activeStep < 4) activeStep + 1 else 1
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Arsitektur Multi-Tenant VPS",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Alur Kerja Koneksi Dinamis Real-Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
                IconButton(
                    onClick = onShowGuideClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = accentColor.copy(alpha = 0.2f),
                        contentColor = accentColor
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Show Source Code",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The Flow Path Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowStepItem(
                    stepNumber = 1,
                    title = "1. Kredensial Pengguna dikirim",
                    description = "Mobile client mengirim username & password ke VPS.",
                    isActive = activeStep == 1,
                    icon = Icons.Default.Smartphone,
                    accentColor = accentColor
                )

                FlowConnector()

                FlowStepItem(
                    stepNumber = 2,
                    title = "2. Master Lookup (konter_master)",
                    description = "Node.js mencari user di db master dan mengambil db_name: '$dbName'.",
                    isActive = activeStep == 2,
                    icon = Icons.Default.Storage,
                    accentColor = Color(0xFF38BDF8)
                )

                FlowConnector()

                FlowStepItem(
                    stepNumber = 3,
                    title = "3. Dynamic mysql.createPool()",
                    description = "Node.js menginisiasi pool koneksi ke database tenant '$dbName'.",
                    isActive = activeStep == 3,
                    icon = Icons.Default.Dns,
                    accentColor = Color(0xFFFBBF24)
                )

                FlowConnector()

                FlowStepItem(
                    stepNumber = 4,
                    title = "4. Transaksi Tenant Diproses",
                    description = "Selesai! Data stok '$tenantName' dimuat langsung.",
                    isActive = activeStep == 4,
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Cara Kerja Connection Pool Caching:",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sistem Node.js di VPS menyimpan cache objek pool (Map<String, Pool>) agar tidak membuat pool baru di setiap request. Query berikutnya langsung menggunakan pool cache sesuai tenant db_name sehingga performa tetap instan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FlowStepItem(
    stepNumber: Int,
    title: String,
    description: String,
    isActive: Boolean,
    icon: ImageVector,
    accentColor: Color
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) accentColor else Color(0x1AFFFFFF),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "BorderColor"
    )

    val containerBg by animateColorAsState(
        targetValue = if (isActive) Color(0xFF1A2238) else Color(0xFF111827),
        animationSpec = tween(300),
        label = "BgColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isActive) accentColor.copy(alpha = 0.2f) else Color(0x1AFFFFFF),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) accentColor else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) Color.White else Color.LightGray,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) Color.LightGray else Color.Gray,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun FlowConnector() {
    Column(
        modifier = Modifier.fillMaxWidth().height(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x4DFFFFFF), Color(0x1AFFFFFF))
                    )
                )
        )
    }
}
