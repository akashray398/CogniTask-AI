package com.example.knotes.ui.analytics

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.knotes.R
import com.example.knotes.domain.usecase.AchievementBadge
import com.example.knotes.domain.usecase.DailyActivity
import com.example.knotes.domain.usecase.ProductivityAnalyticsReport

@Composable
fun AnalyticsScreenCompose(
    report: ProductivityAnalyticsReport,
    onBackClick: () -> Unit
) {
    var selectedDayActivity by remember { mutableStateOf<DailyActivity?>(null) }

    val animatedScore by animateIntAsState(
        targetValue = report.weeklyScorePercent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "weeklyScore"
    )

    val animatedCompletionRate by animateIntAsState(
        targetValue = report.completionRatePercent,
        animationSpec = tween(durationMillis = 800),
        label = "completionRate"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Bar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Executive Analytics 📊",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Live Dashboard Engine",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "🔥 ${report.currentStreak}d Streak",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Executive KPI Cards (2x2 Grid)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // KPI Card 1: Weekly Output Ring
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "WEEKLY SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(60.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { animatedScore / 100f },
                                    modifier = Modifier.size(60.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    strokeWidth = 6.dp
                                )
                                Text(
                                    text = "$animatedScore%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${report.weeklyCompletedCount}/${report.weeklyTotalCount} Tasks Done",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // KPI Card 2: Velocity & Trend
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "VELOCITY TREND",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (report.velocityTrendPercent >= 0) "+${report.velocityTrendPercent}%" else "${report.velocityTrendPercent}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = if (report.velocityTrendPercent >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.velocityTrendLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // KPI Card 3: Completion Rate
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "COMPLETION RATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$animatedCompletionRate%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { animatedCompletionRate / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // KPI Card 4: Critical Items Alert
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (report.highPriorityPendingCount > 0)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "HIGH PRIORITY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = if (report.highPriorityPendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${report.highPriorityPendingCount}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = if (report.highPriorityPendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (report.highPriorityPendingCount > 0) "Urgent tasks pending" else "Backlog clear! ✨",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 35-Day Executive Habit Heatmap (7 Columns x 5 Rows Grid)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "35-Day Habit Heatmap 🟩",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Real-time daily activity matrix",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Tap cell to inspect",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day Header Row (SUN MON TUE WED THU FRI SAT)
                    val daysHeader = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        daysHeader.forEach { dayLetter ->
                            Text(
                                text = dayLetter,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5 Rows x 7 Columns Grid
                    val rows = report.heatmapGrid.chunked(7)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rows.forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { activity ->
                                    val cellColor = when (activity.level) {
                                        4 -> MaterialTheme.colorScheme.primary
                                        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                    }

                                    val isSelected = selectedDayActivity?.dateMillis == activity.dateMillis

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cellColor)
                                            .then(
                                                if (isSelected) Modifier.border(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.onSurface,
                                                    RoundedCornerShape(8.dp)
                                                ) else Modifier
                                            )
                                            .clickable {
                                                selectedDayActivity = activity
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = activity.dayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = if (activity.level >= 2) FontWeight.Bold else FontWeight.Normal,
                                            color = if (activity.level >= 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Legend Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Less",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        listOf(0, 1, 2, 3, 4).forEach { lvl ->
                            val color = when (lvl) {
                                4 -> MaterialTheme.colorScheme.primary
                                3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Dynamic Executive Day Inspector Sheet (Animated when cell is clicked)
            AnimatedVisibility(
                visible = selectedDayActivity != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedDayActivity?.let { activity ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "📅 ${activity.fullDateLabel}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Level ${activity.level} Output",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Text(
                                    text = "✕",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.clickable { selectedDayActivity = null }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ActionStatChip(label = "Tasks Completed", count = activity.completedTasks)
                                ActionStatChip(label = "Tasks Created", count = activity.createdTasks)
                                ActionStatChip(label = "Notes Activity", count = activity.createdNotes + activity.updatedNotes)
                                ActionStatChip(label = "Total Actions", count = activity.totalActions)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Past 7-Day Velocity Bar Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "7-Day Productivity Velocity ⚡",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxCount = maxOf(1, report.dailyActivities.maxOfOrNull { it.totalActions } ?: 1)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        report.dailyActivities.forEach { activity ->
                            val barHeightRatio = (activity.totalActions.toFloat() / maxCount.toFloat()).coerceIn(0.12f, 1.0f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDayActivity = activity }
                            ) {
                                Text(
                                    text = "${activity.totalActions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activity.totalActions > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(barHeightRatio)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (activity.totalActions > 0)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = activity.dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Peak Window & Live Focus Window Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ai_assist),
                                    contentDescription = "Peak Performance",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "HISTORICAL PEAK WINDOW ⚡",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = report.peakWindowLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${report.peakWindowCount} tasks",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = report.activeWindowLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.activeWindowRecommendation,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Achievement Badges Carousel
            Text(
                text = "Milestones & Achievement Badges 🏆",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(report.badges) { badge ->
                    BadgeCard(badge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActionStatChip(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun BadgeCard(badge: AchievementBadge) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(170.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = CardDefaults.outlinedCardBorder(enabled = badge.isUnlocked)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = badge.icon,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = if (badge.isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { badge.progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (badge.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (badge.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (badge.isUnlocked) "Unlocked ✓" else badge.progressText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isUnlocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
