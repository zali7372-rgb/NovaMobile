package com.nova.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startNovaService()
            }
        }

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * MICROPHONE PERMISSION
         */

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            microphonePermission.launch(
                Manifest.permission.RECORD_AUDIO
            )
        } else {
            startNovaService()
        }

        /*
         * NOTIFICATION PERMISSION
         */

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

        /*
         * NOVA UI
         */

        setContent {
            NovaScreen()
        }
    }

    private fun startNovaService() {

        val serviceIntent =
            Intent(
                this,
                NovaService::class.java
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            startForegroundService(
                serviceIntent
            )
        } else {
            startService(
                serviceIntent
            )
        }
    }
}

/*
 * NOVA UI
 */

@Composable
fun NovaScreen() {

    val transition =
        rememberInfiniteTransition(
            label = "NovaCore"
        )

    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        1200,
                        easing = EaseInOut
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "Pulse"
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        contentAlignment =
            Alignment.Center
    ) {

        Canvas(
            modifier =
                Modifier.fillMaxSize()
        ) {

            val center =
                androidx.compose.ui.geometry.Offset(
                    size.width / 2f,
                    size.height / 2f
                )

            val radius =
                70.dp.toPx() * pulse

            /*
             * OUTER GLOW
             */

            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                Color(0xFF00E5FF)
                                    .copy(alpha = 0.35f),

                                Color(0xFF0066FF)
                                    .copy(alpha = 0.15f),

                                Color.Transparent
                            ),

                        center = center,

                        radius =
                            radius * 2.8f
                    ),

                radius =
                    radius * 2.8f,

                center = center
            )

            /*
             * CORE
             */

            drawCircle(
                color =
                    Color(0xFF00E5FF),

                radius =
                    radius,

                center =
                    center
            )

            /*
             * SMALL WHITE LIGHT
             */

            drawCircle(
                color =
                    Color.White,

                radius =
                    radius * 0.13f,

                center =
                    androidx.compose.ui.geometry.Offset(
                        center.x -
                            radius * 0.28f,

                        center.y -
                            radius * 0.28f
                    )
            )
        }

        /*
         * TEXT
         */

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,

            modifier =
                Modifier.offset(
                    y = 125.dp
                )
        ) {

            Text(
                text = "N O V A",

                color =
                    Color.White,

                fontSize =
                    18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = "ACTIVE",

                color =
                    Color(0xFF00E5FF),

                fontSize =
                    10.sp
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "Background service running",

                color =
                    Color.Gray,

                fontSize =
                    10.sp
            )
        }
    }
}
