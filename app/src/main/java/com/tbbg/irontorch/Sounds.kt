package com.tbbg.irontorch

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object Sounds {
    private const val SR = 22050
    @Volatile var muted = false
    @Volatile var sirenOn = false
    private var gen = 0
    private var pingTrack: AudioTrack? = null

    private fun track(bytes: Int, mode: Int): AudioTrack =
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bytes)
            .setTransferMode(mode)
            .build()

    private fun makePing(): AudioTrack {
        val n = SR
        val d = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            var v = sin(2 * PI * 1300 * t) * exp(-t * 7)
            val t2 = t - 0.4
            if (t2 > 0) v += 0.35 * sin(2 * PI * 1300 * t2) * exp(-t2 * 9)
            d[i] = (v * 9000).toInt().toShort()
        }
        val tr = track(n * 2, AudioTrack.MODE_STATIC)
        tr.write(d, 0, n)
        return tr
    }

    fun ping() {
        if (muted) return
        try {
            val t = pingTrack ?: makePing().also { pingTrack = it }
            t.stop()
            t.reloadStaticData()
            t.play()
        } catch (e: Exception) {}
    }

    fun stopPing() {
        try { pingTrack?.stop() } catch (e: Exception) {}
    }

    fun siren(on: Boolean) {
        if (on == sirenOn) return
        sirenOn = on
        gen++
        if (!on) return
        val g = gen
        Thread {
            val min = AudioTrack.getMinBufferSize(
                SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val tr = track(maxOf(min, 4096), AudioTrack.MODE_STREAM)
            tr.play()
            val buf = ShortArray(1024)
            var phase = 0.0
            var t = 0.0
            while (sirenOn && g == gen) {
                for (i in buf.indices) {
                    val f = 1000 + 450 * sin(2 * PI * t / 1.5)
                    phase += 2 * PI * f / SR
                    val v = sin(phase) + 0.3 * sin(2 * phase)
                    buf[i] = (v * 8000).toInt().toShort()
                    t += 1.0 / SR
                }
                tr.write(buf, 0, buf.size)
            }
            tr.stop()
            tr.release()
        }.start()
    }
}
