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
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.net.URLEncoder
import java.util.Locale

class NovaService : Service(), TextToSpeech.OnInitListener {

    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private var isListening = false
    private var isSpeaking = false
    private var ttsReady = false

    private var pendingSpeech: String? = null

    private val handler by lazy {
        Handler(mainLooper)
    }

    companion object {

        const val ACTION_DEBUG =
            "com.nova.mobile.DEBUG"

        const val EXTRA_DEBUG_TEXT =
            "debug_text"

        private const val CHANNEL_ID =
            "nova_service"

        private const val NOTIFICATION_ID =
            1001
    }

    // =========================================================
    // SERVICE
    // =========================================================

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle("Nova")
                .setContentText(
                    "Nova figyel a háttérben"
                )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .build()

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        sendDebug(
            """
            🟢 NOVA SERVICE
            Service: FUT
            🎤 Mikrofon: inicializálás...
            🗣️ TTS: inicializálás...
            👂 Hallotta: -
            🧠 Parancs: -
            ⚙️ Művelet: -
            ❌ Hiba: -
            """.trimIndent()
        )

        setupTextToSpeech()

        setupRecognizer()

        handler.postDelayed(
            {
                startListening()
            },
            1000
        )
    }

    // =========================================================
    // TEXT TO SPEECH
    // =========================================================

    private fun setupTextToSpeech() {

        textToSpeech =
            TextToSpeech(
                this,
                this
            )

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(
                    utteranceId: String?
                ) {

                    isSpeaking = true

                    sendDebug(
                        """
                        🗣️ NOVA BESZÉL
                        TTS: BESZÉD ELINDULT
                        """.trimIndent()
                    )
                }

                override fun onDone(
                    utteranceId: String?
                ) {

                    handler.post {
                        isSpeaking = false

                        sendDebug(
                            """
                            🗣️ NOVA BESZÉL
                            TTS: BESZÉD BEFEJEZVE
                            🎤 Mikrofon: újraindítás...
                            """.trimIndent()
                        )

                        startListening()
                    }
                }

                override fun onError(
                    utteranceId: String?
                ) {

                    handler.post {
                        isSpeaking = false

                        sendDebug(
                            """
                            🔴 TTS HIBA
                            ❌ A beszéd leállt
                            """.trimIndent()
                        )

                        startListening()
                    }
                }
            }
        )
    }

    override fun onInit(
        status: Int
    ) {

        if (
            status !=
            TextToSpeech.SUCCESS
        ) {

            ttsReady = false

            sendDebug(
                """
                🔴 TTS
                🗣️ Állapot: HIBA
                ❌ Text To Speech inicializálási hiba
                """.trimIndent()
            )

            return
        }

        val result =
            textToSpeech?.setLanguage(
                Locale("hu", "HU")
            )

        textToSpeech?.setSpeechRate(
            1.0f
        )

        textToSpeech?.setPitch(
            1.0f
        )

        ttsReady =
            result !=
                TextToSpeech.LANG_MISSING_DATA &&
            result !=
                TextToSpeech.LANG_NOT_SUPPORTED

        if (!ttsReady) {

            sendDebug(
                """
                🔴 TTS
                🗣️ Állapot: HIBA
                ❌ Magyar TTS nem érhető el
                """.trimIndent()
            )

            return
        }

        sendDebug(
            """
            🟢 TTS
            🗣️ Állapot: KÉSZ
            🇭🇺 Nyelv: magyar
            """.trimIndent()
        )

        pendingSpeech?.let {
            pendingSpeech = null
            speak(it)
        }
    }

    private fun speak(
        text: String
    ) {

        if (text.isBlank()) {
            return
        }

        sendDebug(
            """
            🗣️ NOVA VÁLASZ
            "$text"
            """.trimIndent()
        )

        if (!ttsReady) {

            pendingSpeech = text

            sendDebug(
                """
                🟡 TTS
                ⏳ Még nem kész
                📝 Várakozó szöveg:
                "$text"
                """.trimIndent()
            )

            return
        }

        isSpeaking = true

        if (isListening) {

            try {
                recognizer?.cancel()
            } catch (_: Exception) {
            }

            isListening = false
        }

        try {

            textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "NOVA_RESPONSE"
            )

        } catch (e: Exception) {

            isSpeaking = false

            sendDebug(
                """
                🔴 TTS HIBA
                ❌ ${e.message}
                """.trimIndent()
            )

            restartListening(1000)
        }
    }

    // =========================================================
    // NOVA MEGSZÓLÍTÁSOK
    // =========================================================

    private val novaNames =
        listOf(

            "nova",
            "noa",
            "novaa",
            "novah",
            "novi",
            "novi ai",
            "nova ai",
            "novus",
            "novácska",
            "novacska",

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

            "hé nova",
            "he nova",

            "na nova",
            "na noa",

            "nova kérlek",
            "nova kerlek",
            "noa kérlek",
            "noa kerlek",

            "nova hallgass",
            "noa hallgass",

            "nova segíts",
            "nova segits",
            "noa segíts",
            "noa segits",

            "nova segíts nekem",
            "nova segits nekem",

            "nova csináld",
            "nova csinald",
            "noa csináld",
            "noa csinald",

            "nova csináld meg",
            "nova csinald meg",

            "nova indulj",
            "noa indulj",

            "nova ébresztő",
            "nova ebreszto",

            "nova figyelj rám",
            "nova figyelj ram",

            "noa figyelj rám",
            "noa figyelj ram"
        )

    // =========================================================
    // APP ALIASOK
    // =========================================================

    private val appAliases =
        mapOf(

            "youtube" to listOf(
                "youtube",
                "youtubeot",
                "you tube",
                "you tube ot",
                "jutub",
                "jútub",
                "youtub",
                "youtubu",
                "jutube",
                "jutubot",
                "youtubot",
                "youtube app"
            ),

            "discord" to listOf(
                "discord",
                "discordot",
                "diszkord",
                "discort",
                "disscord",
                "diszkordot",
                "discord app"
            ),

            "tiktok" to listOf(
                "tiktok",
                "tiktokot",
                "tik tok",
                "tik tokot",
                "tiktók",
                "tiktak",
                "tiktok app"
            ),

            "chrome" to listOf(
                "chrome",
                "chromeot",
                "króm",
                "krom",
                "kromot",
                "chrome app"
            ),

            "spotify" to listOf(
                "spotify",
                "spotifyt",
                "spoty",
                "szpotifáj",
                "szpotify",
                "spotify app"
            ),

            "facebook" to listOf(
                "facebook",
                "facebookot",
                "fészbuk",
                "feszbuk",
                "facebook app"
            ),

            "messenger" to listOf(
                "messenger",
                "messengert",
                "mesenger",
                "messzi",
                "messenger app"
            ),

            "instagram" to listOf(
                "instagram",
                "instagramot",
                "insta",
                "instát",
                "instat",
                "instagra",
                "instagram app"
            ),

            "google maps" to listOf(
                "google maps",
                "google map",
                "google mapsot",
                "maps",
                "mapsot",
                "térkép",
                "terkep",
                "google térkép",
                "google terkep"
            ),

            "gmail" to listOf(
                "gmail",
                "gmailt",
                "g mail",
                "g mailt",
                "gmail app"
            ),

            "roblox" to listOf(
                "roblox",
                "robloxot",
                "robloks",
                "roblox app"
            ),

            "minecraft" to listOf(
                "minecraft",
                "minecraftot",
                "mine craft",
                "mine craftot",
                "minecraft app"
            ),

            "whatsapp" to listOf(
                "whatsapp",
                "whatsappot",
                "whats app",
                "vácáp",
                "vacap",
                "whatsapp app"
            ),

            "telegram" to listOf(
                "telegram",
                "telegramot",
                "telegrám",
                "telegram app"
            ),

            "reddit" to listOf(
                "reddit",
                "redditet",
                "redit",
                "reddit app"
            ),

            "netflix" to listOf(
                "netflix",
                "netflixet",
                "netfliksz",
                "netflix app"
            ),

            "galéria" to listOf(
                "galéria",
                "galeria",
                "galériát",
                "galeriat",
                "képek",
                "kepek",
                "fotók",
                "fotok",
                "galéria app",
                "galeria app"
            ),

            "kamera" to listOf(
                "kamera",
                "kamerát",
                "kamerat",
                "kamerát nyisd meg",
                "kamera app"
            ),

            "beállítások" to listOf(
                "beállítások",
                "beallitasok",
                "beállítás",
                "beallitas",
                "beállításokat",
                "beallitasokat",
                "beállítások app"
            ),

            "Play Áruház" to listOf(
                "play áruház",
                "play aruhaz",
                "play áruházat",
                "play aruhazat",
                "play store",
                "playstore",
                "google play",
                "google playt"
            ),

            "Google" to listOf(
                "google",
                "google-t",
                "googlet",
                "gugli"
            ),

            "Telefon" to listOf(
                "telefon",
                "telefont",
                "telefon app",
                "hívások",
                "hivasok"
            ),

            "Üzenetek" to listOf(
                "üzenetek",
                "uzenetek",
                "üzeneteket",
                "uzeneteket",
                "sms",
                "sms-ek"
            ),

            "Óra" to listOf(
                "óra",
                "ora",
                "órát",
                "orat",
                "óra app",
                "ora app"
            ),

            "Naptár" to listOf(
                "naptár",
                "naptar",
                "naptárat",
                "naptarat",
                "calendar"
            ),

            "Fájlok" to listOf(
                "fájlok",
                "fajlok",
                "fájlkezelő",
                "fajlkezelo",
                "file manager"
            )
        )

    // =========================================================
    // SPEECH RECOGNIZER
    // =========================================================

    private fun setupRecognizer() {

        if (
            !SpeechRecognizer
                .isRecognitionAvailable(
                    this
                )
        ) {

            sendDebug(
                """
                🔴 SPEECH RECOGNIZER
                🎤 Mikrofon: NEM ELÉRHETŐ
                ❌ Speech recognition nem támogatott
                """.trimIndent()
            )

            return
        }

        recognizer =
            SpeechRecognizer
                .createSpeechRecognizer(
                    this
                )

        recognizer?.setRecognitionListener(
            object :
                RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    isListening = true

                    sendDebug(
                        """
                        🟢 MIKROFON
                        🎤 Állapot: AKTÍV
                        👂 Hallgatlak...
                        """.trimIndent()
                    )
                }

                override fun onBeginningOfSpeech() {

                    sendDebug(
                        """
                        🟢 MIKROFON
                        🎤 Állapot: BESZÉDET ÉSZLEL
                        👂 Hallotta: ...
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
                        🟡 MIKROFON
                        🎤 Állapot: FELVÉTEL VÉGE
                        👂 Feldolgozás...
                        """.trimIndent()
                    )
                }

                override fun onError(
                    error: Int
                ) {

                    isListening = false

                    val errorText =
                        getSpeechErrorText(
                            error
                        )

                    sendDebug(
                        """
                        🔴 SPEECH HIBA
                        🎤 Mikrofon: HIBA
                        ❌ Kód: $error
                        ❌ Hiba: $errorText
                        """.trimIndent()
                    )

                    restartListening(
                        1000
                    )
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    isListening = false

                    val matches =
                        results
                            ?.getStringArrayList(
                                SpeechRecognizer
                                    .RESULTS_RECOGNITION
                            )

                    val text =
                        matches
                            ?.firstOrNull()
                            ?.trim()
                            ?: ""

                    sendDebug(
                        """
                        🟢 FELISMERÉS
                        👂 Hallotta:
                        "$text"
                        🧠 Feldolgozás...
                        """.trimIndent()
                    )

                    handleCommand(
                        text
                    )
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

    // =========================================================
    // LISTENING
    // =========================================================

    private fun startListening() {

        if (isSpeaking) {
            return
        }

        if (isListening) {
            return
        }

        if (!ttsReady) {

            sendDebug(
                """
                🟡 HALLGATÁS
                ⏳ TTS még inicializálódik
                """.trimIndent()
            )

            return
        }

        if (recognizer == null) {

            setupRecognizer()

            if (recognizer == null) {
                return
            }
        }

        val intent =
            Intent(
                RecognizerIntent
                    .ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent
                        .EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent
                        .LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_LANGUAGE,
                    "hu-HU"
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_LANGUAGE_PREFERENCE,
                    "hu-HU"
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_PARTIAL_RESULTS,
                    false
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_MAX_RESULTS,
                    10
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_CALLING_PACKAGE,
                    packageName
                )
            }

        try {

            isListening = true

            recognizer?.startListening(
                intent
            )

            sendDebug(
                """
                🟢 HALLGATÁS
                🎤 Mikrofon: FIGYEL
                🇭🇺 Nyelv: hu-HU
                """.trimIndent()
            )

        } catch (e: Exception) {

            isListening = false

            sendDebug(
                """
                🔴 HALLGATÁS HIBA
                ❌ ${e.message}
                """.trimIndent()
            )

            restartListening(
                1500
            )
        }
    }

    private fun restartListening(
        delay: Long
    ) {

        handler.postDelayed(
            {

                if (
                    !isSpeaking &&
                    !isListening
                ) {
                    startListening()
                }

            },
            delay
        )
    }

    // =========================================================
    // COMMAND HANDLER
    // =========================================================

    private fun handleCommand(
        text: String
    ) {

        if (text.isBlank()) {

            sendDebug(
                """
                🟡 FELISMERÉS
                👂 Hallotta: ""
                🧠 Parancs: nincs
                """.trimIndent()
            )

            restartListening(
                500
            )

            return
        }

        val normalized =
            normalizeText(
                text
            )

        val triggered =
            novaNames.any {
                normalized.contains(
                    normalizeText(it)
                )
            }

        if (!triggered) {

            sendDebug(
                """
                🟡 NOVA
                👂 Hallotta:
                "$text"
                🧠 Parancs:
                Nem Nova megszólítás
                ⚙️ Művelet: nincs
                """.trimIndent()
            )

            restartListening(
                500
            )

            return
        }

        var command =
            normalized

        novaNames.forEach {

            command =
                command.replace(
                    normalizeText(it),
                    " "
                )
        }

        command =
            command
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        sendDebug(
            """
            🧠 NOVA PARANCS
            👂 Hallotta:
            "$text"
            🧠 Parancs:
            "$command"
            """.trimIndent()
        )

        // =====================================================
        // CSAK NOVA
        // =====================================================

        if (command.isBlank()) {

            speak(
                "Igen?"
            )

            return
        }

        // =====================================================
        // LEÁLLÍTÁS
        // =====================================================

        if (
            containsAny(
                command,
                listOf(
                    "allj",
                    "állj",
                    "kapcsold ki magad",
                    "kapcsold ki",
                    "allj le",
                    "állj le",
                    "leallhatsz",
                    "leállhatsz",
                    "stop",
                    "off",
                    "állj le nova",
                    "allj le nova",
                    "fejezd be",
                    "állítsd le",
                    "allitsd le"
                )
            )
        ) {

            sendDebug(
                """
                ⚙️ MŰVELET
                Nova leállítása
                """.trimIndent()
            )

            speak(
                "Oké, leállok."
            )

            handler.postDelayed(
                {
                    stopSelf()
                },
                1800
            )

            return
        }

        // =====================================================
        // IDŐZÍTŐ
        // =====================================================

        if (
            containsAny(
                command,
                listOf(
                    "idozito",
                    "időzítő",
                    "timer",
                    "állíts be időzítőt",
                    "allits be idozitot",
                    "állíts időzítőt",
                    "allits idozitot",
                    "időzítőt kérek",
                    "idozitot kerek"
                )
            )
        ) {

            val minutes =
                Regex(
                    """(\d+)"""
                )
                    .find(command)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()

            if (minutes != null) {

                sendDebug(
                    """
                    ⚙️ MŰVELET
                    $minutes perces időzítő
                    """.trimIndent()
                )

                speak(
                    "Oké, beállítottam a $minutes perces időzítőt."
                )

                val intent =
                    Intent(
                        android.provider
                            .AlarmClock
                            .ACTION_SET_TIMER
                    ).apply {

                        putExtra(
                            android.provider
                                .AlarmClock
                                .EXTRA_LENGTH,
                            (
                                minutes * 60
                            ).toInt()
                        )

                        putExtra(
                            android.provider
                                .AlarmClock
                                .EXTRA_SKIP_UI,
                            false
                        )

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                handler.postDelayed(
                    {

                        try {
                            startActivity(
                                intent
                            )
                        } catch (e: Exception) {

                            sendDebug(
                                """
                                🔴 IDŐZÍTŐ HIBA
                                ❌ ${e.message}
                                """.trimIndent()
                            )
                        }

                    },
                    1000
                )

                return
            }

            speak(
                "Hány percre állítsam?"
            )

            return
        }

        // =====================================================
        // GOOGLE KERESÉS
        // =====================================================

        if (
            containsAny(
                command,
                listOf(
                    "keress",
                    "keress rá",
                    "keress ra",
                    "keresd meg",
                    "googlezd meg",
                    "googlezz rá",
                    "googlezz ra",
                    "keress nekem",
                    "nézz utána",
                    "nezz utana",
                    "keress rá arra"
                )
            )
        ) {

            var query =
                command

            listOf(
                "keress rá",
                "keress ra",
                "keress",
                "keresd meg",
                "googlezd meg",
                "googlezz rá",
                "googlezz ra",
                "keress nekem",
                "nézz utána",
                "nezz utana",
                "keress rá arra"
            ).forEach {

                query =
                    query.replace(
                        normalizeText(it),
                        ""
                    )
            }

            query =
                query
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()

            if (
                query.isNotBlank()
            ) {

                sendDebug(
                    """
                    ⚙️ MŰVELET
                    Google keresés:
                    "$query"
                    """.trimIndent()
                )

                speak(
                    "Oké, rákeresek."
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
                        android.net.Uri.parse(
                            url
                        )
                    ).apply {

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                handler.postDelayed(
                    {

                        try {
                            startActivity(
                                intent
                            )
                        } catch (e: Exception) {

                            sendDebug(
                                """
                                🔴 GOOGLE HIBA
                                ❌ ${e.message}
                                """.trimIndent()
                            )
                        }

                    },
                    1200
                )

                return
            }

            speak(
                "Mit keressek?"
            )

            return
        }

        // =====================================================
        // APP MEGNYITÁS
        // =====================================================

        if (
            containsAny(
                command,
                listOf(
                    "nyisd meg",
                    "nyisd ki",
                    "nyisd",
                    "inditsd el",
                    "indítsd el",
                    "inditsd",
                    "indítsd",
                    "nyisd fel",
                    "menj a",
                    "menj be",
                    "nyisd meg nekem",
                    "nyisd ki nekem",
                    "indítsd el nekem",
                    "inditsd el nekem",
                    "nyisd meg légyszi",
                    "nyisd meg legyszi",
                    "indítsd el légyszi",
                    "inditsd el legyszi"
                )
            )
        ) {

            var appName =
                command

            listOf(
                "nyisd meg nekem",
                "nyisd meg",
                "nyisd ki nekem",
                "nyisd ki",
                "nyisd fel",
                "nyisd",
                "indítsd el nekem",
                "inditsd el nekem",
                "indítsd el",
                "inditsd el",
                "indítsd",
                "inditsd",
                "menj a",
                "menj be",
                "légyszi",
                "legyszi",
                "légy szíves",
                "legy szives"
            ).forEach {

                appName =
                    appName.replace(
                        normalizeText(it),
                        ""
                    )
            }

            appName =
                appName
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()

            val resolvedApp =
                resolveAppAlias(
                    appName
                )

            if (
                resolvedApp != null
            ) {

                sendDebug(
                    """
                    ⚙️ MŰVELET
                    App megnyitása:
                    "$resolvedApp"
                    """.trimIndent()
                )

                openApplication(
                    resolvedApp
                )

                return
            }

            if (
                appName.isNotBlank()
            ) {

                sendDebug(
                    """
                    ⚙️ MŰVELET
                    App keresése:
                    "$appName"
                    """.trimIndent()
                )

                openApplication(
                    appName
                )

                return
            }

            speak(
                "Melyik alkalmazást nyissam meg?"
            )

            return
        }

        // =====================================================
        // KÖZVETLEN APP NÉV
        // =====================================================

        val directApp =
            resolveAppAlias(
                command
            )

        if (
            directApp != null
        ) {

            sendDebug(
                """
                ⚙️ MŰVELET
                App megnyitása:
                "$directApp"
                """.trimIndent()
            )

            openApplication(
                directApp
            )

            return
        }

        // =====================================================
        // EGYSZERŰ VÁLASZOK
        // =====================================================

        if (
            containsAny(
                command,
                listOf(
                    "hogy vagy",
                    "hogy vagy nova",
                    "mi újság",
                    "miujsag",
                    "mi a helyzet",
                    "mizu",
                    "mizujs"
                )
            )
        ) {

            speak(
                "Köszi, jól vagyok. Futok és figyelek."
            )

            return
        }

        if (
            containsAny(
                command,
                listOf(
                    "köszönöm",
                    "koszonom",
                    "köszi",
                    "koszi",
                    "köszi nova",
                    "koszi nova"
                )
            )
        ) {

            speak(
                "Szívesen."
            )

            return
        }

        if (
            containsAny(
                command,
                listOf(
                    "mit tudsz",
                    "mire vagy képes",
                    "mire vagy kepes",
                    "mit tudsz nova"
                )
            )
        ) {

            speak(
                "Tudok alkalmazásokat megnyitni, keresni a Google-ben, időzítőt beállítani, és egyszerű parancsokat végrehajtani."
            )

            return
        }

        // =====================================================
        // ISMERETLEN
        // =====================================================

        sendDebug(
            """
            🟡 NOVA
            👂 Hallotta:
            "$text"

            🧠 Parancs:
            "$command"

            ⚙️ Művelet:
            Ismeretlen parancs
            """.trimIndent()
        )

        speak(
            "Ezt a parancsot még nem ismerem."
        )
    }

    // =========================================================
    // APP ALIAS FELISMERÉS
    // =========================================================

    private fun resolveAppAlias(
        input: String
    ): String? {

        val normalized =
            normalizeText(
                input
            )

        if (normalized.isBlank()) {
            return null
        }

        for (
            entry in appAliases
        ) {

            val officialName =
                entry.key

            val aliases =
                entry.value

            if (
                normalized ==
                normalizeText(
                    officialName
                )
            ) {
                return officialName
            }

            for (
                alias in aliases
            ) {

                val cleanAlias =
                    normalizeText(
                        alias
                    )

                if (
                    normalized ==
                    cleanAlias
                ) {
                    return officialName
                }

                if (
                    normalized.contains(
                        cleanAlias
                    )
                ) {
                    return officialName
                }
            }
        }

        return null
    }

    // =========================================================
    // APP MEGNYITÁSA
    // =========================================================

    private fun openApplication(
        name: String
    ) {

        val apps =
            packageManager
                .getInstalledApplications(
                    0
                )

        val searchName =
            normalizeText(
                name
            )

        for (
            app in apps
        ) {

            val label =
                packageManager
                    .getApplicationLabel(
                        app
                    )
                    .toString()

            val normalizedLabel =
                normalizeText(
                    label
                )

            if (
                normalizedLabel ==
                searchName ||
                normalizedLabel.contains(
                    searchName
                ) ||
                searchName.contains(
                    normalizedLabel
                )
            ) {

                val launchIntent =
                    packageManager
                        .getLaunchIntentForPackage(
                            app.packageName
                        )

                if (
                    launchIntent != null
                ) {

                    launchIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK

                    try {

                        startActivity(
                            launchIntent
                        )

                        speak(
                            "Oké, megnyitottam a $label alkalmazást."
                        )

                        sendDebug(
                            """
                            🟢 APP
                            ✅ Megnyitva:
                            $label
                            """.trimIndent()
                        )

                    } catch (e: Exception) {

                        sendDebug(
                            """
                            🔴 APP HIBA
                            $label
                            ❌ ${e.message}
                            """.trimIndent()
                        )

                        speak(
                            "Nem sikerült megnyitnom a $label alkalmazást."
                        )
                    }

                    return
                }
            }
        }

        sendDebug(
            """
            🟡 APP
            ❌ Nem található:
            "$name"
            """.trimIndent()
        )

        speak(
            "Ezt az alkalmazást nem találom."
        )
    }

    // =========================================================
    // SEGÉDFÜGGVÉNYEK
    // =========================================================

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
            .lowercase(
                Locale("hu", "HU")
            )
            .replace(
                "á",
                "a"
            )
            .replace(
                "é",
                "e"
            )
            .replace(
                "í",
                "i"
            )
            .replace(
                "ó",
                "o"
            )
            .replace(
                "ö",
                "o"
            )
            .replace(
                "ő",
                "o"
            )
            .replace(
                "ú",
                "u"
            )
            .replace(
                "ü",
                "u"
            )
            .replace(
                "ű",
                "u"
            )
            .replace(
                Regex("[.!?,;:]"),
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    // =========================================================
    // DEBUG
    // =========================================================

    private fun sendDebug(
        text: String
    ) {

        val intent =
            Intent(
                ACTION_DEBUG
            ).apply {

                setPackage(
                    packageName
                )

                putExtra(
                    EXTRA_DEBUG_TEXT,
                    text
                )
            }

        sendBroadcast(
            intent
        )
    }

    // =========================================================
    // SPEECH ERROR
    // =========================================================

    private fun getSpeechErrorText(
        error: Int
    ): String {

        return when (
            error
        ) {

            SpeechRecognizer
                .ERROR_AUDIO ->
                "Audio hiba"

            SpeechRecognizer
                .ERROR_CLIENT ->
                "Kliens hiba"

            SpeechRecognizer
                .ERROR_INSUFFICIENT_PERMISSIONS ->
                "Nincs mikrofonengedély"

            SpeechRecognizer
                .ERROR_NETWORK ->
                "Hálózati hiba"

            SpeechRecognizer
                .ERROR_NETWORK_TIMEOUT ->
                "Hálózati időtúllépés"

            SpeechRecognizer
                .ERROR_NO_MATCH ->
                "Nem értettem"

            SpeechRecognizer
                .ERROR_RECOGNIZER_BUSY ->
                "A felismerő foglalt"

            SpeechRecognizer
                .ERROR_SERVER ->
                "Szerver hiba"

            SpeechRecognizer
                .ERROR_SPEECH_TIMEOUT ->
                "Nem hallott beszédet"

            else ->
                "Ismeretlen speech hiba"
        }
    }

    // =========================================================
    // NOTIFICATION
    // =========================================================

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Nova Background",
                NotificationManager
                    .IMPORTANCE_LOW
            )

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        isListening = false
        isSpeaking = false
        ttsReady = false

        handler.removeCallbacksAndMessages(
            null
        )

        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }

        recognizer?.destroy()
        recognizer = null

        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }

        textToSpeech?.shutdown()
        textToSpeech = null

        sendDebug(
            """
            🔴 NOVA SERVICE
            Service: LEÁLLT
            🎤 Mikrofon: INAKTÍV
            🗣️ TTS: LEÁLLT
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
