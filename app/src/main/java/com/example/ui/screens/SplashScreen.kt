package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("Menghubungkan ke VPS...") }

    // Dynamic animated values
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    val opacityAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "LogoOpacity"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        
        // Progress of connection steps
        delay(800)
        loadingText = "Membaca konter_master..."
        delay(800)
        loadingText = "Mengautentikasi Sesi VPS..."
        delay(600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background light aura
        Box(
            modifier = Modifier
                .size(300.dp)
                .alpha(0.08f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFF59E0B), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(scaleAnim)
                .alpha(opacityAnim)
        ) {
            // Elegant Handcrafted Animated Logo in Canvas
            Canvas(modifier = Modifier.size(120.dp)) {
                val center = size.width / 2
                val h = size.height
                
                // Let's draw the 3D isometric box
                // Coordinates for points
                val pTop = Offset(center, 25f)
                val pRight = Offset(center + 45f, 45f)
                val pBottom = Offset(center, 65f)
                val pLeft = Offset(center - 45f, 45f)
                
                val pBottomLeft = Offset(center - 45f, 85f)
                val pBottomRight = Offset(center + 45f, 85f)
                val pBottomCenter = Offset(center, 105f)

                // 1. Top face (Emerald Accent)
                val topPath = Path().apply {
                    moveTo(pTop.x, pTop.y)
                    lineTo(pRight.x, pRight.y)
                    lineTo(pBottom.x, pBottom.y)
                    lineTo(pLeft.x, pLeft.y)
                    close()
                }
                drawPath(topPath, color = Color(0xFFF59E0B))

                // 2. Left Face (Medium Emerald)
                val leftPath = Path().apply {
                    moveTo(pLeft.x, pLeft.y)
                    lineTo(pBottom.x, pBottom.y)
                    lineTo(pBottomCenter.x, pBottomCenter.y)
                    lineTo(pBottomLeft.x, pBottomLeft.y)
                    close()
                }
                drawPath(leftPath, color = Color(0xFF059669))

                // 3. Right Face (Mint Light)
                val rightPath = Path().apply {
                    moveTo(pBottom.x, pBottom.y)
                    lineTo(pRight.x, pRight.y)
                    lineTo(pBottomRight.x, pBottomRight.y)
                    lineTo(pBottomCenter.x, pBottomCenter.y)
                    close()
                }
                drawPath(rightPath, color = Color(0xFF34D399))

                // 4. Gold Upward arrow representing high value stock growth
                val arrowPath = Path().apply {
                    moveTo(center - 15f, 45f)
                    lineTo(center + 15f, 15f)
                    lineTo(center + 15f, 30f)
                    moveTo(center + 15f, 15f)
                    lineTo(center, 15f)
                }
                
                // Drawing dynamic gold arrow on top
                val arrowStrokeWidth = 8f
                drawPath(
                    path = Path().apply {
                        moveTo(center - 10f, 40f)
                        lineTo(center + 15f, 15f)
                        lineTo(center + 15f, 28f)
                    },
                    color = Color(0xFFF59E0B),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = arrowStrokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
                
                drawPath(
                    path = Path().apply {
                        moveTo(center + 15f, 15f)
                        lineTo(center + 2f, 15f)
                    },
                    color = Color(0xFFF59E0B),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = arrowStrokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ASTOCK Typography
            Text(
                text = "ASTOCK",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.SansSerif
            )

            Text(
                text = "MULTI-TENANT STOCK CONTROL",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Connection loading feedback
            CircularProgressIndicator(
                color = Color(0xFFF59E0B),
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = loadingText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                letterSpacing = 1.sp
            )
        }

        // Footer Attribution
        Text(
            text = "Powered by Node.JS & MySQL VPS",
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
