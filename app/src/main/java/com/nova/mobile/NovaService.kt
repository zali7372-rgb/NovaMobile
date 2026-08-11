package com.nova.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
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
    private var ttsReady = false

    private val handler = Handler()

    companion object {
        const val ACTION_DEBUG = "com.nova.mobile.DEBUG"
        const val EXTRA_DEBUG_TEXT = "debug_text"
        private const val CHANNEL_ID = "nova_service"
        private const val NOTIFICATION_ID = 1001
    }

    // ---------------------------------------------------------
    // NOVA MEGSZÓLÍTÁSOK
    // ---------------------------------------------------------

    private val novaNames = listOf(
        "nova",
        "noa",
        "nóva",
        "nóvá",
        "novaa",
        "novaaa",
        "novi",
        "novy",
        "novus",
        "novacska",
        "novácska",
        "novacskam",
        "novácskám",
        "novam",
        "novám",
        "novat",
        "novát",
        "nova ai",
        "nova assistant",
        "nova asszisztens",
        "nova bot",

        "hey nova",
        "hey noa",
        "hé nova",
        "hé noa",
        "he nova",
        "he noa",
        "hello nova",
        "hello noa",
        "helló nova",
        "helló noa",

        "szia nova",
        "szia noa",
        "hali nova",
        "hali noa",
        "hallod nova",
        "hallod noa",
        "figyelj nova",
        "figyelj noa",
        "nova figyelj",
        "noa figyelj",
        "nova hallod",
        "noa hallod",

        "nova kérlek",
        "nova kerlek",
        "noa kérlek",
        "noa kerlek",
        "nova légyszi",
        "nova legyszi",
        "noa légyszi",
        "noa legyszi",
        "nova légy szíves",
        "nova legy szives",
        "noa légy szíves",
        "noa legy szives",

        "szólok novának",
        "szolok novanak",
        "novának",
        "novanak"
    )

    // ---------------------------------------------------------
    // APP ALIASOK
    // ---------------------------------------------------------

    private val youtubeNames = listOf(
        "youtube",
        "youtubeot",
        "youtubot",
        "youtubot",
        "youtub",
        "youtubot",
        "jutub",
        "jutubot",
        "jutúb",
        "jutúbot",
        "youtube app",
        "youtube alkalmazás",
        "youtube alkalmazas",
        "youtube program",
        "videó app",
        "video app",
        "videókat",
        "videokat",
        "videós app",
        "videos app"
    )

    private val discordNames = listOf(
        "discord",
        "discordot",
        "diszkord",
        "diszkordot",
        "disc",
        "discord app",
        "discord alkalmazás",
        "discord alkalmazas",
        "discord program",
        "chat",
        "chat app",
        "cset",
        "csevegő",
        "csevego",
        "discordra",
        "discordba",
        "discordot"
    )

    private val chromeNames = listOf(
        "chrome",
        "chromot",
        "krom",
        "króm",
        "kromot",
        "krómot",
        "böngésző",
        "bongeszo",
        "böngészőt",
        "bongeszo",
        "internet",
        "internetet",
        "internet böngésző",
        "chrome app",
        "chrome alkalmazás",
        "chrome alkalmazas",
        "böngésző app"
    )

    private val cameraNames = listOf(
        "kamera",
        "kamerát",
        "kamerat",
        "kamera app",
        "kamera alkalmazás",
        "kamera alkalmazas",
        "fényképező",
        "fenykepezo",
        "fényképezőt",
        "fenykepezot",
        "fényképezőgép",
        "fenykepezogep",
        "fotó",
        "foto",
        "fotózás",
        "fotozas"
    )

    private val settingsNames = listOf(
        "beállítások",
        "beallitasok",
        "beállítás",
        "beallitas",
        "telefon beállítások",
        "telefon beallitasok",
        "settings",
        "setting",
        "beállítások app",
        "beallitasok app",
        "beállítás alkalmazás",
        "beallitas alkalmazas"
    )

    private val phoneNames = listOf(
        "telefon",
        "telefon app",
        "telefon alkalmazás",
        "telefon alkalmazas",
        "hívás",
        "hivas",
        "hívó",
        "hivo",
        "tárcsázó",
        "tarcsazo",
        "telefonálás",
        "telefonalas",
        "telefon appot"
    )

    private val musicNames = listOf(
        "zene",
        "zene app",
        "zene alkalmazás",
        "zene alkalmazas",
        "zenét",
        "zenet",
        "music",
        "music app",
        "lejátszó",
        "lejatszo",
        "zenelejátszó",
        "zenelejatszo",
        "muzsika",
        "muzsikát",
        "muzsikat"
    )

    private val galleryNames = listOf(
        "galéria",
        "galeria",
        "galériát",
        "galeriat",
        "képek",
        "kepek",
        "fotók",
        "fotok",
        "fotók app",
        "fotok app",
        "galéria app",
        "galeria app",
        "képek alkalmazás",
        "kepek alkalmazas"
    )

    private val calculatorNames = listOf(
        "számológép",
        "szamologep",
        "számolót",
        "szamolot",
        "kalkulátor",
        "kalkulator",
        "calculator",
        "calculator app",
        "számológép app",
        "szamologep app"
    )

    // ---------------------------------------------------------
    // INDÍTÁS
    // ---------------------------------------------------------

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("Nova")
            .setContentText("Nova figyel a háttérben")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        sendDebug(
            """
            🟢 SERVICE: FUT
            🎤 Mikrofon: inicializálás
            👂 Hallotta: -
            🧠 Parancs: -
            ⚙️ Művelet: -
            🔊 TTS: inicializálás
            ❌ Hiba: -
            """.trimIndent()
        )

        textToSpeech = TextToSpeech(
            this,
            this
        )

        setupRecognizer()
        startListening()
    }

    // ---------------------------------------------------------
    // TTS
    // ---------------------------------------------------------

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            val result = textToSpeech?.setLanguage(
                Locale("hu", "HU")
            )

            ttsReady =
                result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED

            textToSpeech?.setSpeechRate(1.0f)

            sendDebug(
                """
                🟢 TTS: KÉSZ
                🔊 Magyar nyelv: $ttsReady
                🎤 Mikrofon: AKTÍV
                """.trimIndent()
            )
        } else {

            sendDebug(
                """
                🔴 TTS: HIBA
                ❌ Nem sikerült inicializálni
                """.trimIndent()
            )
        }
    }

    private fun speak(text: String) {

        if (!ttsReady) {
            startListening()
            return
        }

        isSpeaking = true

        sendDebug(
            """
            🔊 NOVA BESZÉL
            💬 "$text"
            """.trimIndent()
        )

        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "nova_response"
        )

        handler.postDelayed(
            {
                isSpeaking = false
                startListening()
            },
            1800
        )
    }

    // ---------------------------------------------------------
    // SPEECH RECOGNIZER
    // ---------------------------------------------------------

    private fun setupRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            sendDebug(
                """
                🔴 SPEECH: NEM ELÉRHETŐ
                ❌ A telefon beszédfelismerője nem érhető el.
                """.trimIndent()
            )

            return
        }

        recognizer?.destroy()

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
                        🟢 MIKROFON: AKTÍV
                        👂 Nova figyel...
                        """.trimIndent()
                    )
                }

                override fun onBeginningOfSpeech() {

                    sendDebug(
                        """
                        🎤 BESZÉDET ÉSZLEL
                        👂 Hallgat...
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
                        🟡 MIKROFON: FELVÉTEL VÉGE
                        🧠 Feldolgozás...
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
                        🔴 SPEECH HIBA
                        ❌ $error
                        📄 $errorText
                        """.trimIndent()
                    )

                    restartListening(800)
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
                        👂 HALLotta:
                        "$text"
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
        }

        if (recognizer == null) return

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
                    10
                )
            }

        try {

            isListening = true

            recognizer?.startListening(intent)

        } catch (e: Exception) {

            isListening = false

            sendDebug(
                """
                🔴 LISTENING HIBA
                ❌ ${e.message}
                """.trimIndent()
            )

            restartListening(1500)
        }
    }

    private fun restartListening(
        delay: Long
    ) {

        handler.postDelayed(
            {

                if (!isSpeaking) {
                    startListening()
                }

            },
            delay
        )
    }

    // ---------------------------------------------------------
    // PARANCS FELDOLGOZÁS
    // ---------------------------------------------------------

    private fun handleCommand(
        originalText: String
    ) {

        if (originalText.isBlank()) {

            sendDebug(
                """
                🟡 ÜRES FELISMERÉS
                """.trimIndent()
            )

            restartListening(500)
            return
        }

        val normalized =
            normalizeText(originalText)

        val novaTrigger =
            novaNames
                .map { normalizeText(it) }
                .any {
                    normalized.contains(it)
                }

        if (!novaTrigger) {

            sendDebug(
                """
                🟡 NINCS NOVA MEGSZÓLÍTÁS
                👂 "$originalText"
                """.trimIndent()
            )

            restartListening(400)
            return
        }

        var command = normalized

        novaNames.forEach {
            command = command.replace(
                normalizeText(it),
                " "
            )
        }

        command =
            command
                .replace(",", " ")
                .replace(".", " ")
                .trim()
                .replace(Regex("\\s+"), " ")

        sendDebug(
            """
            🧠 PARANCS:
            "$command"
            """.trimIndent()
        )

        // -----------------------------------------------------
        // CSAK "NOVA"
        // -----------------------------------------------------

        if (command.isBlank()) {

            speak("Igen?")

            return
        }

        // -----------------------------------------------------
        // LEÁLLÍTÁS
        // -----------------------------------------------------

        if (
            containsAny(
                command,
                listOf(
                    "állj",
                    "allj",
                    "állj le",
                    "allj le",
                    "kapcsold ki magad",
                    "kapcsold ki",
                    "állj le nova",
                    "stop",
                    "stop nova",
                    "off",
                    "leállítás",
                    "leallitas",
                    "fejezd be",
                    "hagyd abba"
                )
            )
        ) {

            sendDebug("⚙️ Nova leállítása")

            speak("Rendben, leállok.")

            handler.postDelayed(
                {
                    stopSelf()
                },
                1500
            )

            return
        }

        // -----------------------------------------------------
        // YOUTUBE
        // -----------------------------------------------------

        if (containsAny(command, youtubeNames)) {

            sendDebug(
                "⚙️ YouTube megnyitása"
            )

            speak("Megnyitom a YouTube-ot.")

            openApplicationByAliases(
                youtubeNames
            )

            return
        }

        // -----------------------------------------------------
        // DISCORD
        // -----------------------------------------------------

        if (containsAny(command, discordNames)) {

            sendDebug(
                "⚙️ Discord megnyitása"
            )

            speak("Megnyitom a Discordot.")

            openApplicationByAliases(
                discordNames
            )

            return
        }

        // -----------------------------------------------------
        // CHROME
        // -----------------------------------------------------

        if (containsAny(command, chromeNames)) {

            sendDebug(
                "⚙️ Chrome megnyitása"
            )

            speak("Megnyitom a böngészőt.")

            openApplicationByAliases(
                chromeNames
            )

            return
        }

        // -----------------------------------------------------
        // KAMERA
        // -----------------------------------------------------

        if (containsAny(command, cameraNames)) {

            sendDebug(
                "⚙️ Kamera megnyitása"
            )

            speak("Megnyitom a kamerát.")

            openApplicationByAliases(
                cameraNames
            )

            return
        }

        // -----------------------------------------------------
        // BEÁLLÍTÁSOK
        // -----------------------------------------------------

        if (containsAny(command, settingsNames)) {

            sendDebug(
                "⚙️ Beállítások megnyitása"
            )

            speak("Megnyitom a beállításokat.")

            try {

                val intent =
                    Intent(
                        android.provider.Settings.ACTION_SETTINGS
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                startActivity(intent)

            } catch (e: Exception) {

                sendDebug(
                    "🔴 Beállítások hiba: ${e.message}"
                )
            }

            return
        }

        // -----------------------------------------------------
        // TELEFON
        // -----------------------------------------------------

        if (containsAny(command, phoneNames)) {

            sendDebug(
                "⚙️ Telefon megnyitása"
            )

            speak("Megnyitom a telefont.")

            openApplicationByAliases(
                phoneNames
            )

            return
        }

        // -----------------------------------------------------
        // ZENE
        // -----------------------------------------------------

        if (containsAny(command, musicNames)) {

            sendDebug(
                "⚙️ Zene alkalmazás megnyitása"
            )

            speak("Megnyitom a zenelejátszót.")

            openApplicationByAliases(
                musicNames
            )

            return
        }

        // -----------------------------------------------------
        // GALÉRIA
        // -----------------------------------------------------

        if (containsAny(command, galleryNames)) {

            sendDebug(
                "⚙️ Galéria megnyitása"
            )

            speak("Megnyitom a galériát.")

            openApplicationByAliases(
                galleryNames
            )

            return
        }

        // -----------------------------------------------------
        // SZÁMOLÓGÉP
        // -----------------------------------------------------

        if (containsAny(command, calculatorNames)) {

            sendDebug(
                "⚙️ Számológép megnyitása"
            )

            speak("Megnyitom a számológépet.")

            openApplicationByAliases(
                calculatorNames
            )

            return
        }

        // -----------------------------------------------------
        // IDŐZÍTŐ
        // -----------------------------------------------------

        if (
            containsAny(
                command,
                listOf(
                    "időzítő",
                    "idozito",
                    "timer",
                    "időzítőt",
                    "idozitot",
                    "állíts be időzítőt",
                    "allits be idozitot",
                    "állíts időzítőt",
                    "allits idozitot",
                    "indíts időzítőt",
                    "indits idozitot",
                    "tegyél be időzítőt",
                    "tegyel be idozitot"
                )
            )
        ) {

            val minutes =
                Regex("""(\d+)""")
                    .find(command)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()

            if (minutes != null) {

                sendDebug(
                    "⚙️ $minutes perces időzítő"
                )

                speak(
                    "$minutes perces időzítőt állítok be."
                )

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

                handler.postDelayed(
                    {
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            sendDebug(
                                "🔴 Időzítő hiba: ${e.message}"
                            )
                        }
                    },
                    900
                )

                return
            }

            speak(
                "Mondd meg, hány percre állítsam."
            )

            return
        }

        // -----------------------------------------------------
        // GOOGLE KERESÉS
        // -----------------------------------------------------

        if (
            containsAny(
                command,
                listOf(
                    "keress",
                    "keres",
                    "keresés",
                    "kereses",
                    "keress rá",
                    "keress ra",
                    "googlezd",
                    "googlezd meg",
                    "keresd meg",
                    "nézz utána",
                    "nezz utana",
                    "nézz rá",
                    "nezz ra"
                )
            )
        ) {

            var query = command

            listOf(
                "keress rá",
                "keress ra",
                "keress",
                "keresés",
                "kereses",
                "keres",
                "googlezd meg",
                "googlezd",
                "keresd meg",
                "nézz utána",
                "nezz utana",
                "nézz rá",
                "nezz ra"
            ).forEach {
                query = query.replace(
                    it,
                    ""
                )
            }

            query = query.trim()

            if (query.isNotBlank()) {

                sendDebug(
                    "⚙️ Google: $query"
                )

                speak("Rákeresek.")

                val url =
                    "https://www.google.com/search?q=" +
                        URLEncoder.encode(
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

                handler.postDelayed(
                    {
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            sendDebug(
                                "🔴 Google hiba: ${e.message}"
                            )
                        }
                    },
                    900
                )

                return
            }
        }

        // -----------------------------------------------------
        // ÁLTALÁNOS APP MEGNYITÁS
        // -----------------------------------------------------

        if (
            containsAny(
                command,
                listOf(
                    "nyisd meg",
                    "nyisd ki",
                    "nyisd",
                    "indítsd el",
                    "inditsd el",
                    "indítsd",
                    "inditsd",
                    "indítsd meg",
                    "inditsd meg",
                    "menj a",
                    "lépj be",
                    "lepes be",
                    "kapcsold be",
                    "nyomd meg",
                    "nyisd fel"
                )
            )
        ) {

            var appName = command

            listOf(
                "nyisd meg",
                "nyisd ki",
                "nyisd",
                "indítsd el",
                "inditsd el",
                "indítsd",
                "inditsd",
                "indítsd meg",
                "inditsd meg",
                "menj a",
                "lépj be",
                "lepes be",
                "kapcsold be",
                "nyomd meg",
                "nyisd fel"
            ).forEach {
                appName =
                    appName.replace(
                        it,
                        ""
                    )
            }

            appName = appName.trim()

            if (appName.isNotBlank()) {

                sendDebug(
                    "⚙️ Általános app: $appName"
                )

                speak("Megpróbálom megnyitni.")

                handler.postDelayed(
                    {
                        openApplication(
                            appName
                        )
                    },
                    900
                )

                return
            }
        }

        // -----------------------------------------------------
        // ISMERETLEN
        // -----------------------------------------------------

        sendDebug(
            """
            🟡 ISMERETLEN PARANCS
            🧠 "$command"
            """.trimIndent()
        )

        speak(
            "Ezt a parancsot még nem ismerem."
        )
    }

    // ---------------------------------------------------------
    // APP KERESÉS ALIASOKKAL
    // ---------------------------------------------------------

    private fun openApplicationByAliases(
        aliases: List<String>
    ) {

        val apps =
            packageManager.getInstalledApplications(0)

        for (app in apps) {

            val label =
                packageManager
                    .getApplicationLabel(app)
                    .toString()
                    .lowercase(Locale.getDefault())

            val match =
                aliases.any {
                    val alias =
                        normalizeText(it)

                    label.contains(alias) ||
                    alias.contains(
                        normalizeText(label)
                    )
                }

            if (match) {

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

                        sendDebug(
                            "🟢 APP ELINDÍTVA: $label"
                        )

                        return

                    } catch (e: Exception) {

                        sendDebug(
                            """
                            🔴 APP HIBA
                            $label
                            ${e.message}
                            """.trimIndent()
                        )

                        return
                    }
                }
            }
        }

        sendDebug(
            """
            🔴 APP NEM TALÁLHATÓ
            Aliasok: ${aliases.take(5)}
            """.trimIndent()
        )

        speak(
            "Ezt az alkalmazást nem találom."
        )
    }

    private fun openApplication(
        name: String
    ) {

        val normalizedName =
            normalizeText(name)

        val apps =
            packageManager
                .getInstalledApplications(0)

        for (app in apps) {

            val label =
                packageManager
                    .getApplicationLabel(app)
                    .toString()

            val normalizedLabel =
                normalizeText(label)

            if (
                normalizedLabel.contains(
                    normalizedName
                ) ||
                normalizedName.contains(
                    normalizedLabel
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

                        sendDebug(
                            "🟢 APP ELINDÍTVA: $label"
                        )

                        return

                    } catch (e: Exception) {

                        sendDebug(
                            "🔴 App hiba: ${e.message}"
                        )

                        return
                    }
                }
            }
        }

        sendDebug(
            """
            🔴 APP NEM TALÁLHATÓ
            "$name"
            """.trimIndent()
        )

        speak(
            "Ezt az alkalmazást nem találom."
        )
    }

    // ---------------------------------------------------------
    // SEGÉDFÜGGVÉNYEK
    // ---------------------------------------------------------

    private fun containsAny(
        text: String,
        values: List<String>
    ): Boolean {

        return values.any {
            text.contains(
                normalizeText(it)
            )
        }
    }

    private fun normalizeText(
        text: String
    ): String {

        return text
            .lowercase(Locale("hu", "HU"))
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ö", "o")
            .replace("ő", "o")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace("ű", "u")
            .replace("-", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace("!", " ")
            .replace("?", " ")
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
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

    // ---------------------------------------------------------
    // NOTIFICATION
    // ---------------------------------------------------------

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
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

    // ---------------------------------------------------------
    // LEÁLLÍTÁS
    // ---------------------------------------------------------

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        recognizer?.destroy()
        recognizer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        isListening = false
        isSpeaking = false
        ttsReady = false

        sendDebug(
            """
            🔴 NOVA SERVICE: LEÁLLT
            🎤 Mikrofon: INAKTÍV
            🔊 TTS: LEÁLLT
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
