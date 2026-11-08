package com.tbbg.irontorch

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class RadarView(c: Context) : View(c) {
    private val ICE = Color.parseColor("#7FE9FF")
    private val RED = Color.parseColor("#FF3B3B")
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        color = Color.WHITE
    }
    private var sweepIce: Shader? = null
    private var sweepRed: Shader? = null
    private var sweepAngle = 0f
    private var last = 0L
    private val blips = ArrayList<FloatArray>()

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    private fun makeSweep(cx: Float, cy: Float, r: Int, g: Int, b: Int) = SweepGradient(
        cx, cy,
        intArrayOf(Color.argb(0, r, g, b), Color.argb(0, r, g, b), Color.argb(170, r, g, b)),
        floatArrayOf(0f, 0.72f, 1f)
    )

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val cx = w / 2f
        val cy = h / 2f
        sweepIce = makeSweep(cx, cy, 127, 233, 255)
        sweepRed = makeSweep(cx, cy, 255, 59, 59)
    }

    private fun newBlips() {
        blips.clear()
        repeat(Random.nextInt(2, 5)) {
            blips.add(floatArrayOf(Random.nextFloat() * 360f, 0.25f + Random.nextFloat() * 0.65f))
        }
    }

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 16f
        val now = System.currentTimeMillis()
        val active = Torch.active
        val red = Torch.alarmRunning && (now / 250) % 2 == 1L
        val col = if (!active) Color.parseColor("#5B5F58") else if (red) RED else ICE

        fill.shader = null
        fill.color = if (active) Color.parseColor("#06161C") else Color.parseColor("#1E201D")
        cv.drawCircle(cx, cy, r, fill)

        grid.color = col
        if (active) grid.setShadowLayer(14f, 0f, 0f, col) else grid.clearShadowLayer()
        for (k in 1..4) {
            grid.strokeWidth = if (k == 4) 7f else 3f
            cv.drawCircle(cx, cy, r * k / 4f, grid)
        }
        grid.strokeWidth = 3f
        cv.drawLine(cx - r, cy, cx + r, cy, grid)
        cv.drawLine(cx, cy - r, cx, cy + r, grid)
        val d = r * 0.7071f
        cv.drawLine(cx - d, cy - d, cx + d, cy + d, grid)
        cv.drawLine(cx - d, cy + d, cx + d, cy - d, grid)

        if (active) {
            val first = last == 0L
            val dt = if (first) 0L else now - last
            last = now
            sweepAngle += dt * 0.144f
            var wrap = first
            if (sweepAngle >= 360f) { sweepAngle -= 360f; wrap = true }
            if (wrap) {
                newBlips()
                if (Torch.steady) Sounds.ping()
            }

            cv.save()
            cv.rotate(sweepAngle, cx, cy)
            fill.shader = if (red) sweepRed else sweepIce
            cv.drawCircle(cx, cy, r, fill)
            fill.shader = null
            cv.restore()

            val rad = Math.toRadians(sweepAngle.toDouble())
            grid.strokeWidth = 5f
            cv.drawLine(cx, cy, cx + (r * cos(rad)).toFloat(), cy + (r * sin(rad)).toFloat(), grid)
            grid.strokeWidth = 3f

            for (b in blips) {
                val diff = (sweepAngle - b[0] + 360f) % 360f
                val a = max(0f, 1f - diff / 200f)
                if (a > 0f) {
                    val br = Math.toRadians(b[0].toDouble())
                    val bx = cx + (r * b[1] * cos(br)).toFloat()
                    val by = cy + (r * b[1] * sin(br)).toFloat()
                    fill.color = Color.argb((255 * a).toInt(), 210, 250, 255)
                    cv.drawCircle(bx, by, 9f, fill)
                }
            }

            txt.color = col
            txt.textSize = r * 0.09f
            val label = if (Torch.alarmRunning) "ТРЕВОГА" else if (Torch.sosRunning) "SOS" else "ПОИСК СУДОВ"
            cv.drawText(label, cx, cy + r * 0.55f, txt)
            txt.color = Color.WHITE
            postInvalidateOnAnimation()
        } else {
            last = 0L
            txt.textSize = r * 0.45f
            cv.drawText("☠", cx, cy, txt)
            txt.textSize = r * 0.2f
            cv.drawText("ВКЛЮЧИТЬ", cx, cy + r * 0.3f, txt)
        }
    }
}
