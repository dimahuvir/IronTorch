package com.tbbg.irontorch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var design: DesignView

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
    }

    // картинка на весь экран, под прозрачными системными панелями
    private fun fullScreen() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULL_SCREEN or
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
            onInfo = {
                Toast.makeText(this@MainActivity,
                    "IRON TORCH\n3x кнопка питания = фонарь\nBy TBBG", Toast.LENGTH_LONG).show()
            }
            onGear = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            onBright = { bright() }
            onRed = { on -> setRed(on) }
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
