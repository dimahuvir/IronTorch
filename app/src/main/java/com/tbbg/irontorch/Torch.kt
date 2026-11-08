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
    @Volatile var alarmRunning = false
    private var saved = false
    private var gen = 0
    private var cm: CameraManager? = null
    private var id = ""

    val active: Boolean get() = on || sosRunning || alarmRunning
    val steady: Boolean get() = on && !sosRunning && !alarmRunning

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

    fun stopEffects() {
        sosRunning = false
        alarmRunning = false
        Sounds.siren(false)
    }

    fun toggle() {
        if (sosRunning || alarmRunning) { stopEffects(); return }
        on = !on
        apply()
        if (!on) Sounds.stopPing()
        onChange?.invoke()
    }

    fun step(dir: Int) {
        level = (level + dir * maxOf(1, max / 6)).coerceIn(1, max)
        if (on) apply()
        onChange?.invoke()
    }

    fun changeLevel(l: Int) { level = l.coerceIn(1, max); if (on) apply() }

    var onChange: (() -> Unit)? = null

    // SOS по морзе: ... --- ...
    fun sos() {
        if (sosRunning) { stopEffects(); return }
        if (!alarmRunning) saved = on
        stopEffects()
        sosRunning = true
        val g = ++gen
        onChange?.invoke()
        Thread {
            val code = "...---..."
            while (sosRunning && g == gen) {
                for ((i, ch) in code.withIndex()) {
                    if (!sosRunning || g != gen) break
                    on = true; apply(); Thread.sleep(if (ch == '.') 200 else 600)
                    on = false; apply(); Thread.sleep(200)
                    if (i % 3 == 2) Thread.sleep(400)
                }
                Thread.sleep(1200)
            }
            if (g == gen && !sosRunning && !alarmRunning) { on = saved; apply() }
            onChange?.invoke()
        }.start()
    }

    // Сигнализация: сирена + быстрый стробоскоп
    fun alarm() {
        if (alarmRunning) { stopEffects(); return }
        if (!sosRunning) saved = on
        stopEffects()
        alarmRunning = true
        val g = ++gen
        Sounds.siren(true)
        onChange?.invoke()
        Thread {
            while (alarmRunning && g == gen) {
                on = true; apply(); Thread.sleep(110)
                on = false; apply(); Thread.sleep(110)
            }
            if (g == gen && !sosRunning && !alarmRunning) { on = saved; apply() }
            onChange?.invoke()
        }.start()
    }
}
