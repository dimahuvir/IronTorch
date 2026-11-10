package com.tbbg.irontorch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var design: DesignView
    private lateinit var root: LinearLayout
    private lateinit var sig: TextView

    private fun openField() {
        val pm = packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val app = pm.queryIntentActivities(i, 0).firstOrNull {
            val n = it.loadLabel(pm).toString().lowercase()
            n.contains("походн") || n.contains("camp")
        }
        val launch = app?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (launch == null) {
            Toast.makeText(this, "Системный полевой фонарь не найден", Toast.LENGTH_LONG).show()
        } else {
            startActivity(launch)
        }
    }

    private fun bright() {
        if (Torch.max > 1) {
            Torch.changeLevel(Torch.max)
            if (!Torch.on) Torch.toggle()
            design.invalidate()
        } else {
            openField()
        }
    }

    // RED LIGHT: ярко-красный экран на максимальной яркости
    private fun setRed(on: Boolean) {
        val a = window.attributes
        a.screenBrightness = if (on) 1f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = a
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val c = if (on) Color.rgb(255, 0, 0) else Color.parseColor("#0F100B")
        window.statusBarColor = c
        window.navigationBarColor = c
        root.setBackgroundColor(c)
        sig.visibility = if (on) View.GONE else View.VISIBLE
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Torch.init(this)
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        startForegroundService(Intent(this, TorchService::class.java))
        val pw = getSystemService(android.os.PowerManager::class.java)
        if (!pw.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:$packageName")))
        }
        window.statusBarColor = Color.parseColor("#0F100B")
        window.navigationBarColor = Color.parseColor("#0F100B")

        design = DesignView(this, BitmapFactory.decodeResource(resources, R.drawable.design)).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            onInfo = {
                Toast.makeText(this@MainActivity,
                    "IRON TORCH\n3x кнопка питания = фонарь\nBy TBBG", Toast.LENGTH_LONG).show()
            }
            onGear = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            onBright = { bright() }
            onRed = { on -> setRed(on) }
        }

        sig = TextView(this).apply {
            text = "— By TBBG —"
            textSize = 15f
            setTextColor(Color.parseColor("#B8C400"))
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(0, 12, 0, 16)
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F100B"))
            addView(design)
            addView(sig)
        }
        setContentView(root)
        Torch.onChange = { runOnUiThread { design.invalidate() } }
    }

    override fun onResume() {
        super.onResume()
        design.invalidate()
    }
}
