package com.nova.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.AlarmClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.net.URLEncoder
import java.util.Locale

class NovaService : Service(), TextToSpeech.OnInitListener {

    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private var isListening = false
    private var isSpeaking = false

    companion object {
        const val ACTION_DEBUG = "com.nova.mobile.DEBUG"
        const val EXTRA_DEBUG_TEXT = "debug_text"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification =
            NotificationCompat.Builder(
                this,
                "nova_service"
            )
                .setContentTitle("Nova")
                .setContentText("Nova figyel a háttérben")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()

        startForeground(1001, notification)

        sendDebug(
            """
            🟢 Service: FUT
            🎤 Mikrofon: inicializálás...
            👂 Hallotta: -
            🧠 Parancs: -
            ⚙️ Művelet: -
            ❌ Hiba: -
            """.trimIndent()
        )

        textToSpeech = TextToSpeech(this, this)

        setupRecognizer()
        startListening()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            textToSpeech?.language = Locale("hu", "HU")
            textToSpeech?.setSpeechRate(1.0f)

            sendDebug(
                """
                🟢 Service: FUT
                🎤 Mikrofon: ENGEDÉLYEZVE
                👂 Hallotta: -
                🧠 Parancs: -
                ⚙️ Művelet: -
                ❌ Hiba: -
                """.trimIndent()
            )
        } else {
            sendDebug(
                """
                🔴 TTS HIBA
                ❌ Text-to-Speech inicializálás sikertelen
                """.trimIndent()
            )
        }
    }

    private fun setupRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            sendDebug(
                """
                🔴 Service: FUT
                🎤 Mikrofon: ❌
                👂 Hallotta: -
                🧠 Parancs: -
                ⚙️ Művelet: -
                ❌ Hiba:
                Speech recognition nem érhető el
                """.trimIndent()
            )

            return
        }

        recognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {
                    isListening = true

                    sendDebug(
                        """
                        🟢 Service: FUT
                        🎤 Mikrofon: AKTÍV
                        👂 Hallotta: -
                        🧠 Parancs: -
                        ⚙️ Művelet: -
                        ❌ Hiba: -
                        """.trimIndent()
                    )
                }

                override fun onBeginningOfSpeech() {

                    sendDebug(
                        """
                        🟢 Service: FUT
                        🎤 Mikrofon: BESZÉDET ÉSZLEL
                        👂 Hallotta: ...
                        🧠 Parancs: -
                        ⚙️ Művelet: -
                        ❌ Hiba: -
                        """.trimIndent()
                    )
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

                    isListening = false

                    sendDebug(
                        """
                        🟡 Service: FUT
                        🎤 Mikrofon: FELVÉTEL VÉGE
                        👂 Hallotta: feldolgozás...
                        🧠 Parancs: -
                        ⚙️ Művelet: -
                        ❌ Hiba: -
                        """.trimIndent()
                    )
                }

                override fun onError(
                    error: Int
                ) {

                    isListening = false

                    val errorText =
                        getSpeechErrorText(error)

                    sendDebug(
                        """
                        🔴 Service: FUT
                        🎤 Mikrofon: HIBA
                        👂 Hallotta: -
                        🧠 Parancs: -
                        ⚙️ Művelet: -
                        ❌ Hiba:
                        $error ($errorText)
                        """.trimIndent()
                    )

                    restartListening(1000)
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    isListening = false

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        matches
                            ?.firstOrNull()
                            ?.lowercase(Locale.getDefault())
                            ?: ""

                    sendDebug(
                        """
                        🟢 Service: FUT
                        🎤 Mikrofon: FELISMERÉS KÉSZ
                        👂 Hallotta:
                        "$text"
                        🧠 Parancs: feldolgozás...
                        ⚙️ Művelet: -
                        ❌ Hiba: -
                        """.trimIndent()
                    )

                    handleCommand(text)
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

        if (isSpeaking) return
        if (isListening) return

        if (recognizer == null) {
            setupRecognizer()

            if (recognizer == null) {
                return
            }
        }

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
                    "hu-HU"
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    "hu-HU"
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    5
                )
            }

        try {

            isListening = true

            recognizer?.startListening(intent)

            sendDebug(
                """
                🟢 Service: FUT
                🎤 Mikrofon: FIGYEL
                👂 Hallotta: -
                🧠 Parancs: -
                ⚙️ Művelet: -
                ❌ Hiba: -
                """.trimIndent()
            )

        } catch (e: Exception) {

            isListening = false

            sendDebug(
                """
                🔴 Service: FUT
                🎤 Mikrofon: NEM INDULT
                👂 Hallotta: -
                🧠 Parancs: -
                ⚙️ Művelet: -
                ❌ Hiba:
                ${e.message}
                """.trimIndent()
            )

            restartListening(1500)
        }
    }

    private fun restartListening(
        delay: Long
    ) {

        Handler(mainLooper).postDelayed(
            {
                if (!isSpeaking) {
                    startListening()
                }
            },
            delay
        )
    }

    private fun handleCommand(
        text: String
    ) {

        if (text.isBlank()) {

            sendDebug(
                """
                🟡 Service: FUT
                🎤 Mikrofon: AKTÍV
                👂 Hallotta: ""
                🧠 Parancs: nincs
                ⚙️ Művelet: nincs
                ❌ Hiba: üres felismerés
                """.trimIndent()
            )

            restartListening(500)
            return
        }

        val normalized =
            text.lowercase(Locale.getDefault())

        /*
         * NOVA MEGSZÓLÍTÁSOK
         *
         * Nem csak egyetlen "Nova" létezik.
         * A felismerő néha érdekes dolgokat hall,
         * mert természetesen az emberiség feltalált
         * hangfelismerést, mielőtt megtanította volna rendesen hallani.
         */

        val novaNames =
            listOf(
                "nova",
                "noa",
                "novaa",
                "novi",
                "novus",
                "novacska",
                "nova ai",
                "novaáj",
                "nova i",
                "hey nova",
                "hey noa",
                "he nova",
                "he noa",
                "szia nova",
                "szia noa",
                "hallod nova",
                "hallod noa",
                "figyelj nova",
                "figyelj noa",
                "nova figyelj",
                "noa figyelj",
                "nova légyszi",
                "nova legyszi",
                "nova légy szíves",
                "nova legy szives",
                "noa légyszi",
                "noa legyszi",
                "noa légy szíves",
                "noa legy szives",
                "nóva",
                "nóva figyelj",
                "nóva légyszi"
            )

        val triggered =
            novaNames.any {
                normalized.contains(it)
            }

        if (!triggered) {

            sendDebug(
                """
                🟡 Service: FUT
                🎤 Mikrofon: AKTÍV
                👂 Hallotta:
                "$text"
                🧠 Parancs:
                Nincs Nova megszólítás
                ⚙️ Művelet: nincs
                ❌ Hiba: -
                """.trimIndent()
            )

            restartListening(500)
            return
        }

        var command = normalized

        novaNames.forEach {
            command = command.replace(it, "")
        }

        command =
            command
                .replace(",", "")
                .replace(".", "")
                .trim()

        sendDebug(
            """
            🟢 Service: FUT
            🎤 Mikrofon: AKTÍV
            👂 Hallotta:
            "$text"
            🧠 Parancs:
            "$command"
            ⚙️ Művelet: feldolgozás...
            ❌ Hiba: -
            """.trimIndent()
        )

        /*
         * CSAK "NOVA"
         */

        if (command.isBlank()) {

            speak("Igen?")
            return
        }

        /*
         * LEÁLLÍTÁS
         */

        if (
            command.contains("állj") ||
            command.contains("allj") ||
            command.contains("állj le") ||
            command.contains("allj le") ||
            command.contains("kapcsold ki") ||
            command.contains("kapcsold le") ||
            command.contains("leállítás") ||
            command.contains("leallitas") ||
            command.contains("leállj") ||
            command.contains("leallj") ||
            command.contains("stop") ||
            command.contains("off") ||
            command.contains("állj meg") ||
            command.contains("allj meg")
        ) {

            debugAction(
                text,
                command,
                "Nova leállítása"
            )

            speak("Rendben, leállok.")

            Handler(mainLooper).postDelayed(
                {
                    stopSelf()
                },
                1200
            )

            return
        }

        /*
         * IDŐZÍTŐ
         */

        if (
            command.contains("időzítő") ||
            command.contains("idozito") ||
            command.contains("timer") ||
            command.contains("visszaszámlálás") ||
            command.contains("visszaszamlalas")
        ) {

            val minutes =
                Regex("""(\d+)""")
                    .find(command)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()

            if (minutes != null) {

                debugAction(
                    text,
                    command,
                    "$minutes perces időzítő"
                )

                speak(
                    "$minutes perces időzítőt állítok be."
                )

                val intent =
                    Intent(
                        AlarmClock.ACTION_SET_TIMER
                    ).apply {

                        putExtra(
                            AlarmClock.EXTRA_LENGTH,
                            (minutes * 60).toInt()
                        )

                        putExtra(
                            AlarmClock.EXTRA_SKIP_UI,
                            false
                        )

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                Handler(mainLooper).postDelayed(
                    {
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            sendDebug(
                                """
                                🔴 Időzítő hiba:
                                ${e.message}
                                """.trimIndent()
                            )
                        }
                    },
                    900
                )

                return
            }

            speak("Hány percre állítsam az időzítőt?")
            return
        }

        /*
         * GOOGLE KERESÉS
         */

        if (
            command.contains("keress") ||
            command.contains("keresés") ||
            command.contains("kereses") ||
            command.contains("googlezd") ||
            command.contains("keresd meg") ||
            command.contains("nézz utána") ||
            command.contains("nezz utana")
        ) {

            val query =
                command
                    .replace("keress rá", "")
                    .replace("keress ra", "")
                    .replace("keress", "")
                    .replace("keresés", "")
                    .replace("kereses", "")
                    .replace("googlezd meg", "")
                    .replace("googlezd", "")
                    .replace("keresd meg", "")
                    .replace("nézz utána", "")
                    .replace("nezz utana", "")
                    .trim()

            if (query.isNotBlank()) {

                debugAction(
                    text,
                    command,
                    "Google keresés: $query"
                )

                val url =
                    "https://www.google.com/search?q=" +
                        URLEncoder.encode(
                            query,
                            "UTF-8"
                        )

                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                speak("Rákeresek.")

                Handler(mainLooper).postDelayed(
                    {
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            sendDebug(
                                """
                                🔴 Google hiba:
                                ${e.message}
                                """.trimIndent()
                            )
                        }
                    },
                    900
                )

                return
            }
        }

        /*
         * HÍVÁS
         */

        if (
            command.contains("hívd fel") ||
            command.contains("hivd fel") ||
            command.contains("hívás") ||
            command.contains("hivas") ||
            command.contains("telefonálj") ||
            command.contains("telefonalj")
        ) {

            val number =
                Regex("""\+?\d[\d\s-]{5,}""")
                    .find(command)
                    ?.value
                    ?.replace(" ", "")
                    ?.replace("-", "")

            if (!number.isNullOrBlank()) {

                debugAction(
                    text,
                    command,
                    "Hívás indítása: $number"
                )

                val intent =
                    Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:$number")
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                speak("Megnyitom a hívást.")

                Handler(mainLooper).postDelayed(
                    {
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            sendDebug(
                                """
                                🔴 Hívási hiba:
                                ${e.message}
                                """.trimIndent()
                            )
                        }
                    },
                    900
                )

                return
            }
        }

        /*
         * APP MEGNYITÁS
         *
         * Sokféle megfogalmazás.
         */

        val openCommand =
            command.contains("nyisd meg") ||
            command.contains("nyisd ki") ||
            command.contains("nyisd fel") ||
            command.contains("indítsd el") ||
            command.contains("inditsd el") ||
            command.contains("indítsd") ||
            command.contains("inditsd") ||
            command.contains("nyisd") ||
            command.contains("menj a") ||
            command.contains("lépj be") ||
            command.contains("lépj be") ||
            command.contains("nyomd meg")

        if (openCommand) {

            val appName =
                command
                    .replace("nyisd meg", "")
                    .replace("nyisd ki", "")
                    .replace("nyisd fel", "")
                    .replace("indítsd el", "")
                    .replace("inditsd el", "")
                    .replace("indítsd", "")
                    .replace("inditsd", "")
                    .replace("menj a", "")
                    .replace("lépj be", "")
                    .replace("lépj be", "")
                    .replace("nyisd", "")
                    .replace("nyomd meg", "")
                    .replace("az ", "")
                    .replace("a ", "")
                    .replace("appot", "")
                    .replace("alkalmazást", "")
                    .replace("alkalmazast", "")
                    .trim()

            if (appName.isNotBlank()) {

                openKnownApplication(
                    text,
                    command,
                    appName
                )

                return
            }
        }

        /*
         * KÖZVETLEN APP PARANCSOK
         *
         * Ezek akkor is működnek, ha a felismerő
         * kicsit furán szedi szét a mondatot.
         */

        if (
            containsAny(
                command,
                "youtube",
                "youtubeot",
                "jutub",
                "jutubot",
                "you tube",
                "you tubeot",
                "jútúb",
                "jútúbot"
            )
        ) {
            openPackage(
                text,
                command,
                "com.google.android.youtube",
                "YouTube"
            )
            return
        }

        if (
            containsAny(
                command,
                "discord",
                "discordot",
                "diszkord",
                "diszkordot"
            )
        ) {
            openPackage(
                text,
                command,
                "com.discord",
                "Discord"
            )
            return
        }

        if (
            containsAny(
                command,
                "tiktok",
                "tiktokot",
                "tik tok",
                "tik tokot"
            )
        ) {
            openPackage(
                text,
                command,
                "com.zhiliaoapp.musically",
                "TikTok"
            )
            return
        }

        if (
            containsAny(
                command,
                "instagram",
                "instagramot",
                "insta",
                "instát",
                "instat"
            )
        ) {
            openPackage(
                text,
                command,
                "com.instagram.android",
                "Instagram"
            )
            return
        }

        if (
            containsAny(
                command,
                "facebook",
                "facebookot",
                "face",
                "fészbuk",
                "feszbuk"
            )
        ) {
            openPackage(
                text,
                command,
                "com.facebook.katana",
                "Facebook"
            )
            return
        }

        if (
            containsAny(
                command,
                "messenger",
                "messengert",
                "messengerbe"
            )
        ) {
            openPackage(
                text,
                command,
                "com.facebook.orca",
                "Messenger"
            )
            return
        }

        if (
            containsAny(
                command,
                "chrome",
                "chromot",
                "google chrome",
                "böngésző",
                "bongeszo"
            )
        ) {
            openPackage(
                text,
                command,
                "com.android.chrome",
                "Chrome"
            )
            return
        }

        if (
            containsAny(
                command,
                "spotify",
                "spotifyt"
            )
        ) {
            openPackage(
                text,
                command,
                "com.spotify.music",
                "Spotify"
            )
            return
        }

        if (
            containsAny(
                command,
                "netflix",
                "netflixet"
            )
        ) {
            openPackage(
                text,
                command,
                "com.netflix.mediaclient",
                "Netflix"
            )
            return
        }

        if (
            containsAny(
                command,
                "gmail",
                "gmailt",
                "gmailem"
            )
        ) {
            openPackage(
                text,
                command,
                "com.google.android.gm",
                "Gmail"
            )
            return
        }

        if (
            containsAny(
                command,
                "maps",
                "térkép",
                "terkep",
                "google maps"
            )
        ) {
            openPackage(
                text,
                command,
                "com.google.android.apps.maps",
                "Google Térkép"
            )
            return
        }

        if (
            containsAny(
                command,
                "fotók",
                "fotok",
                "galéria",
                "galeria",
                "google photos"
            )
        ) {
            openPackage(
                text,
                command,
                "com.google.android.apps.photos",
                "Google Fotók"
            )
            return
        }

        if (
            containsAny(
                command,
                "play áruház",
                "play aruhaz",
                "play store",
                "play áruháza",
                "playaruhaz"
            )
        ) {
            openPackage(
                text,
                command,
                "com.android.vending",
                "Play Áruház"
            )
            return
        }

        if (
            containsAny(
                command,
                "beállítások",
                "beallitasok",
                "settings"
            )
        ) {
            openPackage(
                text,
                command,
                "com.android.settings",
                "Beállítások"
            )
            return
        }

        if (
            containsAny(
                command,
                "kamera",
                "kamerát",
                "kamerat"
            )
        ) {
            openCamera(
                text,
                command
            )
            return
        }

        /*
         * HA ISMERETLEN
         */

        sendDebug(
            """
            🟡 Service: FUT
            🎤 Mikrofon: AKTÍV
            👂 Hallotta:
            "$text"
            🧠 Parancs:
            "$command"
            ⚙️ Művelet:
            Ismeretlen parancs
            ❌ Hiba: -
            """.trimIndent()
        )

        speak(
            "Ezt a parancsot még nem ismerem."
        )
    }

    private fun openKnownApplication(
        originalText: String,
        command: String,
        appName: String
    ) {

        when {

            containsAny(
                appName,
                "youtube",
                "youtubeot",
                "jutub",
                "jutubot",
                "jútúb",
                "jútúbot"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.google.android.youtube",
                    "YouTube"
                )

                return
            }

            containsAny(
                appName,
                "discord",
                "discordot",
                "diszkord",
                "diszkordot"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.discord",
                    "Discord"
                )

                return
            }

            containsAny(
                appName,
                "tiktok",
                "tiktokot",
                "tik tok"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.zhiliaoapp.musically",
                    "TikTok"
                )

                return
            }

            containsAny(
                appName,
                "instagram",
                "instagramot",
                "insta"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.instagram.android",
                    "Instagram"
                )

                return
            }

            containsAny(
                appName,
                "facebook",
                "facebookot",
                "feszbuk",
                "fészbuk"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.facebook.katana",
                    "Facebook"
                )

                return
            }

            containsAny(
                appName,
                "messenger",
                "messengert"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.facebook.orca",
                    "Messenger"
                )

                return
            }

            containsAny(
                appName,
                "chrome",
                "chromot",
                "google chrome"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.android.chrome",
                    "Chrome"
                )

                return
            }

            containsAny(
                appName,
                "spotify",
                "spotifyt"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.spotify.music",
                    "Spotify"
                )

                return
            }

            containsAny(
                appName,
                "netflix",
                "netflixet"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.netflix.mediaclient",
                    "Netflix"
                )

                return
            }

            containsAny(
                appName,
                "gmail",
                "gmailt"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.google.android.gm",
                    "Gmail"
                )

                return
            }

            containsAny(
                appName,
                "maps",
                "google maps",
                "térkép",
                "terkep"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.google.android.apps.maps",
                    "Google Térkép"
                )

                return
            }

            containsAny(
                appName,
                "fotók",
                "fotok",
                "galéria",
                "galeria"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.google.android.apps.photos",
                    "Google Fotók"
                )

                return
            }

            containsAny(
                appName,
                "play áruház",
                "play aruhaz",
                "play store",
                "playaruhaz"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.android.vending",
                    "Play Áruház"
                )

                return
            }

            containsAny(
                appName,
                "beállítások",
                "beallitasok",
                "settings"
            ) -> {

                openPackage(
                    originalText,
                    command,
                    "com.android.settings",
                    "Beállítások"
                )

                return
            }

            containsAny(
                appName,
                "kamera",
                "kamerát",
                "kamerat"
            ) -> {

                openCamera(
                    originalText,
                    command
                )

                return
            }

            else -> {

                openApplication(
                    originalText,
                    command,
                    appName
                )
            }
        }
    }

    private fun openPackage(
        originalText: String,
        command: String,
        packageName: String,
        displayName: String
    ) {

        val launchIntent =
            packageManager.getLaunchIntentForPackage(
                packageName
            )

        if (launchIntent == null) {

            sendDebug(
                """
                🔴 Service: FUT
                🎤 Mikrofon: AKTÍV
                👂 Hallotta:
                "$originalText"
                🧠 Parancs:
                "$command"
                ⚙️ Művelet:
                $displayName megnyitása
                ❌ Hiba:
                Az alkalmazás nincs telepítve
                """.trimIndent()
            )

            speak(
                "$displayName nincs telepítve."
            )

            restartListening(1500)
            return
        }

        sendDebug(
            """
            🟢 Service: FUT
            🎤 Mikrofon: AKTÍV
            👂 Hallotta:
            "$originalText"
            🧠 Parancs:
            "$command"
            ⚙️ Művelet:
            $displayName megnyitása
            ❌ Hiba: -
            """.trimIndent()
        )

        speak(
            "Megnyitom a $displayName alkalmazást."
        )

        launchIntent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK

        Handler(mainLooper).postDelayed(
            {

                try {
                    startActivity(launchIntent)
                } catch (e: Exception) {

                    sendDebug(
                        """
                        🔴 App megnyitási hiba:
                        $displayName
                        ${e.message}
                        """.trimIndent()
                    )
                }
            },
            900
        )
    }

    private fun openApplication(
        originalText: String,
        command: String,
        name: String
    ) {

        val apps =
            packageManager
                .getInstalledApplications(0)

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
                ) ||
                name.contains(
                    label,
                    ignoreCase = true
                )
            ) {

                val launchIntent =
                    packageManager
                        .getLaunchIntentForPackage(
                            app.packageName
                        )

                if (launchIntent != null) {

                    debugAction(
                        originalText,
                        command,
                        "$label megnyitása"
                    )

                    speak(
                        "Megnyitom a $label alkalmazást."
                    )

                    launchIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK

                    Handler(mainLooper).postDelayed(
                        {

                            try {
                                startActivity(
                                    launchIntent
                                )
                            } catch (
                                e: Exception
                            ) {

                                sendDebug(
                                    """
                                    🔴 App megnyitási hiba:
                                    ${e.message}
                                    """.trimIndent()
                                )
                            }
                        },
                        900
                    )

                    return
                }
            }
        }

        sendDebug(
            """
            🟡 Service: FUT
            🎤 Mikrofon: AKTÍV
            👂 Hallotta:
            "$originalText"
            🧠 Parancs:
            "$command"
            ⚙️ Művelet:
            App megnyitása: $name
            ❌ Hiba:
            Nincs ilyen telepített alkalmazás
            """.trimIndent()
        )

        speak(
            "Ezt az alkalmazást nem találom."
        )
    }

    private fun openCamera(
        originalText: String,
        command: String
    ) {

        debugAction(
            originalText,
            command,
            "Kamera megnyitása"
        )

        val intent =
            Intent(
                "android.media.action.IMAGE_CAPTURE"
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK
            }

        speak("Megnyitom a kamerát.")

        Handler(mainLooper).postDelayed(
            {

                try {
                    startActivity(intent)
                } catch (e: Exception) {

                    sendDebug(
                        """
                        🔴 Kamera hiba:
                        ${e.message}
                        """.trimIndent()
                    )
                }
            },
            900
        )
    }

    private fun containsAny(
        text: String,
        vararg words: String
    ): Boolean {

        return words.any {
            text.contains(
                it,
                ignoreCase = true
            )
        }
    }

    private fun debugAction(
        originalText: String,
        command: String,
        action: String
    ) {

        sendDebug(
            """
            🟢 Service: FUT
            🎤 Mikrofon: AKTÍV
            👂 Hallotta:
            "$originalText"
            🧠 Parancs:
            "$command"
            ⚙️ Művelet:
            $action
            ❌ Hiba: -
            """.trimIndent()
        )
    }

    private fun speak(
        text: String
    ) {

        isSpeaking = true

        sendDebug(
            """
            🟢 Service: FUT
            🎤 Mikrofon: BESZÉD
            👂 Hallotta: -
            🧠 Parancs: -
            ⚙️ Művelet:
            Nova válasza: "$text"
            ❌ Hiba: -
            """.trimIndent()
        )

        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "nova_response"
        )

        Handler(mainLooper).postDelayed(
            {

                isSpeaking = false

                startListening()

            },
            1800
        )
    }

    private fun sendDebug(
        text: String
    ) {

        val intent =
            Intent(ACTION_DEBUG).apply {

                setPackage(packageName)

                putExtra(
                    EXTRA_DEBUG_TEXT,
                    text
                )
            }

        sendBroadcast(intent)
    }

    private fun getSpeechErrorText(
        error: Int
    ): String {

        return when (error) {

            SpeechRecognizer.ERROR_AUDIO ->
                "Audio hiba"

            SpeechRecognizer.ERROR_CLIENT ->
                "Kliens hiba"

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Nincs mikrofonengedély"

            SpeechRecognizer.ERROR_NETWORK ->
                "Hálózati hiba"

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Hálózati időtúllépés"

            SpeechRecognizer.ERROR_NO_MATCH ->
                "Nem értettem"

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "A felismerő foglalt"

            SpeechRecognizer.ERROR_SERVER ->
                "Szerver hiba"

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "Nem hallott beszédet"

            else ->
                "Ismeretlen speech hiba"
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

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        sendDebug(
            """
            🔴 Service: LEÁLLT
            🎤 Mikrofon: INAKTÍV
            👂 Hallotta: -
            🧠 Parancs: -
            ⚙️ Művelet: -
            ❌ Hiba: -
            """.trimIndent()
        )

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
