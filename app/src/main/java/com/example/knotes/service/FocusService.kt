package com.example.knotes.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.knotes.MainActivity
import com.example.knotes.R
import com.example.knotes.util.FocusAlarmPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class FocusTimerState(
    val isActive: Boolean = false,
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isCompleted: Boolean = false
)

class FocusService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null

    companion object {
        private const val TAG = "FocusService"
        const val CHANNEL_ID = "focus_burst_channel"
        const val NOTIFICATION_ID = 200001

        const val ACTION_START = "ACTION_START_FOCUS"
        const val ACTION_PAUSE_RESUME = "ACTION_PAUSE_RESUME_FOCUS"
        const val ACTION_STOP = "ACTION_STOP_FOCUS"
        const val ACTION_DISMISS = "ACTION_DISMISS_FOCUS"

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"

        private val _focusTimerState = MutableStateFlow(FocusTimerState())
        val focusTimerState: StateFlow<FocusTimerState> = _focusTimerState.asStateFlow()

        fun startFocus(context: Context, durationMinutes: Int) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_SECONDS, durationMinutes * 60)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun togglePauseResume(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_PAUSE_RESUME
            }
            context.startService(intent)
        }

        fun stopFocus(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun dismissCompletion(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_DISMISS
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationSec = intent.getIntExtra(EXTRA_DURATION_SECONDS, 15 * 60)
                startTimer(durationSec)
            }
            ACTION_PAUSE_RESUME -> {
                togglePauseResumeInternal()
            }
            ACTION_STOP -> {
                stopTimerInternal()
            }
            ACTION_DISMISS -> {
                dismissInternal()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(durationSeconds: Int) {
        timerJob?.cancel()
        _focusTimerState.value = FocusTimerState(
            isActive = true,
            isRunning = true,
            remainingSeconds = durationSeconds,
            totalSeconds = durationSeconds,
            isCompleted = false
        )

        startForegroundWithNotification()

        timerJob = serviceScope.launch {
            while (isActive && _focusTimerState.value.remainingSeconds > 0) {
                if (_focusTimerState.value.isRunning) {
                    delay(1000L)
                    if (_focusTimerState.value.isRunning && _focusTimerState.value.remainingSeconds > 0) {
                        val newRemaining = _focusTimerState.value.remainingSeconds - 1
                        _focusTimerState.value = _focusTimerState.value.copy(remainingSeconds = newRemaining)
                        updateNotification()
                    }
                } else {
                    delay(500L)
                }
            }

            if (_focusTimerState.value.remainingSeconds == 0 && _focusTimerState.value.isActive) {
                onTimerCompleted()
            }
        }
    }

    private fun togglePauseResumeInternal() {
        val current = _focusTimerState.value
        if (!current.isActive) return
        val newRunning = !current.isRunning
        _focusTimerState.value = current.copy(isRunning = newRunning)
        updateNotification()
    }

    private fun stopTimerInternal() {
        timerJob?.cancel()
        _focusTimerState.value = FocusTimerState()
        FocusAlarmPlayer.stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun dismissInternal() {
        timerJob?.cancel()
        _focusTimerState.value = FocusTimerState()
        FocusAlarmPlayer.stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerCompleted() {
        _focusTimerState.value = _focusTimerState.value.copy(
            isRunning = false,
            isCompleted = true
        )

        // Play 528Hz Meditation Zen Bell Chime
        FocusAlarmPlayer.playCalmAlarm(this, 10_000L)

        showCompletionNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Burst Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing Focus Burst countdown notification"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Foreground service type start warning: ${e.message}")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildOngoingNotification())
    }

    private fun buildOngoingNotification(): Notification {
        val state = _focusTimerState.value
        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, FocusService::class.java).apply { action = ACTION_PAUSE_RESUME }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseLabel = if (state.isRunning) "⏸ Pause" else "▶ Resume"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tasks)
            .setContentTitle("🎯 Focus Burst Active: $formattedTime")
            .setContentText("Stay in the flow! Timer keeps counting on lock screen.")
            .setSubText("KNotes Focus Mode")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on Lock Screen!
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_reminder, pauseLabel, pauseResumePendingIntent)
            .addAction(R.drawable.ic_back, "✕ Stop", stopPendingIntent)
            .setColor(getColor(R.color.purple_6750A4))
            .build()
    }

    private fun showCompletionNotification() {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tasks)
            .setContentTitle("🎉 Focus Burst Complete!")
            .setContentText("Great job! You completed your focus session. Sounding calm meditation chime.")
            .setSubText("KNotes Focus Mode")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on Lock Screen!
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .setColor(getColor(R.color.purple_6750A4))
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
