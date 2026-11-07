package com.tbbg.irontorch

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

object Torch {
    var on = false
    var level = 1
    var max = 1
    @Volatile var sosRunning = false
    private var cm: CameraManager? = null
    private var id = ""

    fun init(c: Context) {
        if (cm != null) return
        val m = c.getSystemService(CameraManager::class.java)
        id = m.cameraIdList.firstOrNull {
            m.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return
        if (Build.VERSION.SDK_INT >= 33)
            max = m.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        level = max
        cm = m
    }

    fun apply() {
        val m = cm ?: return
        try {
            when {
                !on -> m.setTorchMode(id, false)
                Build.VERSION.SDK_INT >= 33 && max > 1 -> m.turnOnTorchWithStrengthLevel(id, level)
                else -> m.setTorchMode(id, true)
            }
        } catch (e: Exception) {}
    }

    fun toggle() { sosRunning = false; on = !on; apply(); onChange?.invoke() }

    fun step(dir: Int) {
        level = (level + dir * maxOf(1, max / 6)).coerceIn(1, max)
        if (on) apply()
        onChange?.invoke()
    }

    fun changeLevel: Int) { level = l.coerceIn(1, max); if (on) apply() }

    var onChange: (() -> Unit)? = null

    // SOS по морзе: ... --- ...
    fun sos() {
        if (sosRunning) { sosRunning = false; return }
        sosRunning = true
        Thread {
            val code = "...---..."
            val was = on
            while (sosRunning) {
                for ((i, ch) in code.withIndex()) {
                    if (!sosRunning) break
                    on = true; apply(); Thread.sleep(if (ch == '.') 200 else 600)
                    on = false; apply(); Thread.sleep(200)
                    if (i % 3 == 2) Thread.sleep(400)
                }
                Thread.sleep(1200)
            }
            on = was; apply()
        }.start()
    }
}
