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

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

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

                    // Restart listening automatically.
                    startListening()
                }

                override fun onResults(results: Bundle?) {

                    listening = false

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        matches
                            ?.firstOrNull()
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

        val intent =
            Intent(
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
                    5
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

        val normalized =
            text
                .lowercase(Locale.getDefault())
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ö", "o")
                .replace("ő", "o")
                .replace("ú", "u")
                .replace("ü", "u")
                .replace("ű", "u")

        /*
         * NOVA ACTIVATION
         */

        val activationWords = listOf(
            "nova",
            "noa",
            "nóva",
            "hey nova",
            "ok nova",
            "okay nova"
        )

        val activated =
            activationWords.any {
                normalized.contains(it)
            }

        if (activated) {

            val turningOff =
                normalized.contains("off") ||
                normalized.contains("ki") ||
                normalized.contains("kikapcsol")

            if (turningOff) {
                novaActive = false
                lastCommand = "Nova OFF"
                return
            }

            novaActive = true

            /*
             * If there is an app command in the same sentence,
             * process it immediately.
             */
            val command =
                normalized
                    .replace("nova", "")
                    .replace("noa", "")
                    .replace("hey", "")
                    .replace("ok", "")
                    .replace("okay", "")
                    .trim()

            if (command.isNotBlank()) {
                openAppFromCommand(command)
            } else {
                lastCommand = "Nova ACTIVE"
            }

            return
        }

        /*
         * NOVA MUST BE ACTIVE FOR APP COMMANDS
         */

        if (!novaActive) return

        openAppFromCommand(normalized)
    }

    private fun openAppFromCommand(command: String) {

        var cleaned =
            command
                .lowercase(Locale.getDefault())
                .trim()

        val wordsToRemove = listOf(
            "nyisd meg",
            "nyisd ki",
            "inditsd el",
            "inditsd",
            "nyisd",
            "meg",
            "el",
            "az",
            "a",
            "appot",
            "alkalmazast",
            "alkalmazást"
        )

        for (word in wordsToRemove) {
            cleaned =
                cleaned.replace(word, "")
        }

        cleaned =
            cleaned
                .replace("spotifyt", "spotify")
                .replace("youtubet", "youtube")
                .replace("discordot", "discord")
                .replace("chromot", "chrome")
                .replace("robloxot", "roblox")
                .replace("gmailt", "gmail")
                .replace("tiktokot", "tiktok")
                .replace("mapset", "maps")
                .replace("kamerat", "kamera")
                .replace("fotokat", "fotok")
                .trim()

        if (cleaned.isBlank()) return

        val apps =
            packageManager
                .getInstalledApplications(
                    PackageManager.GET_META_DATA
                )

        var bestPackage: String? = null
        var bestScore = 0

        for (app in apps) {

            val label =
                packageManager
                    .getApplicationLabel(app)
                    .toString()
                    .lowercase(Locale.getDefault())

            val score =
                calculateMatchScore(
                    cleaned,
                    label
                )

            if (score > bestScore) {
                bestScore = score
                bestPackage = app.packageName
            }
        }

        if (
            bestPackage != null &&
            bestScore >= 60
        ) {

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        bestPackage!!
                    )

            if (launchIntent != null) {

                lastCommand =
                    "Opening: $cleaned"

                try {
                    startActivity(launchIntent)
                    return
                } catch (_: Exception) {
                }
            }
        }

        lastCommand =
            "App not found: $cleaned"
    }

    private fun calculateMatchScore(
        command: String,
        appName: String
    ): Int {

        if (command == appName) {
            return 100
        }

        if (appName.contains(command)) {
            return 90
        }

        if (command.contains(appName)) {
            return 85
        }

        val commandWords =
            command
                .split(" ")
                .filter { it.length >= 2 }

        if (commandWords.isEmpty()) {
            return 0
        }

        var matched = 0

        for (word in commandWords) {
            if (appName.contains(word)) {
                matched++
            }
        }

        return if (matched > 0) {
            (matched.toFloat() /
                    commandWords.size *
                    80).toInt()
        } else {
            0
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
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        contentAlignment =
            Alignment.Center
    ) {

        androidx.compose.foundation.Canvas(
            modifier =
                Modifier.fillMaxSize()
        ) {

            val center =
                androidx.compose.ui.geometry.Offset(
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
                    androidx.compose.ui.geometry.Offset(
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
