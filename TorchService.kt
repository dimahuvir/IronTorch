package com.tbbg.irontorch

import android.app.*
import android.content.*
import android.os.IBinder

class TorchService : Service() {
    private val hits = ArrayList<Long>()
    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            // 1 нажатие питания = 1 событие экрана (вкл/выкл). 3 события за 2 сек = тройное нажатие
            val now = System.currentTimeMillis()
            hits.add(now)
            hits.removeAll { now - it > 2000 }
            if (hits.size >= 3) { hits.clear(); Torch.toggle() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Torch.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("it", "Iron Torch", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "it")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle("☠ IRON TORCH НА ПОСТУ")
            .setContentText("3x кнопка питания = фонарь | By TBBG")
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build()
        startForeground(1, n)
        registerReceiver(rx, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int) = START_STICKY
    override fun onDestroy() { unregisterReceiver(rx); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        c.startForegroundService(Intent(c, TorchService::class.java))
    }
}
