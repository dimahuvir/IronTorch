package com.tbbg.irontorch

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var design: DesignView

    private val INFO_HTML = """
<font color="#E6F000"><b>ГЛАВНАЯ КНОПКА</b></font><br>
Большая круглая кнопка включает и выключает вспышку камеры. Пока фонарь горит, виден луч и надпись ON.<br><br>
<font color="#E6F000"><b>ТРОЙНОЕ НАЖАТИЕ ПИТАНИЯ</b></font><br>
Три быстрых нажатия кнопки питания включают и выключают фонарь, даже если приложение свёрнуто. Телефон коротко вибрирует. Если вместо этого открывается камера, отключи в настройках телефона запуск камеры двойным нажатием питания.<br><br>
<font color="#E6F000"><b>КНОПКИ ГРОМКОСТИ + и −</b></font><br>
Пока фонарь горит, меняют яркость, но только на телефонах, где вспышка умеет регулироваться (Android 13 и новее). Один раз включи службу: шестерёнка вверху справа → Специальные возможности → IRON TORCH.<br><br>
<font color="#E6F000"><b>BRIGHT</b></font><br>
Экран заливается ярким белым светом на максимальной яркости: светит сам дисплей, вспышка не нужна. Касание в любом месте возвращает назад.<br><br>
<font color="#E6F000"><b>STROBE</b></font><br>
Быстрый стробоскоп вспышки со звуком полицейской сирены. Нажми ещё раз, чтобы остановить.<br><br>
<font color="#E6F000"><b>SOS</b></font><br>
Вспышка подаёт сигнал бедствия азбукой Морзе: три коротких, три длинных, три коротких. Нажми ещё раз, чтобы остановить.<br><br>
<font color="#E6F000"><b>RED LIGHT</b></font><br>
Экран заливается ярко-красным светом на максимальной яркости. Касание возвращает назад.<br><br>
<font color="#E6F000"><b>ШЕСТЕРЁНКА</b></font><br>
Открывает настройки специальных возможностей, где включаются кнопки громкости.<br><br>
<font color="#E6F000"><b>РАБОТА В ФОНЕ</b></font><br>
Чтобы тройное нажатие работало всегда, разреши уведомления и в настройках батареи выбери для приложения «Без ограничений». В шторке должно висеть уведомление «IRON TORCH НА ПОСТУ». Когда экран полностью погашен, срабатывание зависит от телефона.<br><br>
<font color="#E6F000"><b>ВНИМАНИЕ</b></font><br>
Не свети в глаза людям и животным. Стробоскоп, SOS и яркие режимы быстро разряжают батарею.<br><br>
<font color="#B8C400"><b>By TBBG</b></font>
"""

    private fun showInfo() {
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val title = TextView(this).apply {
            text = "IRON TORCH — ИНСТРУКЦИЯ"
            textSize = 17f
            setTextColor(Color.parseColor("#E6F000"))
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }
        val body = TextView(this).apply {
            text = Html.fromHtml(INFO_HTML, Html.FROM_HTML_MODE_LEGACY)
            textSize = 14f
            setTextColor(Color.parseColor("#D8D8C0"))
            setLineSpacing(5f, 1f)
        }
        val scroll = ScrollView(this).apply {
            addView(body)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        val ok = TextView(this).apply {
            text = "ПОНЯТНО"
            textSize = 16f
            setTextColor(Color.parseColor("#12130E"))
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 22)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B8C400"))
                cornerRadius = 18f
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 }
            setOnClickListener { dlg.dismiss() }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 34, 36, 28)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#2B2E22"), Color.parseColor("#12130E"))).apply {
                cornerRadius = 28f
                setStroke(4, Color.parseColor("#B8C400"))
            }
            addView(title)
            addView(scroll)
            addView(ok)
        }
        dlg.setContentView(box)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.show()
        val dm = resources.displayMetrics
        dlg.window?.setLayout((dm.widthPixels * 0.92f).toInt(), (dm.heightPixels * 0.80f).toInt())
    }

    // белый и красный экран: максимальная яркость и экран не гаснет
    private fun setScreen(on: Boolean) {
        val a = window.attributes
        a.screenBrightness = if (on) 1f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = a
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // картинка на весь экран, под прозрачными системными панелями
    private fun fullScreen() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
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
        fullScreen()

        design = DesignView(this, BitmapFactory.decodeResource(resources, R.drawable.design)).apply {
            onInfo = { showInfo() }
            onGear = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            onScreen = { on -> setScreen(on) }
        }
        setContentView(design)
        Torch.onChange = { runOnUiThread { design.invalidate() } }
    }

    override fun onResume() {
        super.onResume()
        fullScreen()
        design.invalidate()
    }
}
