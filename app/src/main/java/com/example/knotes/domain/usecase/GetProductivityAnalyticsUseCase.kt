package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Note
import com.example.knotes.domain.model.Task
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DailyActivity(
    val dateMillis: Long,
    val dayLabel: String,
    val completedTasks: Int,
    val totalTasks: Int,
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
    val dailyActivities: List<DailyActivity>,
    val heatmapGrid: List<DailyActivity>,
    val peakWindowLabel: String,
    val peakWindowCount: Int,
    val activeWindowLabel: String,
    val badges: List<AchievementBadge>,
    val currentStreak: Int,
    val highestStreak: Int,
    val totalNotes: Int
)

class GetProductivityAnalyticsUseCase @Inject constructor() {

    operator fun invoke(
        notes: List<Note>,
        tasks: List<Task>,
        currentStreak: Int,
        highestStreak: Int
    ): ProductivityAnalyticsReport {
        val now = System.currentTimeMillis()

        // 1. Calculate Past 7 Days Activity for Bar Chart
        val dailyActivities = mutableListOf<DailyActivity>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

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
                if (!task.isCompleted) false
                else {
                    val compTime = task.lastCompletedTime ?: task.updatedTime
                    compTime in startOfDay..endOfDay
                }
            }

            val totalOnDay = tasks.count { task ->
                val targetTime = task.deadline ?: task.reminderTime ?: task.createdTime
                targetTime in startOfDay..endOfDay
            }

            val count = maxOf(completedOnDay, totalOnDay)
            val level = when {
                count >= 5 -> 4
                count >= 3 -> 3
                count >= 2 -> 2
                count >= 1 -> 1
                else -> 0
            }

            dailyActivities.add(
                DailyActivity(
                    dateMillis = startOfDay,
                    dayLabel = dayFormat.format(Date(startOfDay)),
                    completedTasks = completedOnDay,
                    totalTasks = maxOf(totalOnDay, completedOnDay),
                    level = level
                )
            )
        }

        // 2. Calculate Past 30 Days Heatmap Grid
        val heatmapGrid = mutableListOf<DailyActivity>()
        val shortDayFormat = SimpleDateFormat("d", Locale.getDefault())

        for (i in 29 downTo 0) {
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
                if (!task.isCompleted) false
                else {
                    val compTime = task.lastCompletedTime ?: task.updatedTime
                    compTime in startOfDay..endOfDay
                }
            }

            val level = when {
                completedOnDay >= 5 -> 4
                completedOnDay >= 3 -> 3
                completedOnDay >= 2 -> 2
                completedOnDay >= 1 -> 1
                else -> 0
            }

            heatmapGrid.add(
                DailyActivity(
                    dateMillis = startOfDay,
                    dayLabel = shortDayFormat.format(Date(startOfDay)),
                    completedTasks = completedOnDay,
                    totalTasks = completedOnDay,
                    level = level
                )
            )
        }

        // 3. Weekly Summary Totals
        val weeklyCompleted = dailyActivities.sumOf { it.completedTasks }
        val weeklyTotal = dailyActivities.sumOf { it.totalTasks }
        val weeklyPercent = if (weeklyTotal > 0) minOf(100, ((weeklyCompleted.toFloat() / weeklyTotal.toFloat()) * 100).toInt()) else 0

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

        // Determine Live Current Active Window
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val activeWindowLabel = when (currentHour) {
            in 5..11 -> "🌅 Morning Focus Window (5 AM - 12 PM)"
            in 12..16 -> "☀️ Afternoon Focus Window (12 PM - 5 PM)"
            in 17..21 -> "🌆 Evening Focus Window (5 PM - 10 PM)"
            else -> "🌙 Night Focus Window (10 PM - 5 AM)"
        }

        // 5. Achievement Badges with Real Progress
        val totalCompletedAllTime = tasks.count { it.isCompleted }
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
                isUnlocked = notes.size >= 5,
                progressText = "${minOf(notes.size, 5)}/5 Notes",
                progressRatio = minOf(1.0f, notes.size.toFloat() / 5.0f)
            )
        )

        return ProductivityAnalyticsReport(
            weeklyScorePercent = weeklyPercent,
            weeklyCompletedCount = weeklyCompleted,
            weeklyTotalCount = maxOf(weeklyTotal, weeklyCompleted),
            dailyActivities = dailyActivities,
            heatmapGrid = heatmapGrid,
            peakWindowLabel = peakEntry.key,
            peakWindowCount = peakEntry.value,
            activeWindowLabel = activeWindowLabel,
            badges = badges,
            currentStreak = currentStreak,
            highestStreak = highestStreak,
            totalNotes = notes.size
        )
    }
}
