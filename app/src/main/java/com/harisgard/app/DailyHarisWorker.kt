package com.harisgard.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar

class DailyHarisWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("haris_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("daily_enabled", false)) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return Result.success()
        val lines = listOf(
            "هریس را با نگاه تازه‌ای ببین؛ زیبایی در جزئیات پنهان است.",
            "هر سفر خوب با احترام به مردم و طبیعت آغاز می‌شود.",
            "نقش فرش هریس، روایت صبر و هنر دست‌های هنرمند است.",
            "امروز فرصتی تازه برای شناختن گوشه‌ای از سرزمینمان است.",
            "طبیعت را همان‌طور که دوست داریم، پاک و زیبا نگه داریم.",
            "میراث فرهنگی با شناختن و پاس‌داشتن آن زنده می‌ماند.",
            "گاهی یک مسیر کوتاه، خاطره‌ای ماندگار می‌سازد."
        )
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val text = lines[day % lines.size]
        val date = jalaliDate()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "haris_daily"
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(channelId, "پیام روزانه هریس‌گرد", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "یک جمله کوتاه همراه تاریخ، هر روز" })
        val intent = Intent(applicationContext, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = PendingIntent.getActivity(applicationContext, 410, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_harisgard)
            .setContentTitle("هریس‌گرد · $date")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\nتاریخ: $date"))
            .setContentIntent(pending).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        manager.notify(410, notification)
        return Result.success()
    }

    private fun jalaliDate(): String {
        val c = Calendar.getInstance(); val gy = c.get(Calendar.YEAR); val gm = c.get(Calendar.MONTH) + 1; val gd = c.get(Calendar.DAY_OF_MONTH)
        val gdm = intArrayOf(0,31,59,90,120,151,181,212,243,273,304,334)
        var gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053); days %= 12053
        jy += 4 * (days / 1461); days %= 1461
        if (days > 365) { jy += (days - 1) / 365; days = (days - 1) % 365 }
        val jm: Int; val jd: Int
        if (days < 186) { jm = 1 + days / 31; jd = 1 + days % 31 } else { jm = 7 + (days - 186) / 30; jd = 1 + (days - 186) % 30 }
        return "%04d/%02d/%02d".format(jy, jm, jd).map { ch -> if (ch in '0'..'9') ('۰'.code + (ch.code - '0'.code)).toChar() else ch }.joinToString("")
    }
}
