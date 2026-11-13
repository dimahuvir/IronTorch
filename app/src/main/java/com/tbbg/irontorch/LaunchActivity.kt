package com.tbbg.irontorch

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class LaunchActivity : Activity() {

    private fun goNext() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showReport(report: String, prefs: SharedPreferences) {
        window.statusBarColor = Color.parseColor("#0F100B")
        window.navigationBarColor = Color.parseColor("#0F100B")

        val title = TextView(this).apply {
            text = "Прошлый запуск завершился ошибкой"
            textSize = 18f
            setTextColor(Color.parseColor("#E6F000"))
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 12)
        }
        val hint = TextView(this).apply {
            text = "Сделай скриншот этого экрана и пришли в чат"
            textSize = 13f
            setTextColor(Color.parseColor("#B8C400"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        val body = TextView(this).apply {
            text = report.lines().take(30).joinToString("\n")
            textSize = 11f
            setTextColor(Color.parseColor("#D8D8C0"))
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(body)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        val copy = Button(this).apply {
            text = "Скопировать"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", report))
            }
        }
        val go = Button(this).apply {
            text = "Продолжить"
            setOnClickListener {
                prefs.edit().remove("last").apply()
                goNext()
            }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F100B"))
            setPadding(30, 70, 30, 40)
            addView(title)
            addView(hint)
            addView(scroll)
            addView(copy)
            addView(go)
        }
        setContentView(root)
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val prefs = getSharedPreferences("crash", Context.MODE_PRIVATE)
        val report = prefs.getString("last", null)
        if (report == null) {
            goNext()
        } else {
            showReport(report, prefs)
        }
    }
}
