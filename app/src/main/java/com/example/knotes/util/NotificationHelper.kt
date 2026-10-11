package com.example.knotes.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.knotes.MainActivity
import com.example.knotes.R
import com.example.knotes.receiver.ReminderReceiver

object NotificationHelper {
    const val CHANNEL_ID_TASKS = "tasks_reminders"
    const val CHANNEL_ID_DAILY = "daily_reminders"
    const val CHANNEL_ID_OVERDUE = "overdue_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val taskChannel = NotificationChannel(CHANNEL_ID_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders for upcoming tasks"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            val dailyChannel = NotificationChannel(CHANNEL_ID_DAILY, "Daily Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily summary of pending tasks"
                enableVibration(true)
            }
            
            val overdueChannel = NotificationChannel(CHANNEL_ID_OVERDUE, "Overdue Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts for overdue tasks"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(listOf(taskChannel, dailyChannel, overdueChannel))
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        id: Int,
        title: String,
        content: String,
        targetScreen: String? = null,
        taskId: Int? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", targetScreen)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = createLogoLargeIcon(context)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_tasks)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText("KNotes AI Workspace ✦")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.purple_6750A4))

        // Interactive Notification Shade Actions for Tasks
        if (taskId != null && taskId > 0) {
            // Action 1: Mark Done
            val markDoneIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = "ACTION_MARK_TASK_DONE"
                putExtra("taskId", taskId)
                putExtra("notificationId", id)
            }
            val markDonePendingIntent = PendingIntent.getBroadcast(
                context,
                id * 10 + 1,
                markDoneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_tasks,
                "✓ Mark Done",
                markDonePendingIntent
            )

            // Action 2: Snooze 15m
            val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = "ACTION_SNOOZE_TASK"
                putExtra("taskId", taskId)
                putExtra("notificationId", id)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                id * 10 + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_reminder,
                "⏰ Snooze 15m",
                snoozePendingIntent
            )
        }

        notificationManager.notify(id, builder.build())
    }

    private fun createLogoLargeIcon(context: Context): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                size / 2f, size / 2f, size / 2f,
                intArrayOf(Color.parseColor("#7D5260"), Color.parseColor("#6750A4"), Color.parseColor("#381E72")),
                null, Shader.TileMode.CLAMP
            )
        }

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw White 'K' Emblem
        val linePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            strokeWidth = 12f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        canvas.drawLine(44f, 32f, 44f, 96f, linePaint)
        canvas.drawLine(44f, 64f, 84f, 32f, linePaint)
        canvas.drawLine(48f, 64f, 88f, 96f, linePaint)

        return bitmap
    }
}
