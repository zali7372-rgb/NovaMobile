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

class NovaService :
    Service(),
    TextToSpeech.OnInitListener {

    private var recognizer:
        SpeechRecognizer? = null

    private var textToSpeech:
        TextToSpeech? = null

    private var isListening =
        false

    private var isSpeaking =
        false

    private var ttsReady =
        false

    private var serviceDestroyed =
        false

    private val handler =
        Handler(mainLooper)

    companion object {

        const val ACTION_DEBUG =
            "com.nova.mobile.DEBUG"

        const val EXTRA_DEBUG_TEXT =
            "debug_text"

        private const val CHANNEL_ID =
            "nova_service"

        private const val NOTIFICATION_ID =
            1001

        private const val TTS_TIMEOUT =
            10000L
    }

    // =====================================================
    // SERVICE INDÍTÁS
    // =====================================================

    override fun onCreate() {

        super.onCreate()

        serviceDestroyed =
            false

        createNotificationChannel()

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "Nova"
                )
                .setContentText(
                    "Nova figyel a háttérben"
                )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(
                    true
                )
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

            🎤 Mikrofon:
            INICIALIZÁLÁS...

            🗣️ TTS:
            INICIALIZÁLÁS...

            👂 Hallotta:
            -

            🧠 Parancs:
            -

            ⚙️ Művelet:
            -

            ❌ Hiba:
            -
            """.trimIndent()
        )

        initializeTts()

        setupRecognizer()

        handler.postDelayed(
            {

                if (
                    !serviceDestroyed
                ) {

                    startListening()
                }

            },
            1200
        )

        handler.postDelayed(
            {

                if (
                    !ttsReady &&
                    !serviceDestroyed
                ) {

                    sendDebug(
                        """
                        🔴 TTS TIMEOUT

                        🗣️ TTS:
                        10 másodperc után sem lett kész.

                        🎤 Mikrofon:
                        A SpeechRecognizer ettől még megpróbál működni.

                        ❌ Ellenőrizd:
                        Beállítások →
                        Általános kezelés →
                        Szövegfelolvasás →
                        Preferált motor
                        """.trimIndent()
                    )
                }

            },
            TTS_TIMEOUT
        )
    }

    // =====================================================
    // TTS
    // =====================================================

    private fun initializeTts() {

        sendDebug(
            """
            🗣️ TTS

            Állapot:
            MOTOR INDÍTÁSA...

            🇭🇺 Nyelv:
            Magyar
            """.trimIndent()
        )

        try {

            textToSpeech =
                TextToSpeech(
                    applicationContext,
                    this
                )

        } catch (
            e: Exception
        ) {

            ttsReady =
                false

            sendDebug(
                """
                🔴 TTS HIBA

                ${e.javaClass.simpleName}

                ${e.message}
                """.trimIndent()
            )
        }
    }

    override fun onInit(
        status: Int
    ) {

        if (
            serviceDestroyed
        ) {
            return
        }

        if (
            status !=
            TextToSpeech.SUCCESS
        ) {

            ttsReady =
                false

            sendDebug(
                """
                🔴 TTS

                Állapot:
                HIBA

                ❌ Inicializálás sikertelen

                Kód:
                $status
                """.trimIndent()
            )

            return
        }

        try {

            val result =
                textToSpeech?.setLanguage(
                    Locale(
                        "hu",
                        "HU"
                    )
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

            if (
                ttsReady
            ) {

                sendDebug(
                    """
                    🟢 TTS

                    Állapot:
                    KÉSZ

                    🇭🇺 Nyelv:
                    Magyar

                    🎤 Mikrofon:
                    A SpeechRecognizer indulhat.
                    """.trimIndent()
                )

            } else {

                sendDebug(
                    """
                    🔴 TTS

                    Állapot:
                    HIBA

                    ❌ Magyar hangadat
                    hiányzik vagy nem támogatott.
                    """.trimIndent()
                )
            }

        } catch (
            e: Exception
        ) {

            ttsReady =
                false

            sendDebug(
                """
                🔴 TTS HIBA

                ${e.javaClass.simpleName}

                ${e.message}
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

        if (
            !ttsReady ||
            textToSpeech == null
        ) {

            sendDebug(
                """
                🔴 TTS HIBA

                A TextToSpeech még
                nem áll készen.
                """.trimIndent()
            )

            return
        }

        isSpeaking =
            true

        if (
            isListening
        ) {

            try {
                recognizer?.cancel()
            } catch (
                _: Exception
            ) {
            }

            isListening =
                false
        }

        try {

            textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "NOVA_RESPONSE"
            )

        } catch (
            e: Exception
        ) {

            isSpeaking =
                false

            sendDebug(
                """
                🔴 TTS BESZÉD HIBA

                ${e.message}
                """.trimIndent()
            )

            restartListening(
                500
            )

            return
        }

        handler.postDelayed(
            {

                isSpeaking =
                    false

                if (
                    !serviceDestroyed
                ) {

                    startListening()
                }

            },
            calculateSpeechDelay(
                text
            )
        )
    }

    private fun calculateSpeechDelay(
        text: String
    ): Long {

        val calculated =
            900L +
            (
                text.length * 45L
            )

        return calculated.coerceIn(
            1500L,
            6000L
        )
    }

    // =====================================================
    // SPEECH RECOGNIZER
    // =====================================================

    private fun setupRecognizer() {

        if (
            !SpeechRecognizer
                .isRecognitionAvailable(
                    applicationContext
                )
        ) {

            sendDebug(
                """
                🔴 SPEECH RECOGNIZER

                🎤 Mikrofon:
                NEM ELÉRHETŐ

                ❌ A készülék nem támogatja
                a beszédfelismerést.
                """.trimIndent()
            )

            return
        }

        try {

            recognizer?.destroy()

            recognizer =
                SpeechRecognizer
                    .createSpeechRecognizer(
                        applicationContext
                    )

            recognizer?.setRecognitionListener(
                object :
                    RecognitionListener {

                    override fun onReadyForSpeech(
                        params: Bundle?
                    ) {

                        isListening =
                            true

                        sendDebug(
                            """
                            🟢 MIKROFON

                            🎤 Állapot:
                            AKTÍV

                            👂 Állapot:
                            HALLGATLAK...

                            🗣️ TTS:
                            ${
                                if (ttsReady)
                                    "KÉSZ"
                                else
                                    "MÉG INICIALIZÁLÓDIK"
                            }
                            """.trimIndent()
                        )
                    }

                    override fun onBeginningOfSpeech() {

                        sendDebug(
                            """
                            🟢 MIKROFON

                            🎤 Állapot:
                            BESZÉDET ÉSZLEL

                            👂 Hallotta:
                            ...

                            🧠 Parancs:
                            FELISMERÉS FOLYAMATBAN...
                            """.trimIndent()
                        )
                    }

                    override fun onRmsChanged(
                        rmsdB: Float
                    ) {

                        // A rendszer hangereje.
                    }

                    override fun onBufferReceived(
                        buffer: ByteArray?
                    ) {
                    }

                    override fun onEndOfSpeech() {

                        isListening =
                            false

                        sendDebug(
                            """
                            🟡 MIKROFON

                            🎤 Állapot:
                            FELVÉTEL VÉGE

                            👂 Hallotta:
                            FELDOLGOZÁS...

                            🧠 Parancs:
                            FELDOLGOZÁS...
                            """.trimIndent()
                        )
                    }

                    override fun onError(
                        error: Int
                    ) {

                        isListening =
                            false

                        val errorText =
                            getSpeechErrorText(
                                error
                            )

                        sendDebug(
                            """
                            🔴 SPEECH HIBA

                            🎤 Mikrofon:
                            HIBA

                            ❌ Kód:
                            $error

                            ❌ Hiba:
                            $errorText

                            🔄 Újrapróbálkozás...
                            """.trimIndent()
                        )

                        restartListening(
                            1000
                        )
                    }

                    override fun onResults(
                        results: Bundle?
                    ) {

                        isListening =
                            false

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
                            🟢 FELISMERÉS KÉSZ

                            👂 Hallotta:
                            "$text"

                            🧠 Parancs:
                            FELDOLGOZÁS...

                            ⚙️ Művelet:
                            -
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

        } catch (
            e: Exception
        ) {

            recognizer =
                null

            sendDebug(
                """
                🔴 SPEECH RECOGNIZER HIBA

                ${e.javaClass.simpleName}

                ${e.message}
                """.trimIndent()
            )
        }
    }

    // =====================================================
    // HALLGATÁS
    // =====================================================

    private fun startListening() {

        if (
            serviceDestroyed
        ) {
            return
        }

        if (
            isSpeaking
        ) {
            return
        }

        if (
            isListening
        ) {
            return
        }

        if (
            recognizer == null
        ) {

            setupRecognizer()
        }

        if (
            recognizer == null
        ) {

            sendDebug(
                """
                🔴 MIKROFON

                SpeechRecognizer:
                NINCS
                """.trimIndent()
            )

            restartListening(
                2000
            )

            return
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
                        .EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,
                    false
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

            recognizer?.startListening(
                intent
            )

            sendDebug(
                """
                🟢 HALLGATÁS

                🎤 Mikrofon:
                INDÍTÁS

                👂 Hallotta:
                -

                🧠 Parancs:
                -

                ⚙️ Művelet:
                -

                🗣️ TTS:
                ${
                    if (ttsReady)
                        "KÉSZ"
                    else
                        "MÉG INICIALIZÁLÓDIK"
                }
                """.trimIndent()
            )

        } catch (
            e: Exception
        ) {

            isListening =
                false

            sendDebug(
                """
                🔴 HALLGATÁS HIBA

                ❌ ${e.javaClass.simpleName}

                ${e.message}
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

        if (
            serviceDestroyed
        ) {
            return
        }

        handler.postDelayed(
            {

                if (
                    !serviceDestroyed &&
                    !isSpeaking &&
                    !isListening
                ) {

                    startListening()
                }

            },
            delay
        )
    }

    // =====================================================
    // NOVA MEGSZÓLÍTÁSOK
    // =====================================================

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

    // =====================================================
    // APP ALIASOK
    // =====================================================

    private val appAliases =
        mapOf(

            "youtube" to listOf(
                "youtube",
                "youtubeot",
                "youtubot",
                "you tube",
                "jutub",
                "jútub",
                "youtub",
                "youtubu",
                "jutube",
                "youtube alkalmazas",
                "youtube app"
            ),

            "discord" to listOf(
                "discord",
                "discordot",
                "diszkord",
                "diszkordot",
                "discort",
                "disscord",
                "discord alkalmazas"
            ),

            "tiktok" to listOf(
                "tiktok",
                "tiktokot",
                "tik tok",
                "tiktók",
                "tiktak",
                "tiktok alkalmazas"
            ),

            "chrome" to listOf(
                "chrome",
                "chromot",
                "króm",
                "krom",
                "chrome böngésző",
                "chrome bongeszo"
            ),

            "spotify" to listOf(
                "spotify",
                "spoty",
                "szpotifáj",
                "szpotifaj",
                "szpotify",
                "spotifyt"
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
                "instagramot",
                "insta",
                "instát",
                "instat"
            ),

            "google maps" to listOf(
                "google maps",
                "google map",
                "maps",
                "térkép",
                "terkep",
                "google térkép",
                "google terkep"
            ),

            "gmail" to listOf(
                "gmail",
                "g mail",
                "gmailt",
                "gmail alkalmazas"
            ),

            "roblox" to listOf(
                "roblox",
                "robloxot",
                "robloks",
                "roblox alkalmazas"
            ),

            "minecraft" to listOf(
                "minecraft",
                "minecraftot",
                "mine craft",
                "minecraft alkalmazas"
            ),

            "whatsapp" to listOf(
                "whatsapp",
                "whatsappot",
                "whats app",
                "vácáp",
                "vacap"
            ),

            "telegram" to listOf(
                "telegram",
                "telegramot",
                "telegrám"
            ),

            "reddit" to listOf(
                "reddit",
                "redditet",
                "redditot"
            ),

            "netflix" to listOf(
                "netflix",
                "netflixet",
                "netfliksz"
            ),

            "galéria" to listOf(
                "galéria",
                "galeria",
                "galériát",
                "galeriat",
                "képek",
                "kepek",
                "fotók",
                "fotok"
            ),

            "kamera" to listOf(
                "kamera",
                "kamerát",
                "kamerat",
                "kamerát nyisd",
                "kamera alkalmazas"
            ),

            "beállítások" to listOf(
                "beállítások",
                "beallitasok",
                "beállítás",
                "beallitas",
                "settings"
            ),

            "telefon" to listOf(
                "telefon",
                "telefon alkalmazas",
                "hívások",
                "hivasok",
                "tárcsázó",
                "tarcsazo"
            ),

            "óra" to listOf(
                "óra",
                "ora",
                "órát",
                "orat",
                "clock"
            ),

            "naptár" to listOf(
                "naptár",
                "naptar",
                "calendar"
            ),

            "fájlok" to listOf(
                "fájlok",
                "fajlok",
                "fájlkezelő",
                "fajlkezelo",
                "my files"
            ),

            "galéria" to listOf(
                "galeria",
                "galéria",
                "képek",
                "kepek",
                "fotók",
                "fotok"
            )
        )

    // =====================================================
    // PARANCSFELDOLGOZÁS
    // =====================================================

    private fun handleCommand(
        originalText: String
    ) {

        if (
            originalText.isBlank()
        ) {

            sendDebug(
                """
                🟡 FELISMERÉS

                👂 Hallotta:
                ""

                🧠 Parancs:
                ÜRES

                ⚙️ Művelet:
                NINCS
                """.trimIndent()
            )

            restartListening(
                500
            )

            return
        }

        val normalized =
            normalizeText(
                originalText
            )

        val triggered =
            novaNames.any {
                normalized.contains(
                    normalizeText(it)
                )
            }

        if (
            !triggered
        ) {

            sendDebug(
                """
                🟡 NOVA

                👂 Hallotta:
                "$originalText"

                🧠 Parancs:
                Nem volt Nova megszólítás.

                ⚙️ Művelet:
                NINCS
                """.trimIndent()
            )

            restartListening(
                400
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
            🟢 NOVA PARANCS

            👂 Hallotta:
            "$originalText"

            🧠 Parancs:
            "$command"

            ⚙️ Művelet:
            FELDOLGOZÁS...
            """.trimIndent()
        )

        if (
            command.isBlank()
        ) {

            speak(
                "Igen?"
            )

            return
        }

        // =================================================
        // STOP
        // =================================================

        if (
            containsAny(
                command,
                listOf(
                    "állj",
                    "allj",
                    "állj le",
                    "allj le",
                    "kapcsold ki magad",
                    "leállhatsz",
                    "leallhatsz",
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
                2000
            )

            return
        }

        // =================================================
        // IDŐZÍTŐ
        // =================================================

        if (
            containsAny(
                command,
                listOf(
                    "időzítő",
                    "idozito",
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
                    .find(
                        command
                    )
                    ?.groupValues
                    ?.getOrNull(
                        1
                    )
                    ?.toLongOrNull()

            if (
                minutes != null
            ) {

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
                                🔴 IDŐZÍTŐ HIBA

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

        // =================================================
        // GOOGLE KERESÉS
        // =================================================

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
                                🔴 GOOGLE HIBA

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

        // =================================================
        // APP MEGNYITÁS
        // =================================================

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
                    "nyisd fel",
                    "menj a",
                    "menj be",
                    "nyisd meg az",
                    "indítsd el az"
                )
            )
        ) {

            var appName =
                command

            listOf(
                "nyisd meg az",
                "nyisd meg a",
                "nyisd meg",
                "nyisd ki az",
                "nyisd ki a",
                "nyisd ki",
                "nyisd az",
                "nyisd a",
                "nyisd",
                "indítsd el az",
                "inditsd el az",
                "indítsd el a",
                "inditsd el a",
                "indítsd el",
                "inditsd el",
                "indítsd",
                "inditsd",
                "nyisd fel",
                "menj a",
                "menj be"
            ).forEach {

                appName =
                    appName.replace(
                        it,
                        " "
                    )
            }

            appName =
                cleanHungarianObjectSuffix(
                    appName
                )

            val resolved =
                resolveAppAlias(
                    appName
                )

            if (
                resolved != null
            ) {

                sendDebug(
                    """
                    ⚙️ MŰVELET

                    App megnyitása:
                    "$resolved"
                    """.trimIndent()
                )

                openApplication(
                    resolved
                )

                return
            }

            if (
                appName.isNotBlank()
            ) {

                sendDebug(
                    """
                    ⚙️ MŰVELET

                    Ismeretlen app keresése:
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

        // =================================================
        // KÖZVETLEN APPNÉV
        // =================================================

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

        // =================================================
        // EGYSZERŰ VÁLASZOK
        // =================================================

        if (
            containsAny(
                command,
                listOf(
                    "hogy vagy",
                    "hogy vagy nova",
                    "miujsag",
                    "mi újság",
                    "mi a helyzet"
                )
            )
        ) {

            speak(
                "Köszi, megvagyok. Figyelek."
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
                    "koszi"
                )
            )
        ) {

            speak(
                "Szívesen."
            )

            return
        }

        // =================================================
        // ISMERETLEN
        // =================================================

        sendDebug(
            """
            🟡 NOVA

            👂 Hallotta:
            "$originalText"

            🧠 Parancs:
            "$command"

            ⚙️ Művelet:
            ISMERETLEN PARANCS
            """.trimIndent()
        )

        speak(
            "Ezt a parancsot még nem ismerem."
        )
    }

    // =====================================================
    // APP ALIAS
    // =====================================================

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

            if (
                normalized ==
                normalizeText(
                    officialName
                )
            ) {

                return officialName
            }

            for (
                alias in entry.value
            ) {

                if (
                    normalized.contains(
                        normalizeText(
                            alias
                        )
                    )
                ) {

                    return officialName
                }
            }
        }

        return null
    }

    // =====================================================
    // APP MEGNYITÁSA
    // =====================================================

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

                        sendDebug(
                            """
                            🟢 APP

                            ⚙️ Művelet:
                            $label megnyitva

                            🗣️ Nova:
                            Oké, megnyitottam a
                            $label alkalmazást.
                            """.trimIndent()
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

                            App:
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

    // =====================================================
    // SEGÉDFÜGGVÉNYEK
    // =====================================================

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

    private fun cleanHungarianObjectSuffix(
        input: String
    ): String {

        var result =
            input.trim()

        val suffixes =
            listOf(
                "ot",
                "et",
                "öt",
                "at",
                "t"
            )

        for (
            suffix in suffixes
        ) {

            if (
                result.endsWith(
                    suffix
                ) &&
                result.length >
                suffix.length + 2
            ) {

                result =
                    result.dropLast(
                        suffix.length
                    )

                break
            }
        }

        return result.trim()
    }

    private fun normalizeText(
        text: String
    ): String {

        return text
            .lowercase(
                Locale(
                    "hu",
                    "HU"
                )
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
                Regex(
                    "[.!?,;:]"
                ),
                " "
            )
            .replace(
                Regex(
                    "\\s+"
                ),
                " "
            )
            .trim()
    }

    // =====================================================
    // DEBUG
    // =====================================================

    private fun sendDebug(
        text: String
    ) {

        if (
            serviceDestroyed
        ) {
            return
        }

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

    // =====================================================
    // SPEECH HIBÁK
    // =====================================================

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

    // =====================================================
    // NOTIFICATION
    // =====================================================

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

    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {

        serviceDestroyed =
            true

        isListening =
            false

        isSpeaking =
            false

        handler.removeCallbacksAndMessages(
            null
        )

        try {
            recognizer?.cancel()
        } catch (
            _: Exception
        ) {
        }

        try {
            recognizer?.destroy()
        } catch (
            _: Exception
        ) {
        }

        recognizer =
            null

        try {
            textToSpeech?.stop()
        } catch (
            _: Exception
        ) {
        }

        try {
            textToSpeech?.shutdown()
        } catch (
            _: Exception
        ) {
        }

        textToSpeech =
            null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}
