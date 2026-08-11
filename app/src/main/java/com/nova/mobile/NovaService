package com.nova.mobile

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import java.util.Locale

class NovaService : Service() {

    private var recognizer: SpeechRecognizer? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification =
            NotificationCompat.Builder(this, "nova_service")
                .setContentTitle("Nova")
                .setContentText("Nova is running in the background")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()

        startForeground(1001, notification)

        setupRecognizer()
        startListening()
    }

    private fun setupRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return
        }

        recognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    startListening()
                }

                override fun onResults(results: Bundle?) {

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
                    false
                )
            }

        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
        }
    }

    private fun handleCommand(text: String) {

        if (!text.contains("nova") &&
            !text.contains("noa")
        ) {
            return
        }

        /*
         * NOVA OFF
         */

        if (
            text.contains("off") ||
            text.contains("ki") ||
            text.contains("állj") ||
            text.contains("allj")
        ) {
            stopSelf()
            return
        }

        /*
         * TIMER
         */

        if (
            text.contains("időzítő") ||
            text.contains("idozito")
        ) {
            val minutes =
                Regex("""(\d+)""")
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.toLongOrNull()

            if (minutes != null) {

                val intent =
                    Intent(
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

                startActivity(intent)
            }

            return
        }

        /*
         * WEB SEARCH
         */

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

                val url =
                    "https://www.google.com/search?q=" +
                        java.net.URLEncoder
                            .encode(
                                query,
                                "UTF-8"
                            )

                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                startActivity(intent)
            }

            return
        }

        /*
         * OPEN APPS
         */

        if (
            text.contains("nyisd") ||
            text.contains("nyisd meg") ||
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

            openApplication(appName)

            return
        }
    }

    private fun openApplication(name: String) {

        val apps =
            packageManager
                .getInstalledApplications(
                    0
                )

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

                    startActivity(
                        launchIntent
                    )

                    return
                }
            }
        }
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

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}
