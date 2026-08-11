package com.nova.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null

    private var novaActive by mutableStateOf(false)
    private var listening by mutableStateOf(false)
    private var lastCommand by mutableStateOf("Say \"Nova\"")

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListening()
            } else {
                lastCommand = "Microphone permission required"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NovaScreen(
                active = novaActive,
                listening = listening,
                text = lastCommand
            )
        }

        setupSpeechRecognizer()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            microphonePermission.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun setupSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            lastCommand = "Speech recognition unavailable"
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    listening = false
                }

                override fun onError(error: Int) {
                    listening = false

                    if (novaActive) {
                        startListening()
                    } else {
                        startListening()
                    }
                }

                override fun onResults(results: Bundle?) {

                    listening = false

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        matches?.firstOrNull()
                            ?.lowercase(Locale.getDefault())
                            ?: ""

                    handleSpeech(text)

                    startListening()
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )
    }

    private fun startListening() {

        val recognizer = speechRecognizer ?: return

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
            )
        }

        try {
            recognizer.startListening(intent)
        } catch (_: Exception) {
        }
    }

    private fun handleSpeech(text: String) {

        if (text.isBlank()) return

        lastCommand = text

        if (
            text.contains("nova") ||
            text.contains("noa")
        ) {

            if (
                text.contains("off") ||
                text.contains("ki") ||
                text.contains("kikapcsol")
            ) {
                novaActive = false
                lastCommand = "Nova OFF"
                return
            }

            novaActive = true
            lastCommand = "Nova ACTIVE"
            return
        }

        if (!novaActive) return

        when {

            text.contains("youtube") -> {
                openApp(
                    "com.google.android.youtube"
                )
            }

            text.contains("chrome") -> {
                openApp(
                    "com.android.chrome"
                )
            }

            text.contains("discord") -> {
                openApp(
                    "com.discord"
                )
            }

            text.contains("ki") -> {
                novaActive = false
                lastCommand = "Nova OFF"
            }
        }
    }

    private fun openApp(packageName: String) {

        val intent =
            packageManager.getLaunchIntentForPackage(
                packageName
            )

        if (intent != null) {
            startActivity(intent)
        }
    }

    override fun onDestroy() {

        speechRecognizer?.destroy()
        speechRecognizer = null

        super.onDestroy()
    }
}

@Composable
fun NovaScreen(
    active: Boolean,
    listening: Boolean,
    text: String
) {

    val transition =
        rememberInfiniteTransition(
            label = "NovaCore"
        )

    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    1200,
                    easing = EaseInOut
                ),
                repeatMode =
                    RepeatMode.Reverse
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

            val center =
                Offset(
                    size.width / 2f,
                    size.height / 2f
                )

            val radius =
                70.dp.toPx() *
                        if (active) pulse else 0.85f

            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            if (active) {
                                listOf(
                                    Color(0xFF00E5FF)
                                        .copy(alpha = 0.35f),
                                    Color(0xFF0066FF)
                                        .copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color.Gray
                                        .copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            },
                        center = center,
                        radius = radius * 2.8f
                    ),
                radius = radius * 2.8f,
                center = center
            )

            drawCircle(
                color =
                    if (active)
                        Color(0xFF00E5FF)
                    else
                        Color.DarkGray,
                radius = radius,
                center = center
            )

            drawCircle(
                color = Color.White,
                radius = radius * 0.13f,
                center =
                    Offset(
                        center.x - radius * 0.28f,
                        center.y - radius * 0.28f
                    )
            )
        }

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            modifier =
                Modifier.offset(y = 125.dp)
        ) {

            Text(
                text = "N O V A",
                color = Color.White,
                fontSize = 18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    when {
                        active -> "ACTIVE"
                        listening -> "LISTENING"
                        else -> "READY"
                    },
                color =
                    if (active)
                        Color(0xFF00E5FF)
                    else
                        Color.Gray,
                fontSize = 10.sp
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = text,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}
