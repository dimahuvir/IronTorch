package com.tbbg.irontorch

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyService : AccessibilityService() {
    override fun onServiceConnected() { Torch.init(this) }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    // Громкость меняет яркость только когда фонарь включён
    override fun onKeyEvent(e: KeyEvent): Boolean {
        if (!Torch.on) return false
        val dir = when (e.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> 1
            KeyEvent.KEYCODE_VOLUME_DOWN -> -1
            else -> return false
        }
        if (e.action == KeyEvent.ACTION_DOWN) Torch.step(dir)
        return true
    }
}
