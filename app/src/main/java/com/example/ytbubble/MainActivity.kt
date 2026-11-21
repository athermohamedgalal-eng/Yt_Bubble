package com.example.ytbubble

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }

        val btnOverlay = Button(this).apply {
            text = "1. Grant Floating Overlay Permission"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnUsage = Button(this).apply {
            text = "2. Grant Usage Access (Detect YouTube)"
            setOnClickListener {
                if (!hasUsageStatsPermission()) {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } else {
                    Toast.makeText(this@MainActivity, "Usage Access already granted", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnStart = Button(this).apply {
            text = "3. Start YT Bubble Service"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity) && hasUsageStatsPermission()) {
                    startService(Intent(this@MainActivity, BubbleService::class.java))
                    Toast.makeText(this@MainActivity, "Service running! Open YouTube.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Please grant both permissions first.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val downloadsHeader = TextView(this).apply {
            text = "\nAuthorized Downloads Manager"
            textSize = 18f
            setPadding(0, 24, 0, 12)
        }

        val sampleDownloads = listOf(
            "No active authorized downloads yet.",
            "Tip: Share an authorized media link to queue it here."
        )
        val downloadsList = ListView(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, sampleDownloads)
        }

        root.addView(statusText)
        root.addView(btnOverlay)
        root.addView(btnUsage)
        root.addView(btnStart)
        root.addView(downloadsHeader)
        root.addView(downloadsList)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val overlayOk = if (Settings.canDrawOverlays(this)) "Granted" else "Missing"
        val usageOk = if (hasUsageStatsPermission()) "Granted" else "Missing"
        statusText.text = "Device: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                "Overlay Permission: $overlayOk\n" +
                "Usage Access: $usageOk"
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
