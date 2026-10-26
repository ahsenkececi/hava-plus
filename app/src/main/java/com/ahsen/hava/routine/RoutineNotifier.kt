package com.ahsen.hava.routine

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ahsen.hava.AppActions
import com.ahsen.hava.MainActivity
import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.RoutineReport
import com.ahsen.hava.domain.model.Routine
import java.time.LocalDateTime

/** Rutin bildirimini kurar ve gösterir. Dokununca Yolculuk+ rutin rotasıyla açılır. */
class RoutineNotifier(private val context: Context) {

    // İzin canNotify() içinde kontrol ediliyor; lint bunu göremediği için uyarıyı kapatıyoruz.
    @SuppressLint("MissingPermission")
    fun show(routine: Routine, departure: LocalDateTime, report: RoutineReport) {
        if (!canNotify()) return
        ensureChannel()

        val lines = report.lines().map { it.resolve(context) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(routineTitle(routine, departure).resolve(context))
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(openTripIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Android 13+ bildirim için çalışma anı izni ister; izin yoksa sessizce vazgeç. */
    private fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.routine_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.routine_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openTripIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(AppActions.OPEN_ROUTINE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val CHANNEL_ID = "routine"
        private const val NOTIFICATION_ID = 1001
    }
}
