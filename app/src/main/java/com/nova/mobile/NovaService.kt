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

    // ---------------------------------------------------------
    // SERVICE
    // ---------------------------------------------------------

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

        textToSpeech =
            TextToSpeech(
                this,
                this
            )

        setupRecognizer()

        handler.postDelayed(
            {
                startListening()
            },
            500
        )
    }

    // ---------------------------------------------------------
    // TEXT TO SPEECH
    // ---------------------------------------------------------

    override fun onInit(
        status: Int
    ) {

        if (
            status ==
            TextToSpeech.SUCCESS
        ) {

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

            if (ttsReady) {

                sendDebug(
                    """
                    🟢 TTS
                    🗣️ Állapot: KÉSZ
                    🇭🇺 Nyelv: magyar
                    """.trimIndent()
                )

            } else {

                sendDebug(
                    """
                    🔴 TTS
                    🗣️ Állapot: HIBA
                    ❌ Magyar TTS nem érhető el
                    """.trimIndent()
                )
            }

        } else {

            ttsReady = false

            sendDebug(
                """
                🔴 TTS
                🗣️ Állapot: NEM INDULT
                ❌ Text To Speech inicializálási hiba
                """.trimIndent()
            )
        }
    }

    private fun speak(
        text: String
    ) {

        sendDebug(
            """
            🗣️ NOVA BESZÉL
            "$text"
            """.trimIndent()
        )

        if (!ttsReady) {

            sendDebug(
                """
                🔴 TTS HIBA
                ❌ A TextToSpeech még nem áll készen
                """.trimIndent()
            )

            return
        }

        isSpeaking = true

        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "NOVA_RESPONSE"
        )

        /*
         * Biztonsági visszaállítás.
         * Ha a TTS callback valamiért nem érkezik meg,
         * Nova akkor is újra figyelni kezd.
         */
        handler.postDelayed(
            {
                isSpeaking = false

                if (!isListening) {
                    startListening()
                }
            },
            2200
        )
    }

    // ---------------------------------------------------------
    // SPEECH RECOGNIZER
    // ---------------------------------------------------------

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
                    // Hangerő változás.
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                    // Nem szükséges.
                }

                override fun onEndOfSpeech() {

                    isListening = false

                    sendDebug(
                        """
                        🟡 MIKROFON
                        🎤 Állapot: FELVÉTEL VÉGE
                        👂 Hallotta: feldolgozás...
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
                            ?.lowercase(
                                Locale("hu", "HU")
                            )
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

    // ---------------------------------------------------------
    // LISTENING
    // ---------------------------------------------------------

    private fun startListening() {

        if (isSpeaking) {
            return
        }

        if (isListening) {
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
            }

        try {

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

        } catch (
            e: Exception
        ) {

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

                if (!isSpeaking) {
                    startListening()
                }

            },
            delay
        )
    }

    // ---------------------------------------------------------
    // NOVA MEGSZÓLÍTÁSOK
    // ---------------------------------------------------------

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
            "na noa"
        )

    // ---------------------------------------------------------
    // APP ALIASOK
    // ---------------------------------------------------------

    private val appAliases =
        mapOf(

            "youtube" to listOf(
                "youtube",
                "you tube",
                "jutub",
                "jútub",
                "youtub",
                "youtubu",
                "jutube"
            ),

            "discord" to listOf(
                "discord",
                "diszkord",
                "discort",
                "disscord"
            ),

            "tiktok" to listOf(
                "tiktok",
                "tik tok",
                "tiktók",
                "tiktak"
            ),

            "chrome" to listOf(
                "chrome",
                "króm",
                "krom"
            ),

            "spotify" to listOf(
                "spotify",
                "spoty",
                "szpotifáj",
                "szpotify"
            ),

            "facebook" to listOf(
                "facebook",
                "facebookot",
                "fészbuk",
                "feszbuk"
            ),

            "messenger" to listOf(
                "messenger",
                "messengert",
                "mesenger",
                "messzi"
            ),

            "instagram" to listOf(
                "instagram",
                "insta",
                "instát",
                "instat"
            ),

            "google maps" to listOf(
                "google maps",
                "google map",
                "maps",
                "térkép",
                "google térkép"
            ),

            "gmail" to listOf(
                "gmail",
                "g mail",
                "gmailt"
            ),

            "roblox" to listOf(
                "roblox",
                "robloxot",
                "robloks"
            ),

            "minecraft" to listOf(
                "minecraft",
                "minecraftot",
                "mine craft"
            ),

            "whatsapp" to listOf(
                "whatsapp",
                "whats app",
                "vácáp"
            ),

            "telegram" to listOf(
                "telegram",
                "telegrám"
            ),

            "reddit" to listOf(
                "reddit",
                "redditet"
            ),

            "netflix" to listOf(
                "netflix",
                "netfliksz"
            ),

            "galéria" to listOf(
                "galéria",
                "galeria",
                "képek",
                "fotók",
                "fotok"
            ),

            "kamera" to listOf(
                "kamera",
                "kamerát",
                "kamerat"
            ),

            "beállítások" to listOf(
                "beállítások",
                "beallitasok",
                "beállítás",
                "beallitas"
            )
        )

    // ---------------------------------------------------------
    // COMMAND HANDLER
    // ---------------------------------------------------------

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

            restartListening(500)

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

            restartListening(500)

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

        // ---------------------------------------------
        // CSAK "NOVA"
        // ---------------------------------------------

        if (command.isBlank()) {

            speak(
                "Igen?"
            )

            return
        }

        // ---------------------------------------------
        // LEÁLLÍTÁS
        // ---------------------------------------------

        if (
            containsAny(
                command,
                listOf(
                    "allj",
                    "állj",
                    "kapcsold ki magad",
                    "allj le",
                    "állj le",
                    "leallhatsz",
                    "leállhatsz",
                    "stop",
                    "off",
                    "állj le nova",
                    "allj le nova"
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

        // ---------------------------------------------
        // IDŐZÍTŐ
        // ---------------------------------------------

        if (
            containsAny(
                command,
                listOf(
                    "idozito",
                    "időzítő",
                    "timer",
                    "állíts be időzítőt",
                    "allits be idozitot"
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
                        } catch (
                            e: Exception
                        ) {

                            sendDebug(
                                """
                                🔴 Időzítő hiba:
                                ${e.message}
                                """.trimIndent()
                            )
                        }

                    },
                    1200
                )

                return
            }

            speak(
                "Hány percre állítsam?"
            )

            return
        }

        // ---------------------------------------------
        // GOOGLE KERESÉS
        // ---------------------------------------------

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
                    "googlezz ra"
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
                "googlezz ra"
            ).forEach {
                query =
                    query.replace(
                        it,
                        ""
                    )
            }

            query =
                query.trim()

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
                        } catch (
                            e: Exception
                        ) {

                            sendDebug(
                                """
                                🔴 Google hiba:
                                ${e.message}
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

        // ---------------------------------------------
        // APP MEGNYITÁS
        // ---------------------------------------------

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
                    "menj be"
                )
            )
        ) {

            var appName =
                command

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
                "menj be"
            ).forEach {
                appName =
                    appName.replace(
                        it,
                        ""
                    )
            }

            appName =
                appName
                    .replace(
                        "ot",
                        ""
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

        // ---------------------------------------------
        // APP KERESÉSE PARANCS NÉLKÜL
        // ---------------------------------------------

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

        // ---------------------------------------------
        // ISMERETLEN
        // ---------------------------------------------

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

    // ---------------------------------------------------------
    // APP ALIAS FELISMERÉS
    // ---------------------------------------------------------

    private fun resolveAppAlias(
        input: String
    ): String? {

        val normalized =
            normalizeText(
                input
            )

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

    // ---------------------------------------------------------
    // APP MEGNYITÁSA
    // ---------------------------------------------------------

    private fun openApplication(
        name: String
    ) {

        val apps =
            packageManager
                .getInstalledApplications(0)

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

                    } catch (
                        e: Exception
                    ) {

                        sendDebug(
                            """
                            🔴 APP HIBA
                            $label
                            ${e.message}
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

    // ---------------------------------------------------------
    // DEBUG
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // SPEECH ERROR
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // NOTIFICATION
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // DESTROY
    // ---------------------------------------------------------

    override fun onDestroy() {

        isListening = false
        isSpeaking = false

        handler.removeCallbacksAndMessages(
            null
        )

        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null

        textToSpeech?.stop()
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
