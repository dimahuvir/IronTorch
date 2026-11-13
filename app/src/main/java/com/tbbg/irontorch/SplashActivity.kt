package com.tbbg.irontorch

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.AudioTrack
import android.os.Bundle
import android.view.View

class SplashActivity : Activity() {
    private var opened = false

    fun openMain() {
        if (opened) return
        opened = true
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        val v = SplashView(this)
        v.onDone = { openMain() }
        v.setOnClickListener { openMain() }
        setContentView(v)
    }
}

class SplashView(c: Context) : View(c) {
    var onDone: () -> Unit = {}

    private val W = 852f
    private val H = 1846f
    private val bg = BitmapFactory.decodeResource(resources, R.drawable.splash_bg)
    private val man = BitmapFactory.decodeResource(resources, R.drawable.soldier)
    private val word = BitmapFactory.decodeResource(resources, R.drawable.tbbg)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bp = Paint(Paint.FILTER_BITMAP_FLAG)
    private val body = Paint(Paint.FILTER_BITMAP_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = 108f
        letterSpacing = 0.05f
    }
    private val beamPath = Path()
    private var start = 0L
    private var finished = false
    private var audio: AudioTrack? = null

    private val TITLE = "IRON TORCH"
    private val MS = 0.62f
    private val MX = 30f
    private val MY = 1760f - man.height * MS
    private val TX = MX + 677.3f * MS
    private val TY = MY + 264.5f * MS
    private val WX = 660f
    private val WY = 1060f
    private val WW = 330f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            audio = Sfx.player(Sfx.buildSplash())
            audio?.play()
        } catch (e: Exception) {
        }
        // картинка стартует чуть позже звука: у звука есть небольшая задержка
        start = System.currentTimeMillis() + 70L
        invalidate()
    }

    override fun onDetachedFromWindow() {
        try {
            audio?.stop()
            audio?.release()
        } catch (e: Exception) {
        }
        audio = null
        super.onDetachedFromWindow()
    }

    private fun smooth(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    // мигание фонаря: два коротких включения, потом ровный свет
    private fun beam(t: Float): Float = when {
        t < 1500f -> 0f
        t < 1550f -> 0.9f
        t < 1600f -> 0.1f
        t < 1650f -> 1f
        t < 1710f -> 0.3f
        t < 2040f -> 0.3f + 0.7f * (t - 1710f) / 330f
        else -> 1f
    }

    override fun onDraw(cv: Canvas) {
        val t = (System.currentTimeMillis() - start).toFloat().coerceAtLeast(0f)
        val sc = maxOf(width / W, height / H)
        cv.save()
        cv.translate((width - W * sc) / 2f, (height - H * sc) / 2f)
        cv.scale(sc, sc)

        cv.drawBitmap(bg, 0f, 0f, bp)

        val b = beam(t)
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = Color.argb((255 * (0.55f - 0.30f * b)).toInt(), 0, 0, 0)
        cv.drawRect(0f, 0f, W, H, p)

        // название печатается как на машинке
        val n = ((t - 300f) / 100f).toInt().coerceIn(0, TITLE.length)
        val full = tp.measureText(TITLE)
        val x0 = (W - full) / 2f
        val y0 = 640f
        val shown = TITLE.substring(0, n)
        if (n > 0) {
            tp.color = Color.argb(200, 0, 0, 0)
            cv.drawText(shown, x0 + 5f, y0 + 5f, tp)
            tp.color = Color.rgb(236, 208, 98)
            cv.drawText(shown, x0, y0, tp)
        }
        val typing = n < TITLE.length
        if (t > 100f && t < 3300f && (typing || ((t / 350f).toInt() % 2 == 0))) {
            tp.color = Color.rgb(236, 208, 98)
            cv.drawText("_", x0 + tp.measureText(shown), y0, tp)
        }

        // солдат
        val a = (t / 600f).coerceIn(0f, 1f)
        val rise = (1f - a) * 24f
        val dst = RectF(MX, MY + rise, MX + man.width * MS, MY + rise + man.height * MS)
        val m = (255 * (0.50f + 0.50f * b)).toInt()
        body.colorFilter = LightingColorFilter(Color.rgb(m, m, m), 0)
        body.alpha = (255 * a).toInt()
        cv.drawBitmap(man, null, dst, body)

        // луч фонаря
        if (b > 0f) {
            val len = 1100f
            val r0 = Math.toRadians(-18.0)
            val r1 = Math.toRadians(-42.0)
            val r2 = Math.toRadians(6.0)
            beamPath.reset()
            beamPath.moveTo(TX, TY)
            beamPath.lineTo(TX + len * Math.cos(r1).toFloat(), TY + len * Math.sin(r1).toFloat())
            beamPath.lineTo(TX + len * Math.cos(r2).toFloat(), TY + len * Math.sin(r2).toFloat())
            beamPath.close()
            p.shader = LinearGradient(TX, TY,
                TX + len * Math.cos(r0).toFloat(), TY + len * Math.sin(r0).toFloat(),
                Color.argb(200, 255, 255, 225), Color.argb(0, 255, 255, 225), Shader.TileMode.CLAMP)
            p.alpha = (255 * b).toInt()
            cv.drawPath(beamPath, p)
            p.shader = RadialGradient(TX, TY, 70f,
                Color.argb(255, 255, 255, 240), Color.argb(0, 255, 255, 240), Shader.TileMode.CLAMP)
            p.alpha = (255 * b).toInt()
            cv.drawCircle(TX, TY, 70f, p)
            p.shader = null
            p.alpha = 255
        }

        // надпись TBBG проявляется в свете луча
        val ta = b * smooth((t - 1640f) / 900f)
        if (ta > 0f) {
            p.shader = RadialGradient(WX, WY, 280f,
                Color.argb(130, 230, 240, 0), Color.argb(0, 230, 240, 0), Shader.TileMode.CLAMP)
            p.alpha = (255 * ta).toInt()
            cv.drawCircle(WX, WY, 280f, p)
            p.shader = null
            p.alpha = 255
            val k = WW / word.width
            val hh = word.height * k
            bp.alpha = (255 * ta).toInt()
            cv.drawBitmap(word, null, RectF(WX - WW / 2f, WY - hh / 2f, WX + WW / 2f, WY + hh / 2f), bp)
            bp.alpha = 255
        }
        cv.restore()

        // затемнение и переход в приложение
        if (t > 3400f) {
            p.shader = null
            p.color = Color.argb((255 * ((t - 3400f) / 400f).coerceIn(0f, 1f)).toInt(), 0, 0, 0)
            cv.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
        }
        if (t >= 3800f) {
            if (!finished) {
                finished = true
                post { onDone() }
            }
        } else {
            postInvalidateOnAnimation()
        }
    }
}
