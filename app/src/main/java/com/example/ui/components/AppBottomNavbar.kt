package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppScreen

@Composable
fun AppBottomNavbar(
    currentScreen: AppScreen,
    dashboardTab: Int,
    tenantAccent: Color,
    onNavigateTab: (AppScreen, Int) -> Unit,
    onScannerClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom Bar Background Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Navigation Items
                NavbarItem(
                    selected = currentScreen == AppScreen.Dashboard && dashboardTab == 1,
                    onClick = { onNavigateTab(AppScreen.Dashboard, 1) },
                    icon = Icons.Default.Home,
                    label = "Beranda",
                    tenantAccent = tenantAccent
                )

                NavbarItem(
                    selected = currentScreen == AppScreen.Dashboard && dashboardTab == 2,
                    onClick = { onNavigateTab(AppScreen.Dashboard, 2) },
                    icon = Icons.Default.Inventory,
                    label = "Stok",
                    tenantAccent = tenantAccent
                )

                // Space for raised scanner button in the middle
                Spacer(modifier = Modifier.width(52.dp))

                // Right Navigation Items
                NavbarItem(
                    selected = currentScreen == AppScreen.Dashboard && dashboardTab == 3,
                    onClick = { onNavigateTab(AppScreen.Dashboard, 3) },
                    icon = Icons.Default.PointOfSale,
                    label = "POS",
                    tenantAccent = tenantAccent
                )

                NavbarItem(
                    selected = currentScreen == AppScreen.Settings,
                    onClick = { onNavigateTab(AppScreen.Settings, 0) },
                    icon = Icons.Default.Settings,
                    label = "Pengaturan",
                    tenantAccent = tenantAccent
                )
            }
        }

        // Center Raised Scanner Floating Action Button (agak naik di tengah navbar)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp)
                .testTag("navbar_scanner_button")
        ) {
            FloatingActionButton(
                onClick = onScannerClick,
                shape = CircleShape,
                containerColor = tenantAccent,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Barcode",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavbarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    tenantAccent: Color
) {
    val activeColor = tenantAccent
    val inactiveColor = Color.Gray

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (selected) activeColor else inactiveColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
