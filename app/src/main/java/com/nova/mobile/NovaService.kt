package com.nova.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.net.URLEncoder
import java.util.Calendar
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

        textToSpeech = TextToSpeech(
            this,
            this
        )

        setupRecognizer()
        startListening()
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            textToSpeech?.language =
                Locale.getDefault()

            textToSpeech?.setSpeechRate(
                1.0f
            )

            textToSpeech?.setPitch(
                1.0f
            )
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
            1800
        )
    }

    private fun setupRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return
        }

        recognizer =
            SpeechRecognizer.createSpeechRecognizer(
                this
            )

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

                        android.os.Handler(
                            mainLooper
                        ).postDelayed(
                            {
                                startListening()
                            },
                            700
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
                            ?.lowercase(
                                Locale.getDefault()
                            )
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

            recognizer?.startListening(
                intent
            )

        } catch (_: Exception) {
        }
    }

    private fun handleCommand(
        originalText: String
    ) {

        val text = normalizeText(
            originalText
        )

       val novaNames = listOf(
    "nova",
    "noa",
    "novaa",
    "novi",
    "novus",
    "novacska",
    "nova ai",
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
    "nova legyszi",
    "nova legy szives",
    "noa legyszi",
    "noa legy szives"
)

val novaTriggered =
    novaNames.any { name ->
        text.contains(name)
    }

if (!novaTriggered) {
    return
}

        val command = text
            .replace("nova", "")
            .replace("noa", "")
            .trim()

        if (command.isBlank()) {

            speak(
                listOf(
                    "Igen?",
                    "Hallgatlak.",
                    "Miben segíthetek?",
                    "Parancs?",
                    "Itt vagyok.",
                    "Mondjad.",
                    "Mit szeretnél?",
                    "Figyelek.",
                    "Igen, itt vagyok.",
                    "Miben segíthetek?"
                ).random()
            )

            return
        }

        if (handleStopCommand(command)) {
            return
        }

        if (handleTimerCommand(command)) {
            return
        }

        if (handleSearchCommand(command)) {
            return
        }

        if (handleCallCommand(command)) {
            return
        }

        if (handleTimeCommand(command)) {
            return
        }

        if (handleDateCommand(command)) {
            return
        }

        if (handleWebsiteCommand(command)) {
            return
        }

        if (handleVolumeCommand(command)) {
            return
        }

        if (handleSettingsCommand(command)) {
            return
        }

        if (handleApplicationCommand(command)) {
            return
        }

        speak(
            "Ezt a parancsot még nem ismerem."
        )
    }

    private fun normalizeText(
        text: String
    ): String {

        return text
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
            .trim()
    }

    private fun handleStopCommand(
        text: String
    ): Boolean {

        val commands = listOf(
            "allj",
            "allj le",
            "allj meg",
            "kapcsold ki",
            "kapcsold le",
            "nova allj",
            "nova allj le",
            "fejezd be",
            "fejezd be a novat",
            "allitsd le magad",
            "leallitas",
            "allj le nova",
            "nova kikapcsolasa",
            "kapcsold ki a novat",
            "nova alljon le",
            "allj mar",
            "hagyd abba",
            "stop",
            "off"
        )

        if (
            commands.any {
                text.contains(it)
            }
        ) {

            speak("Rendben, leállok.")

            android.os.Handler(
                mainLooper
            ).postDelayed(
                {
                    stopSelf()
                },
                1200
            )

            return true
        }

        return false
    }

    private fun handleTimerCommand(
        text: String
    ): Boolean {

        val timerWords = listOf(
            "idozito",
            "idozitot",
            "idozites",
            "timer",
            "allits be egy idozitot",
            "allits be idozitot",
            "indits egy idozitot",
            "indits idozitot",
            "tegyel be egy idozitot",
            "tegyel be idozitot",
            "kell egy idozito",
            "szeretnek egy idozitot"
        )

        if (
            !timerWords.any {
                text.contains(it)
            }
        ) {
            return false
        }

        val number =
            Regex("""(\d+)""")
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()

        if (number == null) {

            speak(
                "Hány perces időzítőt állítsak?"
            )

            return true
        }

        speak(
            "$number perces időzítőt állítok be."
        )

        val intent = Intent(
            AlarmClock.ACTION_SET_TIMER
        ).apply {

            putExtra(
                AlarmClock.EXTRA_LENGTH,
                (number * 60).toInt()
            )

            putExtra(
                AlarmClock.EXTRA_SKIP_UI,
                false
            )

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK
        }

        android.os.Handler(
            mainLooper
        ).postDelayed(
            {
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                }
            },
            1000
        )

        return true
    }

    private fun handleSearchCommand(
        text: String
    ): Boolean {

        val searchWords = listOf(
            "keress ra",
            "keress",
            "keress ra erre",
            "googlezd meg",
            "keress ra googleben",
            "keresd meg",
            "keresd ki",
            "nezz utana",
            "nezz ra",
            "keress ra erre a dologra",
            "talald meg",
            "keress nekem",
            "keress valamit",
            "google keres",
            "google kereses",
            "indits google keresest"
        )

        if (
            !searchWords.any {
                text.contains(it)
            }
        ) {
            return false
        }

        var query = text

        searchWords.forEach {
            query = query.replace(
                it,
                ""
            )
        }

        query = query
            .trim()
            .removePrefix("a ")
            .removePrefix("az ")
            .trim()

        if (query.isBlank()) {

            speak(
                "Mit keressek?"
            )

            return true
        }

        speak(
            "Rákeresek."
        )

        val url =
            "https://www.google.com/search?q=" +
                URLEncoder.encode(
                    query,
                    "UTF-8"
                )

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK
        }

        android.os.Handler(
            mainLooper
        ).postDelayed(
            {
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                }
            },
            900
        )

        return true
    }

    private fun handleCallCommand(
        text: String
    ): Boolean {

        val words = listOf(
            "hivd fel",
            "hivd",
            "telefonalj",
            "telefonalj fel",
            "indits hivast",
            "hivast indits",
            "hivj fel",
            "csorogj ra",
            "hivd fel ezt a szamot",
            "telefonalj ennek",
            "hivast kezdemenyezz",
            "indits telefonhivast"
        )

        if (
            !words.any {
                text.contains(it)
            }
        ) {
            return false
        }

        val number =
            Regex("""[\d +()-]{6,}""")
                .find(text)
                ?.value
                ?.trim()

        if (number.isNullOrBlank()) {

            speak(
                "Melyik számot hívjam?"
            )

            return true
        }

        speak(
            "Megnyitom a hívást."
        )

        val intent = Intent(
            Intent.ACTION_DIAL
        ).apply {

            data = Uri.parse(
                "tel:" + number
            )

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK
        }

        android.os.Handler(
            mainLooper
        ).postDelayed(
            {
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                }
            },
            900
        )

        return true
    }

    private fun handleTimeCommand(
        text: String
    ): Boolean {

        val words = listOf(
            "mennyi az ido",
            "hany ora van",
            "hany ora",
            "mondd meg az idot",
            "mi az ido",
            "pontos ido",
            "aktualis ido",
            "mennyi a pontos ido"
        )

        if (
            !words.any {
                text.contains(it)
            }
        ) {
            return false
        }

        val calendar =
            Calendar.getInstance()

        val hour =
            calendar.get(Calendar.HOUR_OF_DAY)

        val minute =
            calendar.get(Calendar.MINUTE)

        speak(
            "Most $hour óra $minute perc van."
        )

        return true
    }

    private fun handleDateCommand(
        text: String
    ): Boolean {

        val words = listOf(
            "mai datum",
            "milyen nap van",
            "hanyadika van",
            "mi a datum",
            "mondd meg a datumot",
            "milyen datum van",
            "mai nap",
            "melyik nap van ma"
        )

        if (
            !words.any {
                text.contains(it)
            }
        ) {
            return false
        }

        val calendar =
            Calendar.getInstance()

        val day =
            calendar.get(Calendar.DAY_OF_MONTH)

        val month =
            calendar.get(Calendar.MONTH) + 1

        val year =
            calendar.get(Calendar.YEAR)

        speak(
            "Ma $year. év $month. hónap $day. napja van."
        )

        return true
    }

    private fun handleWebsiteCommand(
        text: String
    ): Boolean {

        val websites =
            mapOf(
                "youtube" to "https://youtube.com",
                "google" to "https://google.com",
                "facebook" to "https://facebook.com",
                "instagram" to "https://instagram.com",
                "tiktok" to "https://tiktok.com",
                "discord" to "https://discord.com",
                "reddit" to "https://reddit.com",
                "wikipedia" to "https://wikipedia.org"
            )

        val openWords = listOf(
            "nyisd meg",
            "nyisd ki",
            "nyisd",
            "menj a",
            "lepj a",
            "nyisd meg az",
            "inditsd el"
        )

        if (
            !openWords.any {
                text.contains(it)
            }
        ) {
            return false
        }

        for (
            entry in websites
        ) {

            if (
                text.contains(
                    entry.key
                )
            ) {

                speak(
                    "Megnyitom."
                )

                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            entry.value
                        )
                    ).apply {

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                android.os.Handler(
                    mainLooper
                ).postDelayed(
                    {
                        try {
                            startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    900
                )

                return true
            }
        }

        return false
    }

    private fun handleVolumeCommand(
        text: String
    ): Boolean {

        val audio =
            getSystemService(
                AUDIO_SERVICE
            ) as AudioManager

        if (
            text.contains("hangero fel") ||
            text.contains("hangositsd fel") ||
            text.contains("hangositsd")
        ) {

            audio.adjustVolume(
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )

            speak("Felvettem a hangerőt.")

            return true
        }

        if (
            text.contains("hangero le") ||
            text.contains("halkitsd le") ||
            text.contains("halkitsd")
        ) {

            audio.adjustVolume(
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )

            speak("Levettem a hangerőt.")

            return true
        }

        if (
            text.contains("nemitsd el") ||
            text.contains("ne mitsd el") ||
            text.contains("nema")
        ) {

            audio.adjustVolume(
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI
            )

            speak("Lenémítottam.")

            return true
        }

        return false
    }

    private fun handleSettingsCommand(
        text: String
    ): Boolean {

        if (
            text.contains(
                "nyisd meg a beallitasokat"
            ) ||
            text.contains(
                "nyisd meg a beallitast"
            ) ||
            text.contains(
                "beallitasok megnyitasa"
            ) ||
            text.contains(
                "inditsd el a beallitasokat"
            ) ||
            text.contains(
                "menj a beallitasokhoz"
            )
        ) {

            speak(
                "Megnyitom a beállításokat."
            )

            val intent = Intent(
                Settings.ACTION_SETTINGS
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK
            }

            android.os.Handler(
                mainLooper
            ).postDelayed(
                {
                    try {
                        startActivity(intent)
                    } catch (_: Exception) {
                    }
                },
                900
            )

            return true
        }

        return false
    }

    private fun handleApplicationCommand(
        text: String
    ): Boolean {

        val openWords = listOf(
            "nyisd meg",
            "nyisd ki",
            "nyisd",
            "inditsd el",
            "inditsd",
            "nyissa meg",
            "nyissa ki",
            "nyisd meg nekem",
            "nyisd ki nekem",
            "menj a",
            "menj bele",
            "lepj be",
            "lepj a",
            "kapcsold be",
            "inditsd be",
            "indits el",
            "inditsd el nekem",
            "nyisd meg nekem a",
            "mutasd",
            "hozd be",
            "nyisd ki nekem",
            "inditsd el nekem"
        )

        if (
            !openWords.any {
                text.contains(it)
            }
        ) {
            return false
        }

        val apps =
            mapOf(

                "youtube" to listOf(
                    "youtube",
                    "youtubeot",
                    "youtube ra",
                    "youtube app"
                ),

                "discord" to listOf(
                    "discord",
                    "discordot",
                    "discord app"
                ),

                "spotify" to listOf(
                    "spotify",
                    "spotifyt",
                    "spotify app"
                ),

                "tiktok" to listOf(
                    "tiktok",
                    "tiktokot",
                    "tiktok app"
                ),

                "chrome" to listOf(
                    "chrome",
                    "chromeot",
                    "google chrome"
                ),

                "maps" to listOf(
                    "maps",
                    "google maps",
                    "mapset",
                    "terkep",
                    "terkepeket"
                ),

                "kamera" to listOf(
                    "kamera",
                    "kamerat",
                    "kamera app"
                ),

                "galeria" to listOf(
                    "galeria",
                    "galeriat",
                    "kepek",
                    "fotok"
                ),

                "gmail" to listOf(
                    "gmail",
                    "gmailt",
                    "gmail app"
                ),

                "telefon" to listOf(
                    "telefon",
                    "telefont",
                    "telefon app"
                ),

                "uzenetek" to listOf(
                    "uzenetek",
                    "uzeneteket",
                    "uzenet",
                    "sms"
                ),

                "beallitasok" to listOf(
                    "beallitasok",
                    "beallitas",
                    "beallitast"
                ),

                "ora" to listOf(
                    "ora",
                    "orat",
                    "ora app",
                    "orak"
                ),

                "naptar" to listOf(
                    "naptar",
                    "naptarat",
                    "calendar"
                ),

                "fajlok" to listOf(
                    "fajlok",
                    "fajlkezelo",
                    "fajlokat",
                    "dokumentumok"
                )
            )

        for (
            appEntry in apps
        ) {

            val aliases =
                appEntry.value

            if (
                aliases.any {
                    text.contains(it)
                }
            ) {

                val target =
                    appEntry.key

                speak(
                    "Megnyitom."
                )

                android.os.Handler(
                    mainLooper
                ).postDelayed(
                    {
                        launchKnownApplication(
                            target
                        )
                    },
                    900
                )

                return true
            }
        }

        return false
    }

    private fun launchKnownApplication(
        target: String
    ) {

        val installedApps =
            packageManager
                .getInstalledApplications(
                    PackageManager.GET_META_DATA
                )

        val aliases =
            when (target) {

                "youtube" ->
                    listOf(
                        "youtube"
                    )

                "discord" ->
                    listOf(
                        "discord"
                    )

                "spotify" ->
                    listOf(
                        "spotify"
                    )

                "tiktok" ->
                    listOf(
                        "tiktok"
                    )

                "chrome" ->
                    listOf(
                        "chrome"
                    )

                "maps" ->
                    listOf(
                        "maps",
                        "térkép"
                    )

                "kamera" ->
                    listOf(
                        "kamera",
                        "camera"
                    )

                "galeria" ->
                    listOf(
                        "galéria",
                        "galery",
                        "photos"
                    )

                "gmail" ->
                    listOf(
                        "gmail"
                    )

                "telefon" ->
                    listOf(
                        "telefon",
                        "phone"
                    )

                "uzenetek" ->
                    listOf(
                        "üzenetek",
                        "messages",
                        "messages"
                    )

                "beallitasok" ->
                    listOf(
                        "beállítások",
                        "settings"
                    )

                "ora" ->
                    listOf(
                        "óra",
                        "clock"
                    )

                "naptar" ->
                    listOf(
                        "naptár",
                        "calendar"
                    )

                "fajlok" ->
                    listOf(
                        "fájlok",
                        "files",
                        "file manager"
                    )

                else ->
                    emptyList()
            }

        for (
            app in installedApps
        ) {

            val label =
                packageManager
                    .getApplicationLabel(app)
                    .toString()
                    .lowercase(
                        Locale.getDefault()
                    )

            if (
                aliases.any {
                    label.contains(it)
                }
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
                    } catch (_: Exception) {
                    }

                    return
                }
            }
        }

        speak(
            "Ezt az alkalmazást nem találom a telefonon."
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
