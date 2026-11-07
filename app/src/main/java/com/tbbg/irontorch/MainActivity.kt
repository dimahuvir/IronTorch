package com.tbbg.irontorch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var btn: Button
    private lateinit var bar: SeekBar
    private lateinit var info: TextView
    private val OLIVE = Color.parseColor("#8BC34A")
    private val RED = Color.parseColor("#D32F2F")

    private fun metal(vararg c: String, r: Float = 16f) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR, c.map { Color.parseColor(it) }.toIntArray()
    ).apply { cornerRadius = r; setStroke(3, Color.parseColor("#555955")) }

    private fun tv(t: String, sz: Float, col: Int) = TextView(this).apply {
        text = t; textSize = sz; setTextColor(col); gravity = Gravity.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private fun mbtn(t: String) = Button(this).apply {
        text = t; setTextColor(Color.parseColor("#E0E0E0")); typeface = Typeface.MONOSPACE
        background = metal("#4A4D49", "#2A2C29", "#3C3F3B")
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 }
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
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(40, 90, 40, 40)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#2B2E27"), Color.parseColor("#12130F")))
        }
        root.addView(tv("☠ IRON TORCH ☠", 30f, OLIVE))
        root.addView(tv("▄▀ HEAVY METAL TACTICAL ▀▄  🤘", 12f, Color.parseColor("#9E9E9E")))

        btn = Button(this).apply {
            textSize = 26f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(-1, 520).apply { topMargin = 50; bottomMargin = 30 }
            setOnClickListener { Torch.toggle() }
        }
        root.addView(btn)

        info = tv("", 14f, OLIVE)
        root.addView(info)
        bar = SeekBar(this).apply {
            max = (Torch.max - 1).coerceAtLeast(0)
            progress = Torch.level - 1
            isEnabled = Torch.max > 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, p: Int, u: Boolean) { if (u) Torch.changeLevel(p + 1); refresh() }
                override fun onStartTrackingTouch(b: SeekBar?) {}
                override fun onStopTrackingTouch(b: SeekBar?) {}
            })
        }
        root.addView(bar)

        val sos = mbtn("🚨 SOS  · · · — — — · · ·").apply { setOnClickListener { Torch.sos() } }
        val acc = mbtn("⚙ ВКЛЮЧИТЬ КНОПКИ ГРОМКОСТИ (Спец. возможности)").apply {
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        root.addView(sos); root.addView(acc)
        root.addView(tv("Тройное нажатие питания → фонарь\nГромкость +/- → яркость (когда горит)", 11f, Color.parseColor("#8A8A8A")).apply { setPadding(0, 30, 0, 0) })
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(tv("— By TBBG —", 16f, RED))

        setContentView(root)
        Torch.onChange = { runOnUiThread { refresh() } }
        refresh()
    }

    private fun refresh() {
        val on = Torch.on
        btn.text = if (on) "⚡ ОГОНЬ\nВЫКЛЮЧИТЬ" else "☠ ВКЛЮЧИТЬ"
        btn.setTextColor(Color.WHITE)
        btn.background = if (on) metal("#E53935", "#8E0000", "#B71C1C", r = 260f)
                         else metal("#5B5F58", "#252823", "#3A3D38", r = 260f)
        bar.progress = Torch.level - 1
        info.text = if (Torch.max > 1) "ЯРКОСТЬ: ${Torch.level}/${Torch.max}" else "РЕГУЛИРОВКА ЯРКОСТИ НЕ ПОДДЕРЖАНА ЭТИМ ТЕЛЕФОНОМ"
    }
}
