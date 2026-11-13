package com.tbbg.irontorch

import android.content.Context
import android.graphics.*
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class DesignView(c: Context, private val bmp: Bitmap) : View(c) {
    var onInfo: () -> Unit = {}
    var onGear: () -> Unit = {}
    var onScreen: (Boolean) -> Unit = {}
    var screenMode = 0   // 0 - обычный вид, 1 - красный экран, 2 - белый экран

    private val YEL = Color.parseColor("#E6F000")
    private val CX = 426f
    private val CY = 888f
    // 0 BRIGHT, 1 RED LIGHT (верхний ряд), 2 SOS, 3 STROBE (нижний ряд)
    private val zones = listOf(
        RectF(78f, 1280f, 410f, 1422f),
        RectF(443f, 1280f, 775f, 1422f),
        RectF(78f, 1447f, 410f, 1588f),
        RectF(443f, 1447f, 775f, 1588f)
    )
    private val octPts = floatArrayOf(
        354f, 634f, 496f, 634f, 509f, 643f, 648f, 668f, 726f, 724f, 731f, 744f,
        731f, 1036f, 688f, 1102f, 638f, 1137f, 214f, 1137f, 166f, 1102f, 122f, 1036f,
        122f, 744f, 127f, 724f, 204f, 668f, 343f, 643f
    )
    private val octPath = Path().apply {
        moveTo(octPts[0], octPts[1])
        for (i in 2 until octPts.size step 2) lineTo(octPts[i], octPts[i + 1])
        close()
    }
    private val zonePaths = zones.map { z ->
        Path().apply {
            addRoundRect(RectF(z.left - 3f, z.top - 3f, z.right + 3f, z.bottom + 3f), 28f, 28f, Path.Direction.CW)
        }
    }
    private val infoPath = Path().apply { addCircle(62f, 203f, 50f, Path.Direction.CW) }
    private val gearPath = Path().apply { addCircle(788f, 203f, 50f, Path.Direction.CW) }
    private val bounds = RectF()

    private var pressed = -1
    private var sc = 1f
    private var dx = 0f
    private var dy = 0f
    private val bp = Paint(Paint.FILTER_BITMAP_FLAG)
    private val pressP = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = LightingColorFilter(Color.rgb(170, 170, 170), 0)
    }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private fun near(x: Float, y: Float, cx: Float, cy: Float, r: Float) =
        (x - cx) * (x - cx) + (y - cy) * (y - cy) < r * r

    private fun hit(x: Float, y: Float): Int {
        val ix = (x - dx) / sc
        val iy = (y - dy) / sc
        for (i in zones.indices) if (zones[i].contains(ix, iy)) return i
        if (near(ix, iy, CX, CY, 250f)) return 10
        if (near(ix, iy, 62f, 203f, 55f)) return 11
        if (near(ix, iy, 788f, 203f, 55f)) return 12
        return -1
    }

    private fun act(id: Int) {
        when (id) {
            99 -> { screenMode = 0; onScreen(false) }
            10 -> Torch.toggle()
            0 -> { screenMode = 2; onScreen(true) }
            1 -> { screenMode = 1; onScreen(true) }
            2 -> Torch.sos()
            3 -> Torch.alarm()
            11 -> onInfo()
            12 -> onGear()
        }
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                pressed = if (screenMode != 0) 99 else hit(e.x, e.y)
                if (pressed != -1 && screenMode == 0) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    Sfx.click()
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val h = if (screenMode != 0) 99 else hit(e.x, e.y)
                if (h == pressed && h != -1) act(h)
                pressed = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> { pressed = -1; invalidate() }
        }
        return true
    }

    // кнопка «утоплена»: тёмное гнездо, плита чуть меньше и ниже, тень сверху
    private fun sink(cv: Canvas, clip: Path, tint: Int) {
        clip.computeBounds(bounds, true)
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        // гнездо
        cv.save()
        cv.translate(cx, cy)
        cv.scale(1.04f, 1.04f)
        cv.translate(-cx, -cy)
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = Color.argb(240, 4, 5, 3)
        cv.drawPath(clip, p)
        cv.restore()
        // сама плита
        cv.save()
        cv.translate(cx, cy + 4f)
        cv.scale(0.955f, 0.955f)
        cv.translate(-cx, -cy)
        cv.clipPath(clip)
        cv.drawBitmap(bmp, 0f, 0f, pressP)
        p.shader = LinearGradient(0f, bounds.top, 0f, bounds.top + bounds.height() * 0.28f,
            Color.argb(130, 0, 0, 0), Color.argb(0, 0, 0, 0), Shader.TileMode.CLAMP)
        cv.drawRect(bounds, p)
        p.shader = null
        if (tint != 0) {
            p.color = tint
            cv.drawRect(bounds, p)
        }
        cv.restore()
    }

    override fun onDraw(cv: Canvas) {
        if (screenMode == 2) {
            cv.drawColor(Color.WHITE)
            label.color = Color.rgb(185, 185, 185)
            label.textSize = 26f
            cv.drawText("Коснись экрана, чтобы выйти", width / 2f, height - 90f, label)
            return
        }
        if (screenMode == 1) {
            cv.drawColor(Color.rgb(255, 0, 0))
            label.color = Color.rgb(110, 0, 0)
            label.textSize = 56f
            cv.drawText("RED LIGHT", width / 2f, height / 2f, label)
            label.textSize = 28f
            cv.drawText("Коснись экрана, чтобы выйти", width / 2f, height / 2f + 60f, label)
            return
        }

        sc = maxOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
        dx = (width - bmp.width * sc) / 2f
        dy = (height - bmp.height * sc) / 2f
        cv.save()
        cv.translate(dx, dy)
        cv.scale(sc, sc)
        cv.drawBitmap(bmp, 0f, 0f, bp)

        // нажатые и включённые кнопки выглядят утопленными
        val active = booleanArrayOf(false, false, Torch.sosRunning, Torch.alarmRunning)
        for (i in zones.indices) {
            val on = active[i]
            if (pressed == i || on) {
                sink(cv, zonePaths[i], if (on) Color.argb(46, 230, 240, 0) else 0)
            }
        }
        if (pressed == 10) sink(cv, octPath, 0)
        if (pressed == 11) sink(cv, infoPath, 0)
        if (pressed == 12) sink(cv, gearPath, 0)

        val lit = Torch.active
        if (lit) {
            val beam = Path().apply {
                moveTo(CX - 150f, 636f); lineTo(CX + 150f, 636f)
                lineTo(CX + 340f, 297f); lineTo(CX - 340f, 297f); close()
            }
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(0f, 636f, 0f, 297f,
                Color.argb(235, 255, 255, 235), Color.argb(0, 255, 255, 235), Shader.TileMode.CLAMP)
            cv.drawPath(beam, p)
            p.shader = RadialGradient(CX, CY, 300f,
                Color.argb(110, 230, 240, 0), Color.argb(0, 230, 240, 0), Shader.TileMode.CLAMP)
            cv.drawCircle(CX, CY, 300f, p)
            p.shader = null
        }

        val text = when {
            Torch.alarmRunning -> "STROBE"
            Torch.sosRunning -> "SOS"
            Torch.on -> "ON"
            else -> "OFF"
        }
        label.textSize = if (text.length > 3) 46f else 64f
        label.color = if (lit) YEL else Color.parseColor("#7C7C45")
        cv.drawText(text, 425f, 1187f + label.textSize * 0.35f, label)

        cv.restore()
    }
}
