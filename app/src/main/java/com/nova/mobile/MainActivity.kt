package com.nova.mobile

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var debugText by mutableStateOf(
        """
        🟡 Nova Debug
        Service: várakozás...
        🎤 Mikrofon: -
        👂 Hallotta: -
        🧠 Parancs: -
        ⚙️ Művelet: -
        ❌ Hiba: -
        """.trimIndent()
    )

    private val debugReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    intent?.action ==
                    NovaService.ACTION_DEBUG
                ) {

                    val text =
                        intent.getStringExtra(
                            NovaService.EXTRA_DEBUG_TEXT
                        )

                    if (
                        !text.isNullOrBlank()
                    ) {

                        debugText = text
                    }
                }
            }
        }

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startNovaService()
            } else {

                debugText =
                    """
                    🔴 Nova Debug

                    🎤 Mikrofon: ❌ NINCS ENGEDÉLY

                    Engedélyezd a mikrofont,
                    hogy Nova hallhasson.
                    """.trimIndent()
            }
        }

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            startNovaService()
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        /*
         * DEBUG RECEIVER
         */

        val filter =
            IntentFilter(
                NovaService.ACTION_DEBUG
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            registerReceiver(
                debugReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )

        } else {

            @Suppress("DEPRECATION")
            registerReceiver(
                debugReceiver,
                filter
            )
        }

        /*
         * UI
         */

        setContent {

            NovaDebugScreen(
                debugText = debugText
            )
        }

        /*
         * MICROPHONE
         */

        checkMicrophonePermission()

        /*
         * NOTIFICATION
         */

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {

                notificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun checkMicrophonePermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {

            microphonePermission.launch(
                Manifest.permission.RECORD_AUDIO
            )

        } else {

            startNovaService()
        }
    }

    private fun startNovaService() {

        val intent =
            Intent(
                this,
                NovaService::class.java
            )

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                startForegroundService(
                    intent
                )

            } else {

                @Suppress("DEPRECATION")
                startService(
                    intent
                )
            }

        } catch (
            e: Exception
        ) {

            debugText =
                """
                🔴 Nova Debug

                ❌ Service indítási hiba:

                ${e.message}
                """.trimIndent()
        }
    }

    override fun onDestroy() {

        try {
            unregisterReceiver(
                debugReceiver
            )
        } catch (
            _: Exception
        ) {
        }

        super.onDestroy()
    }
}

/*
 * DEBUG UI
 */

@Composable
fun NovaDebugScreen(
    debugText: String
) {

    val scrollState =
        rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF080808)
                )
                .padding(16.dp)
                .verticalScroll(
                    scrollState
                )
    ) {

        /*
         * TITLE
         */

        Text(
            text = "NOVA",
            color =
                Color(0xFF00E5FF),
            fontSize = 30.sp
        )

        Text(
            text = "DEBUG CONSOLE",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        /*
         * STATUS
         */

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF111111)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text = "LIVE STATUS",
                    color =
                        Color(0xFF00E5FF),
                    fontSize = 15.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        debugText,
                    color =
                        Color.White,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        /*
         * RAW DEBUG
         */

        Text(
            text = "RAW DEBUG OUTPUT",
            color =
                Color(0xFF00E5FF),
            fontSize = 14.sp
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            color =
                Color.Black
        ) {

            Text(
                text =
                    debugText,
                modifier =
                    Modifier.padding(14.dp),
                color =
                    Color(0xFFB8FFB8),
                fontSize = 12.sp
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        /*
         * COMMAND HELP
         */

        Text(
            text =
                "NOVA COMMAND DEBUG",
            color =
                Color(0xFF00E5FF),
            fontSize = 14.sp
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                """
                🎤 Mikrofon
                → mutatja, hogy a felismerő aktív-e

                👂 Hallotta
                → amit a telefon felismert

                🧠 Parancs
                → amit Nova a "Nova" megszólításból
                  levágva feldolgoz

                ⚙️ Művelet
                → amit végre akar hajtani

                ❌ Hiba
                → speech/service hiba
                """.trimIndent(),
            color =
                Color.LightGray,
            fontSize = 12.sp
        )
    }
}
