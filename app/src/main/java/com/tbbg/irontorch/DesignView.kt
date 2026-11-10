package com.tbbg.irontorch

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class DesignView(c: Context, private val bmp: Bitmap) : View(c) {
    var onInfo: () -> Unit = {}
    var onGear: () -> Unit = {}
    var onBright: () -> Unit = {}
    var onRed: (Boolean) -> Unit = {}
    var redMode = false

    private val YEL = Color.parseColor("#E6F000")
    private val CX = 426f
    private val CY = 796f
    private val zones = listOf(
        RectF(78f, 1188f, 410f, 1330f),
        RectF(443f, 1188f, 775f, 1330f),
        RectF(78f, 1355f, 410f, 1496f),
        RectF(443f, 1355f, 775f, 1496f)
    )
    private var pressed = -1
    private var sc = 1f
    private var dx = 0f
    private var dy = 0f
    private val bp = Paint(Paint.FILTER_BITMAP_FLAG)
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
        if (near(ix, iy, 62f, 111f, 55f)) return 11
        if (near(ix, iy, 788f, 111f, 55f)) return 12
        return -1
    }

    private fun act(id: Int) {
        when (id) {
            99 -> { redMode = false; onRed(false) }
            10 -> Torch.toggle()
            0 -> onBright()
            1 -> Torch.alarm()
            2 -> Torch.sos()
            3 -> { redMode = true; onRed(true) }
            11 -> onInfo()
            12 -> onGear()
        }
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                pressed = if (redMode) 99 else hit(e.x, e.y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val h = if (redMode) 99 else hit(e.x, e.y)
                if (h == pressed && h != -1) act(h)
                pressed = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> { pressed = -1; invalidate() }
        }
        return true
    }

    override fun onDraw(cv: Canvas) {
        if (redMode) {
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

        val lit = Torch.active
        if (lit) {
            val beam = Path().apply {
                moveTo(CX - 150f, 544f); lineTo(CX + 150f, 544f)
                lineTo(CX + 340f, 205f); lineTo(CX - 340f, 205f); close()
            }
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(0f, 544f, 0f, 205f,
                Color.argb(235, 255, 255, 235), Color.argb(0, 255, 255, 235), Shader.TileMode.CLAMP)
            cv.drawPath(beam, p)
            p.shader = RadialGradient(CX, CY, 300f,
                Color.argb(110, 230, 240, 0), Color.argb(0, 230, 240, 0), Shader.TileMode.CLAMP)
            cv.drawCircle(CX, CY, 300f, p)
            p.shader = null
        }

        val active = booleanArrayOf(
            Torch.max > 1 && Torch.on && Torch.level == Torch.max,
            Torch.alarmRunning, Torch.sosRunning, false
        )
        for (i in zones.indices) {
            if (active[i]) {
                p.style = Paint.Style.FILL
                p.color = Color.argb(45, 230, 240, 0)
                cv.drawRoundRect(zones[i], 26f, 26f, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 7f
                p.color = YEL
                cv.drawRoundRect(zones[i], 26f, 26f, p)
            }
            if (pressed == i) {
                p.style = Paint.Style.FILL
                p.color = Color.argb(60, 255, 255, 255)
                cv.drawRoundRect(zones[i], 26f, 26f, p)
            }
        }
        if (pressed == 10) {
            p.style = Paint.Style.FILL
            p.color = Color.argb(50, 255, 255, 200)
            cv.drawCircle(CX, CY, 190f, p)
        }

        val text = when {
            Torch.alarmRunning -> "STROBE"
            Torch.sosRunning -> "SOS"
            Torch.on -> "ON"
            else -> "OFF"
        }
        label.textSize = if (text.length > 3) 46f else 64f
        label.color = if (lit) YEL else Color.parseColor("#7C7C45")
        cv.drawText(text, 425f, 1095f + label.textSize * 0.35f, label)
        cv.restore()
    }
}
