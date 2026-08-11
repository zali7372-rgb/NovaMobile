package com.nova.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class NovaService : Service(), TextToSpeech.OnInitListener {

    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isSpeaking = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = NotificationCompat.Builder(
            this,
            "nova_service"
        )
            .setContentTitle("Nova")
            .setContentText("Nova fut a háttérben")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        textToSpeech = TextToSpeech(this, this)

        setupRecognizer()
        startListening()
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            textToSpeech?.language = Locale.getDefault()

            textToSpeech?.setSpeechRate(1.0f)
            textToSpeech?.setPitch(1.0f)
        }
    }

    private fun speak(text: String) {

        if (text.isBlank()) {
            return
        }

        isSpeaking = true

        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "nova_speech"
        )

        android.os.Handler(mainLooper).postDelayed(
            {
                isSpeaking = false
                startListening()
            },
            1500
        )
    }

    private fun setupRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {
                }

                override fun onBeginningOfSpeech() {
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }

                override fun onEndOfSpeech() {
                }

                override fun onError(
                    error: Int
                ) {

                    if (!isSpeaking) {
                        android.os.Handler(mainLooper).postDelayed(
                            {
                                startListening()
                            },
                            500
                        )
                    }
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        matches
                            ?.firstOrNull()
                            ?.lowercase(Locale.getDefault())
                            ?: ""

                    handleCommand(text)

                    if (!isSpeaking) {
                        startListening()
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )
    }

    private fun startListening() {

        if (isSpeaking) {
            return
        }

        if (recognizer == null) {
            return
        }

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
                false
            )
        }

        try {

            recognizer?.startListening(intent)

        } catch (_: Exception) {
        }
    }

    private fun handleCommand(text: String) {

        if (
            !text.contains("nova") &&
            !text.contains("noa")
        ) {
            return
        }

        val command = text
            .replace("nova", "")
            .replace("noa", "")
            .trim()

        // CSAK A „NOVA” MEGSZÓLÍTÁS

        if (command.isBlank()) {

            speak(
                listOf(
                    "Igen?",
                    "Hallgatlak.",
                    "Miben segíthetek?",
                    "Parancs?",
                    "Itt vagyok."
                ).random()
            )

            return
        }

        // NOVA LEÁLLÍTÁSA

        if (
            text.contains("off") ||
            text.contains("ki") ||
            text.contains("állj") ||
            text.contains("allj")
        ) {

            speak("Rendben.")

            android.os.Handler(mainLooper).postDelayed(
                {
                    stopSelf()
                },
                1000
            )

            return
        }

        // IDŐZÍTŐ

        if (
            text.contains("időzítő") ||
            text.contains("idozito")
        ) {

            val minutes =
                Regex("""(\d+)""")
                    .find(text)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()

            if (minutes != null) {

                speak(
                    "$minutes perces időzítőt állítok be."
                )

                val intent = Intent(
                    android.provider.AlarmClock.ACTION_SET_TIMER
                ).apply {

                    putExtra(
                        android.provider.AlarmClock.EXTRA_LENGTH,
                        (minutes * 60).toInt()
                    )

                    putExtra(
                        android.provider.AlarmClock.EXTRA_SKIP_UI,
                        false
                    )

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK
                }

                android.os.Handler(mainLooper).postDelayed(
                    {
                        try {
                            startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    1000
                )
            } else {

                speak(
                    "Hány perces időzítőt állítsak be?"
                )
            }

            return
        }

        // GOOGLE KERESÉS

        if (
            text.contains("keress") ||
            text.contains("keresés") ||
            text.contains("kereses")
        ) {

            val query =
                text
                    .replace("nova", "")
                    .replace("noa", "")
                    .replace("keress rá", "")
                    .replace("keress ra", "")
                    .replace("keress", "")
                    .trim()

            if (query.isNotBlank()) {

                speak("Rákeresek.")

                val url =
                    "https://www.google.com/search?q=" +
                        java.net.URLEncoder.encode(
                            query,
                            "UTF-8"
                        )

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                ).apply {

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK
                }

                android.os.Handler(mainLooper).postDelayed(
                    {
                        try {
                            startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    1000
                )

            } else {

                speak(
                    "Mit keressek?"
                )
            }

            return
        }

        // APP MEGNYITÁSA

        if (
            text.contains("nyisd") ||
            text.contains("indítsd") ||
            text.contains("inditsd")
        ) {

            val appName =
                text
                    .replace("nova", "")
                    .replace("noa", "")
                    .replace("nyisd meg", "")
                    .replace("nyisd", "")
                    .replace("indítsd el", "")
                    .replace("inditsd el", "")
                    .replace("indítsd", "")
                    .replace("inditsd", "")
                    .trim()

            if (appName.isNotBlank()) {

                speak(
                    "Megnyitom."
                )

                android.os.Handler(mainLooper).postDelayed(
                    {
                        openApplication(appName)
                    },
                    1000
                )

            } else {

                speak(
                    "Melyik alkalmazást nyissam meg?"
                )
            }

            return
        }

        // ISMERETLEN PARANCS

        speak(
            "Ezt a parancsot még nem ismerem."
        )
    }

    private fun openApplication(
        name: String
    ) {

        val apps =
            packageManager.getInstalledApplications(0)

        for (app in apps) {

            val label =
                packageManager
                    .getApplicationLabel(app)
                    .toString()

            if (
                label.equals(
                    name,
                    ignoreCase = true
                ) ||
                label.contains(
                    name,
                    ignoreCase = true
                )
            ) {

                val launchIntent =
                    packageManager
                        .getLaunchIntentForPackage(
                            app.packageName
                        )

                if (launchIntent != null) {

                    launchIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK

                    try {

                        startActivity(
                            launchIntent
                        )

                    } catch (_: Exception) {
                    }

                    return
                }
            }
        }

        speak(
            "Nem találom ezt az alkalmazást."
        )
    }

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                "nova_service",
                "Nova Background",
                NotificationManager.IMPORTANCE_LOW
            )

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    override fun onDestroy() {

        recognizer?.destroy()
        recognizer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}
