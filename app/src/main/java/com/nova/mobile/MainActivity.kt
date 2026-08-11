package com.nova.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NovaScreen()
        }
    }
}

@Composable
fun NovaScreen() {

    val infiniteTransition = rememberInfiniteTransition(
        label = "NovaCore"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            val radius = 70.dp.toPx() * pulse

            // Külső aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.30f),
                        Color(0xFF0066FF).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 2.8f
                ),
                radius = radius * 2.8f,
                center = center
            )

            // Külső gyűrű
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.55f),
                radius = radius * 1.55f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx()
                )
            )

            // Nova mag
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFF8FFFFF),
                        Color(0xFF00C8FF),
                        Color(0xFF0055FF),
                        Color(0xFF00152E)
                    ),
                    center = Offset(
                        center.x - radius * 0.25f,
                        center.y - radius * 0.25f
                    ),
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Belső fény
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = radius * 0.13f,
                center = Offset(
                    center.x - radius * 0.28f,
                    center.y - radius * 0.28f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 125.dp)
        ) {

            Text(
                text = "N O V A",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "READY",
                color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                fontSize = 10.sp,
                letterSpacing = 3.sp
            )
        }
    }
}
