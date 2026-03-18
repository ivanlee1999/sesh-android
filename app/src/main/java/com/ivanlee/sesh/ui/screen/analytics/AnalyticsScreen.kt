package com.ivanlee.sesh.ui.screen.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.ivanlee.sesh.data.db.dao.SessionWithCategory
import com.ivanlee.sesh.ui.components.EinkBarChart
import com.ivanlee.sesh.ui.theme.EinkColors
import com.ivanlee.sesh.ui.theme.EinkTypography
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val todaySessions by viewModel.todaySessions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EinkColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Analytics",
            style = EinkTypography.headlineLarge,
            color = EinkColors.OnBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // --- Today stats + Streak ---
        TodayStatsSection(
            todayMinutes = state.todayMinutes,
            sessionCount = state.todaySessionCount,
            streak = state.streak
        )

        SectionDivider()

        // --- Category Breakdown ---
        CategoryBreakdownSection(state.categoryBreakdown)

        SectionDivider()

        // --- 7-Day Chart ---
        Last7DaysSection(state.last7Days)

        SectionDivider()

        // --- Daily Timeline ---
        DailyTimelineSection(todaySessions)

        SectionDivider()

        // --- All Time ---
        AllTimeSection(state.allTimeMinutes)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TodayStatsSection(todayMinutes: Double, sessionCount: Int, streak: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today",
                style = EinkTypography.titleLarge,
                color = EinkColors.OnBackground
            )
            Text(
                text = formatMinutesDisplay(todayMinutes),
                style = EinkTypography.headlineLarge,
                color = EinkColors.OnBackground
            )
            Text(
                text = "$sessionCount session${if (sessionCount != 1) "s" else ""}",
                style = EinkTypography.bodyLarge,
                color = EinkColors.OnBackground
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Streak",
                style = EinkTypography.titleLarge,
                color = EinkColors.OnBackground
            )
            Text(
                text = "$streak day${if (streak != 1) "s" else ""}",
                style = EinkTypography.headlineLarge,
                color = EinkColors.OnBackground
            )
        }
    }
}

@Composable
private fun CategoryBreakdownSection(
    breakdown: List<com.ivanlee.sesh.data.db.dao.CategoryBreakdownResult>
) {
    Text(
        text = "Categories Today",
        style = EinkTypography.titleLarge,
        color = EinkColors.OnBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (breakdown.isEmpty()) {
        Text(
            text = "No focus sessions today",
            style = EinkTypography.bodyLarge,
            color = EinkColors.Disabled
        )
        return
    }

    val totalMinutes = breakdown.sumOf { it.minutes }.coerceAtLeast(1.0)

    breakdown.forEach { category ->
        val percentage = (category.minutes / totalMinutes * 100).toInt()
        val color = try {
            Color(category.color.toColorInt())
        } catch (e: Exception) {
            EinkColors.Disabled
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color bar (proportional width)
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .height(20.dp)
            ) {
                // Background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(EinkColors.Surface, RectangleShape)
                        .border(1.dp, EinkColors.Disabled, RectangleShape)
                )
                // Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (category.minutes / totalMinutes).toFloat().coerceIn(0f, 1f))
                        .height(20.dp)
                        .background(color, RectangleShape)
                        .border(1.dp, EinkColors.OnBackground, RectangleShape)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = category.name,
                style = EinkTypography.bodyMedium,
                color = EinkColors.OnBackground,
                modifier = Modifier.weight(0.3f)
            )

            Text(
                text = "$percentage%",
                style = EinkTypography.bodyMedium,
                color = EinkColors.OnBackground,
                modifier = Modifier.weight(0.2f)
            )
        }
    }
}

@Composable
private fun Last7DaysSection(last7Days: List<com.ivanlee.sesh.data.db.dao.DayFocusResult>) {
    Text(
        text = "Last 7 Days",
        style = EinkTypography.titleLarge,
        color = EinkColors.OnBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (last7Days.isEmpty()) {
        Text(
            text = "No data yet",
            style = EinkTypography.bodyLarge,
            color = EinkColors.Disabled
        )
        return
    }

    // Convert dates to day abbreviations
    val chartData = last7Days.map { dayFocus ->
        val dayLabel = try {
            val date = LocalDate.parse(dayFocus.date)
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .take(1)
                .uppercase()
        } catch (e: Exception) {
            "?"
        }
        Pair(dayLabel, dayFocus.hours.toFloat())
    }

    EinkBarChart(
        data = chartData,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun DailyTimelineSection(sessions: List<SessionWithCategory>) {
    Text(
        text = "Today's Timeline",
        style = EinkTypography.titleLarge,
        color = EinkColors.OnBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (sessions.isEmpty()) {
        Text(
            text = "No sessions today",
            style = EinkTypography.bodyLarge,
            color = EinkColors.Disabled
        )
        return
    }

    // Timeline: show each session as a colored block with time label
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, EinkColors.OnBackground, RectangleShape)
            .padding(2.dp)
    ) {
        sessions.forEach { session ->
            val startTime = formatTimeFromIso(session.startedAt)
            val durationMinutes = (session.actualSeconds - session.pauseSeconds) / 60
            val categoryColor = try {
                Color(session.categoryColor?.toColorInt() ?: 0xFFABB2BF.toInt())
            } catch (e: Exception) {
                EinkColors.Disabled
            }

            // Block height proportional to duration (minimum 32dp, scale: 1min = 2dp)
            val blockHeight = (durationMinutes * 2).coerceIn(32, 120).toInt()

            val isFocus = session.sessionType in listOf("full_focus", "partial_focus")
            val bgColor = if (isFocus) categoryColor else EinkColors.Break

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blockHeight.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time label
                Text(
                    text = startTime,
                    style = EinkTypography.bodyMedium,
                    color = EinkColors.OnBackground,
                    modifier = Modifier.width(48.dp)
                )

                // Session block
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(blockHeight.dp)
                        .background(bgColor, RectangleShape)
                        .border(1.dp, EinkColors.OnBackground, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = session.title.ifEmpty {
                                if (session.sessionType == "rest") "Break" else "Focus"
                            },
                            style = EinkTypography.bodyMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                        if (durationMinutes > 0) {
                            Text(
                                text = "${durationMinutes}m",
                                style = EinkTypography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Composable
private fun AllTimeSection(allTimeMinutes: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "All Time:",
            style = EinkTypography.titleLarge,
            color = EinkColors.OnBackground
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatMinutesDisplay(allTimeMinutes),
            style = EinkTypography.headlineMedium,
            color = EinkColors.OnBackground
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 2.dp,
        color = EinkColors.OnBackground
    )
}

private fun formatMinutesDisplay(minutes: Double): String {
    val totalMinutes = minutes.toLong()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

private fun formatTimeFromIso(isoDateTime: String): String {
    return try {
        if (isoDateTime.contains("T")) {
            isoDateTime.substringAfter("T").take(5)
        } else {
            isoDateTime.takeLast(8).take(5)
        }
    } catch (e: Exception) {
        ""
    }
}
