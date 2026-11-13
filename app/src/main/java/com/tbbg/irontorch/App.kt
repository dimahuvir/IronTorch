package com.tbbg.irontorch

import android.app.Application
import java.io.PrintWriter
import java.io.StringWriter

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                var root: Throwable = e
                while (root.cause != null && root.cause !== root) root = root.cause!!
                val sb = StringBuilder()
                sb.append("ПРИЧИНА: ").append(root.toString()).append("\n")
                for (f in root.stackTrace.take(8)) sb.append("  ").append(f.toString()).append("\n")
                sb.append("\n--- полный отчёт ---\n")
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                sb.append(sw.toString())
                getSharedPreferences("crash", MODE_PRIVATE).edit().putString("last", sb.toString()).commit()
            } catch (x: Throwable) {
            }
            prev?.uncaughtException(t, e)
        }
    }
}
