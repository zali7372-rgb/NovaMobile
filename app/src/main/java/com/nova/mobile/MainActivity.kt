package com.nova.mobile

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var debugText: TextView
    private lateinit var scrollView: ScrollView

    private val debugReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    intent?.action ==
                    NovaService.ACTION_DEBUG
                ) {

                    val text =
                        intent.getStringExtra(
                            NovaService.EXTRA_DEBUG_TEXT
                        ) ?: return

                    runOnUiThread {

                        debugText.text =
                            text

                        scrollView.post {
                            scrollView.fullScroll(
                                ScrollView.FOCUS_DOWN
                            )
                        }
                    }
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        createDebugUi()

        registerDebugReceiver()

        checkPermissions()

        debugText.text =
            """
            🟡 NOVA
            Service: VÁRAKOZÁS

            🎤 Mikrofon:
            Engedély ellenőrzése...

            🗣️ TTS:
            Várakozás...

            👂 Hallotta:
            -

            🧠 Parancs:
            -

            ⚙️ Művelet:
            -

            ❌ Hiba:
            -
            """.trimIndent()
    }

    private fun createDebugUi() {

        val root =
            LinearLayout(
                this
            ).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    24,
                    24,
                    24
                )
            }

        val title =
            TextView(
                this
            ).apply {

                text =
                    "NOVA LIVE DEBUG"

                textSize =
                    24f

                setPadding(
                    0,
                    0,
                    0,
                    20
                )
            }

        debugText =
            TextView(
                this
            ).apply {

                textSize =
                    17f

                setTextIsSelectable(
                    true
                )

                typeface =
                    android.graphics.Typeface.MONOSPACE
            }

        scrollView =
            ScrollView(
                this
            ).apply {

                addView(
                    debugText
                )
            }

        root.addView(
            title
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(
            root
        )
    }

    private fun registerDebugReceiver() {

        val filter =
            IntentFilter(
                NovaService.ACTION_DEBUG
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            registerReceiver(
                debugReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )

        } else {

            @Suppress(
                "DEPRECATION"
            )
            registerReceiver(
                debugReceiver,
                filter
            )
        }
    }

    private fun checkPermissions() {

        val permissions =
            mutableListOf<String>()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) !=
                PackageManager.PERMISSION_GRANTED
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
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

        if (
            permissions.isNotEmpty()
        ) {

            requestPermissions(
                permissions.toTypedArray(),
                REQUEST_PERMISSIONS
            )

        } else {

            startNovaService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            REQUEST_PERMISSIONS
        ) {

            val microphoneGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) ==
                PackageManager.PERMISSION_GRANTED

            if (
                microphoneGranted
            ) {

                startNovaService()

            } else {

                debugText.text =
                    """
                    🔴 NOVA

                    🎤 Mikrofon:
                    NINCS ENGEDÉLY

                    ❌ Hiba:
                    A Nova nem tud hallgatni mikrofonengedély nélkül.
                    """.trimIndent()
            }
        }
    }

    private fun startNovaService() {

        val intent =
            Intent(
                this,
                NovaService::class.java
            )

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                ContextCompat.startForegroundService(
                    this,
                    intent
                )

            } else {

                startService(
                    intent
                )
            }

        } catch (
            e: Exception
        ) {

            debugText.text =
                """
                🔴 NOVA SERVICE HIBA

                ${e.javaClass.simpleName}

                ${e.message}
                """.trimIndent()
        }
    }

    override fun onDestroy() {

        try {
            unregisterReceiver(
                debugReceiver
            )
        } catch (
            _: Exception
        ) {
        }

        super.onDestroy()
    }

    companion object {

        private const val REQUEST_PERMISSIONS =
            500
    }
}
