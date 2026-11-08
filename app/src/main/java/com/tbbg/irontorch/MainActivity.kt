package com.tbbg.irontorch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var radar: RadarView
    private lateinit var info: TextView
    private lateinit var btnSos: Button
    private lateinit var btnAlarm: Button
    private lateinit var btnMute: Button
    private var bar: SeekBar? = null
    private val OLIVE = Color.parseColor("#8BC34A")
    private val RED = Color.parseColor("#D32F2F")

    private fun rect(vararg c: String, stroke: String = "#555955") =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, c.map { Color.parseColor(it) }.toIntArray())
            .apply { cornerRadius = 16f; setStroke(3, Color.parseColor(stroke)) }

    private fun styleBtn(b: Button, active: Boolean) {
        val normal = if (active) rect("#E53935", "#8E0000", "#B71C1C", stroke = "#FF8A80")
                     else rect("#4A4D49", "#2A2C29", "#3C3F3B")
        val pressed = rect("#9CCC65", "#689F38", "#558B2F", stroke = "#DCEDC8")
        b.background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
    }

    private fun tv(t: String, sz: Float, col: Int) = TextView(this).apply {
        text = t; textSize = sz; setTextColor(col); gravity = Gravity.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private fun mbtn(t: String, tap: () -> Unit) = Button(this).apply {
        text = t; textSize = 13f; isAllCaps = false
        setTextColor(Color.WHITE)
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setOnClickListener { tap() }
        styleBtn(this, false)
    }

    private fun row(vararg b: Button) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 }
        b.forEachIndexed { i, x ->
            x.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) leftMargin = 16 }
            addView(x)
        }
    }

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
            orientation = LinearLayout.VERTICAL; setPadding(40, 90, 40, 30)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#2B2E27"), Color.parseColor("#12130F")))
        }
        root.addView(tv("☠ IRON TORCH ☠", 28f, OLIVE))
        root.addView(tv("▄▀ HEAVY METAL TACTICAL ▀▄  🤘", 12f, Color.parseColor("#9E9E9E")))

        radar = RadarView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 20; bottomMargin = 10 }
            setOnClickListener { Torch.toggle() }
        }
        root.addView(radar)

        info = tv("", 12f, OLIVE)
        root.addView(info)
        if (Torch.max > 1) {
            val sb = SeekBar(this).apply {
                max = Torch.max - 1
                progress = Torch.level - 1
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(b: SeekBar?, p: Int, u: Boolean) { if (u) Torch.changeLevel(p + 1); refresh() }
                    override fun onStartTrackingTouch(b: SeekBar?) {}
                    override fun onStopTrackingTouch(b: SeekBar?) {}
                })
            }
            bar = sb
            root.addView(sb)
        }

        btnSos = mbtn("🚨 SOS с мигалкой") { Torch.sos() }
        btnAlarm = mbtn("🚓 Сигнализация") { Torch.alarm() }
        btnMute = mbtn("🔊 ЗВУК: ВКЛ") {
            Sounds.muted = !Sounds.muted
            if (Sounds.muted) Sounds.stopPing()
            refresh()
        }
        val btnField = mbtn("🔦 Полевой фонарь") { openField() }
        root.addView(row(btnSos, btnAlarm))
        root.addView(row(btnField, btnMute))

        root.addView(tv("⚙ Настройка кнопок громкости", 11f, Color.parseColor("#8A8A8A")).apply {
            setPadding(0, 24, 0, 8)
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(tv("— By TBBG —", 16f, RED))

        setContentView(root)
        Torch.onChange = { runOnUiThread { refresh() } }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        styleBtn(btnSos, Torch.sosRunning)
        styleBtn(btnAlarm, Torch.alarmRunning)
        styleBtn(btnMute, Sounds.muted)
        btnSos.text = if (Torch.sosRunning) "⏹ СТОП SOS" else "🚨 SOS с мигалкой"
        btnAlarm.text = if (Torch.alarmRunning) "⏹ СТОП" else "🚓 Сигнализация"
        btnMute.text = if (Sounds.muted) "🔇 ЗВУК: ВЫКЛ" else "🔊 ЗВУК: ВКЛ"
        bar?.progress = Torch.level - 1
        info.text = if (Torch.max > 1) "ЯРКОСТЬ: ${Torch.level}/${Torch.max}"
                    else "ЯРКОСТЬ: ФИКСИРОВАННАЯ (телефон не регулирует)"
        radar.invalidate()
    }
}
