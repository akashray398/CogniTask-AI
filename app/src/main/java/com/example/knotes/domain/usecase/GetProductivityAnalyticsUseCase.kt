package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Note
import com.example.knotes.domain.model.Priority
import com.example.knotes.domain.model.Task
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DailyActivity(
    val dateMillis: Long,
    val dayLabel: String,
    val fullDateLabel: String,
    val completedTasks: Int,
    val createdTasks: Int,
    val createdNotes: Int,
    val updatedNotes: Int,
    val totalTasks: Int,
    val totalActions: Int,
    val level: Int // 0 to 4
)

data class AchievementBadge(
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progressText: String,
    val progressRatio: Float
)

data class ProductivityAnalyticsReport(
    val weeklyScorePercent: Int,
    val weeklyCompletedCount: Int,
    val weeklyTotalCount: Int,
    val weeklyActionsCount: Int,
    val velocityTrendPercent: Int,
    val velocityTrendLabel: String,
    val completionRatePercent: Int,
    val highPriorityPendingCount: Int,
    val totalActiveNotesCount: Int,
    val totalActiveTasksCount: Int,
    val totalCompletedTasksCount: Int,
    val averageDailyActions: Float,
    val peakDayLabel: String,
    val dailyActivities: List<DailyActivity>, // Past 7 Days
    val heatmapGrid: List<DailyActivity>,     // Past 35 Days (5 weeks x 7 days)
    val peakWindowLabel: String,
    val peakWindowCount: Int,
    val activeWindowLabel: String,
    val activeWindowRecommendation: String,
    val badges: List<AchievementBadge>,
    val currentStreak: Int,
    val highestStreak: Int
)

class GetProductivityAnalyticsUseCase @Inject constructor() {

    operator fun invoke(
        notes: List<Note>,
        tasks: List<Task>,
        currentStreak: Int,
        highestStreak: Int
    ): ProductivityAnalyticsReport {
        val now = System.currentTimeMillis()
        val activeNotes = notes.filter { !it.isTrashed }

        // 1. Calculate Past 7 Days Activity for Bar Chart
        val dailyActivities = mutableListOf<DailyActivity>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        for (i in 6 downTo 0) {
            val calDay = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calDay.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L

            val completedOnDay = tasks.count { task ->
                task.isCompleted && ((task.lastCompletedTime ?: task.updatedTime) in startOfDay..endOfDay)
            }

            val createdTasksOnDay = tasks.count { task ->
                task.createdTime in startOfDay..endOfDay
            }

            val createdNotesOnDay = activeNotes.count { note ->
                note.createdTime in startOfDay..endOfDay
            }

            val updatedNotesOnDay = activeNotes.count { note ->
                note.updatedTime in startOfDay..endOfDay && note.updatedTime != note.createdTime
            }

            val totalTasksOnDay = tasks.count { task ->
                val targetTime = task.deadline ?: task.reminderTime ?: task.createdTime
                targetTime in startOfDay..endOfDay
            }

            val totalActions = completedOnDay + createdTasksOnDay + createdNotesOnDay + updatedNotesOnDay

            val level = when {
                totalActions >= 8 -> 4
                totalActions >= 5 -> 3
                totalActions >= 3 -> 2
                totalActions >= 1 -> 1
                else -> 0
            }

            dailyActivities.add(
                DailyActivity(
                    dateMillis = startOfDay,
                    dayLabel = dayFormat.format(Date(startOfDay)),
                    fullDateLabel = fullDateFormat.format(Date(startOfDay)),
                    completedTasks = completedOnDay,
                    createdTasks = createdTasksOnDay,
                    createdNotes = createdNotesOnDay,
                    updatedNotes = updatedNotesOnDay,
                    totalTasks = maxOf(totalTasksOnDay, completedOnDay),
                    totalActions = totalActions,
                    level = level
                )
            )
        }

        // 2. Calculate Past 35 Days Heatmap Grid (5 weeks x 7 days)
        val heatmapGrid = mutableListOf<DailyActivity>()
        val shortDayFormat = SimpleDateFormat("d", Locale.getDefault())

        for (i in 34 downTo 0) {
            val calDay = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calDay.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L

            val completedOnDay = tasks.count { task ->
                task.isCompleted && ((task.lastCompletedTime ?: task.updatedTime) in startOfDay..endOfDay)
            }

            val createdTasksOnDay = tasks.count { task ->
                task.createdTime in startOfDay..endOfDay
            }

            val createdNotesOnDay = activeNotes.count { note ->
                note.createdTime in startOfDay..endOfDay
            }

            val updatedNotesOnDay = activeNotes.count { note ->
                note.updatedTime in startOfDay..endOfDay && note.updatedTime != note.createdTime
            }

            val totalTasksOnDay = tasks.count { task ->
                val targetTime = task.deadline ?: task.reminderTime ?: task.createdTime
                targetTime in startOfDay..endOfDay
            }

            val totalActions = completedOnDay + createdTasksOnDay + createdNotesOnDay + updatedNotesOnDay

            val level = when {
                totalActions >= 8 -> 4
                totalActions >= 5 -> 3
                totalActions >= 3 -> 2
                totalActions >= 1 -> 1
                else -> 0
            }

            heatmapGrid.add(
                DailyActivity(
                    dateMillis = startOfDay,
                    dayLabel = shortDayFormat.format(Date(startOfDay)),
                    fullDateLabel = fullDateFormat.format(Date(startOfDay)),
                    completedTasks = completedOnDay,
                    createdTasks = createdTasksOnDay,
                    createdNotes = createdNotesOnDay,
                    updatedNotes = updatedNotesOnDay,
                    totalTasks = maxOf(totalTasksOnDay, completedOnDay),
                    totalActions = totalActions,
                    level = level
                )
            )
        }

        // 3. Weekly Summary Totals & Velocity Calculation
        val weeklyCompleted = dailyActivities.sumOf { it.completedTasks }
        val weeklyTotal = dailyActivities.sumOf { it.totalTasks }
        val weeklyActions = dailyActivities.sumOf { it.totalActions }
        val weeklyPercent = if (weeklyTotal > 0) minOf(100, ((weeklyCompleted.toFloat() / weeklyTotal.toFloat()) * 100).toInt())
        else if (weeklyActions > 0) minOf(100, weeklyActions * 10)
        else 0

        // Calculate Velocity Trend (Past 7 Days vs Previous 7 Days)
        var previous7DaysActions = 0
        for (i in 13 downTo 7) {
            val calDay = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calDay.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L

            val completed = tasks.count { task ->
                task.isCompleted && ((task.lastCompletedTime ?: task.updatedTime) in startOfDay..endOfDay)
            }
            val createdT = tasks.count { it.createdTime in startOfDay..endOfDay }
            val createdN = activeNotes.count { it.createdTime in startOfDay..endOfDay }
            val updatedN = activeNotes.count { it.updatedTime in startOfDay..endOfDay && it.updatedTime != it.createdTime }

            previous7DaysActions += (completed + createdT + createdN + updatedN)
        }

        val velocityTrendPercent = if (previous7DaysActions == 0) {
            if (weeklyActions > 0) 100 else 0
        } else {
            (((weeklyActions - previous7DaysActions).toFloat() / previous7DaysActions.toFloat()) * 100).toInt()
        }

        val velocityTrendLabel = when {
            velocityTrendPercent > 0 -> "📈 +$velocityTrendPercent% output vs last week"
            velocityTrendPercent < 0 -> "📉 $velocityTrendPercent% output vs last week"
            else -> "➡️ Steady performance vs last week"
        }

        // Executive High-Level KPIs
        val totalCompletedAllTime = tasks.count { it.isCompleted }
        val totalTasksAllTime = tasks.size
        val completionRatePercent = if (totalTasksAllTime > 0) {
            ((totalCompletedAllTime.toFloat() / totalTasksAllTime.toFloat()) * 100).toInt()
        } else 0

        val highPriorityPending = tasks.count { !it.isCompleted && it.priority == Priority.HIGH }
        val averageDailyActions = heatmapGrid.sumOf { it.totalActions }.toFloat() / 35.0f

        val peakDay = heatmapGrid.maxByOrNull { it.totalActions }
        val peakDayLabel = if (peakDay != null && peakDay.totalActions > 0) {
            "${peakDay.fullDateLabel} (${peakDay.totalActions} actions)"
        } else {
            "No activity recorded yet"
        }

        // 4. Calculate Peak Productivity Window & Current Live Active Window
        var morningCount = 0
        var afternoonCount = 0
        var eveningCount = 0
        var nightCount = 0

        tasks.filter { it.isCompleted }.forEach { task ->
            val time = task.lastCompletedTime ?: task.updatedTime
            if (time > 0L) {
                val cal = Calendar.getInstance().apply { timeInMillis = time }
                when (cal.get(Calendar.HOUR_OF_DAY)) {
                    in 5..11 -> morningCount++
                    in 12..16 -> afternoonCount++
                    in 17..21 -> eveningCount++
                    else -> nightCount++
                }
            }
        }

        val windowMap = mapOf(
            "🌅 Morning (5 AM - 12 PM)" to morningCount,
            "☀️ Afternoon (12 PM - 5 PM)" to afternoonCount,
            "🌆 Evening (5 PM - 10 PM)" to eveningCount,
            "🌙 Night (10 PM - 5 AM)" to nightCount
        )
        val peakEntry = windowMap.maxByOrNull { it.value } ?: windowMap.entries.first()

        // Determine Live Current Active System Window & Executive Recommendation
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val activeWindowLabel = when (currentHour) {
            in 5..11 -> "🌅 Morning Focus Window (5 AM - 12 PM)"
            in 12..16 -> "☀️ Afternoon Focus Window (12 PM - 5 PM)"
            in 17..21 -> "🌆 Evening Focus Window (5 PM - 10 PM)"
            else -> "🌙 Night Focus Window (10 PM - 5 AM)"
        }

        val activeWindowRecommendation = when (currentHour) {
            in 5..11 -> "Optimal window for high-priority executive planning & deep focus."
            in 12..16 -> "Ideal time for execution, task resolution, and AI note summaries."
            in 17..21 -> "Great window to wrap up pending tasks and review day velocity."
            else -> "Quiet hours for creative note sketching and backlog planning."
        }

        // 5. Achievement Badges with Real Progress
        val effectiveStreak = maxOf(currentStreak, highestStreak)

        val badges = listOf(
            AchievementBadge(
                title = "First Step 🎯",
                description = "Complete your first task",
                icon = "🎯",
                isUnlocked = totalCompletedAllTime >= 1,
                progressText = if (totalCompletedAllTime >= 1) "1/1 Done" else "0/1 Done",
                progressRatio = minOf(1.0f, totalCompletedAllTime.toFloat() / 1.0f)
            ),
            AchievementBadge(
                title = "3-Day Spark 🔥",
                description = "Maintain a 3-day streak",
                icon = "🔥",
                isUnlocked = effectiveStreak >= 3,
                progressText = "${minOf(effectiveStreak, 3)}/3 Days",
                progressRatio = minOf(1.0f, effectiveStreak.toFloat() / 3.0f)
            ),
            AchievementBadge(
                title = "Week Warrior 👑",
                description = "Maintain a 7-day streak",
                icon = "👑",
                isUnlocked = effectiveStreak >= 7,
                progressText = "${minOf(effectiveStreak, 7)}/7 Days",
                progressRatio = minOf(1.0f, effectiveStreak.toFloat() / 7.0f)
            ),
            AchievementBadge(
                title = "Task Crusher ⚡",
                description = "Complete 10+ tasks",
                icon = "⚡",
                isUnlocked = totalCompletedAllTime >= 10,
                progressText = "${minOf(totalCompletedAllTime, 10)}/10 Tasks",
                progressRatio = minOf(1.0f, totalCompletedAllTime.toFloat() / 10.0f)
            ),
            AchievementBadge(
                title = "Knowledge Master 🧠",
                description = "Create 5+ notes in library",
                icon = "🧠",
                isUnlocked = activeNotes.size >= 5,
                progressText = "${minOf(activeNotes.size, 5)}/5 Notes",
                progressRatio = minOf(1.0f, activeNotes.size.toFloat() / 5.0f)
            )
        )

        return ProductivityAnalyticsReport(
            weeklyScorePercent = weeklyPercent,
            weeklyCompletedCount = weeklyCompleted,
            weeklyTotalCount = maxOf(weeklyTotal, weeklyCompleted),
            weeklyActionsCount = weeklyActions,
            velocityTrendPercent = velocityTrendPercent,
            velocityTrendLabel = velocityTrendLabel,
            completionRatePercent = completionRatePercent,
            highPriorityPendingCount = highPriorityPending,
            totalActiveNotesCount = activeNotes.size,
            totalActiveTasksCount = tasks.count { !it.isCompleted },
            totalCompletedTasksCount = totalCompletedAllTime,
            averageDailyActions = averageDailyActions,
            peakDayLabel = peakDayLabel,
            dailyActivities = dailyActivities,
            heatmapGrid = heatmapGrid,
            peakWindowLabel = peakEntry.key,
            peakWindowCount = peakEntry.value,
            activeWindowLabel = activeWindowLabel,
            activeWindowRecommendation = activeWindowRecommendation,
            badges = badges,
            currentStreak = currentStreak,
            highestStreak = highestStreak
        )
    }
}
