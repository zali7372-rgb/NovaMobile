package com.nova.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var debugText: TextView

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            startNova()
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        createDebugPanel()

        requestPermissionsIfNeeded()
    }

    private fun createDebugPanel() {

        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    30,
                    40,
                    30,
                    30
                )
            }

        val title =
            TextView(this).apply {
                text = "NOVA DEBUG"
                textSize = 26f
                setPadding(
                    0,
                    0,
                    0,
                    25
                )
            }

        debugText =
            TextView(this).apply {
                text = """
                    🟢 Nova Debug

                    Service: indítás...
                    🎤 Mikrofon: ellenőrzés...
                    👂 Hallotta: -
                    🧠 Parancs: -
                    ⚙️ Művelet: -
                    ❌ Hiba: -
                """.trimIndent()

                textSize = 18f
            }

        root.addView(title)
        root.addView(debugText)

        val scroll =
            ScrollView(this).apply {
                addView(root)
            }

        setContentView(scroll)
    }

    private fun requestPermissionsIfNeeded() {

        val permissions =
            mutableListOf<String>()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {

            if (
                checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

        if (permissions.isNotEmpty()) {

            permissionLauncher.launch(
                permissions.toTypedArray()
            )

        } else {

            startNova()
        }
    }

    private fun startNova() {

        try {

            val intent =
                Intent(
                    this,
                    NovaService::class.java
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                startForegroundService(
                    intent
                )

            } else {

                startService(intent)
            }

            updateDebug(
                """
                🟢 Nova Debug

                Service: FUT
                🎤 Mikrofon: ENGEDÉLYEZVE
                👂 Hallotta: -
                🧠 Parancs: -
                ⚙️ Művelet: -
                ❌ Hiba: -

                NovaService elindítva.
                """.trimIndent()
            )

        } catch (e: Exception) {

            updateDebug(
                """
                🔴 NOVA HIBA

                Service: NEM FUT
                🎤 Mikrofon: ellenőrizd
                ❌ Hiba:
                ${e.message}
                """.trimIndent()
            )
        }
    }

    private fun updateDebug(
        message: String
    ) {

        runOnUiThread {

            if (::debugText.isInitialized) {
                debugText.text = message
            }
        }
    }
}
