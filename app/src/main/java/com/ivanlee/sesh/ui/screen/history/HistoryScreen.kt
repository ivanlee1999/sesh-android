package com.ivanlee.sesh.ui.screen.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ivanlee.sesh.ui.FormatUtils
import com.ivanlee.sesh.ui.theme.EinkColors
import com.ivanlee.sesh.ui.theme.EinkTypography

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val groups by viewModel.groupedSessions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EinkColors.Background)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "History",
            style = EinkTypography.headlineLarge,
            color = EinkColors.OnBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No sessions yet",
                    style = EinkTypography.bodyLarge,
                    color = EinkColors.Disabled
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groups.forEach { group ->
                    item {
                        Text(
                            text = group.label,
                            style = EinkTypography.titleLarge,
                            color = EinkColors.OnBackground,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(group.sessions, key = { it.id }) { session ->
                        SessionCard(session)
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionWithCategory) {
    val categoryColor = try {
        Color(session.categoryColor?.toColorInt() ?: 0xFFABB2BF.toInt())
    } catch (e: Exception) {
        EinkColors.Disabled
    }

    val time = formatStartTime(session.startedAt)
    val duration = FormatUtils.formatDurationSeconds(session.actualSeconds - session.pauseSeconds)
    val typeLabel = when (session.sessionType) {
        "full_focus" -> "full"
        "partial_focus" -> "partial"
        "rest" -> "rest"
        "abandoned" -> "abandoned"
        else -> session.sessionType
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, EinkColors.OnBackground, RectangleShape)
            .background(EinkColors.Background)
            .padding(12.dp)
    ) {
        // Top row: time + intention
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = time,
                style = EinkTypography.bodyLarge,
                color = EinkColors.OnBackground
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = session.title.ifEmpty {
                    if (session.sessionType == "rest") "Break" else "Focus Session"
                },
                style = EinkTypography.bodyLarge,
                color = EinkColors.OnBackground,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom row: category dot + name + duration + type
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (session.sessionType != "rest") {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(categoryColor, RectangleShape)
                        .border(1.dp, EinkColors.OnBackground, RectangleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = session.categoryTitle ?: "Uncategorized",
                    style = EinkTypography.bodyMedium,
                    color = EinkColors.OnBackground
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = duration,
                style = EinkTypography.bodyMedium,
                color = EinkColors.OnBackground
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = typeLabel,
                style = EinkTypography.bodyMedium,
                color = EinkColors.Disabled
            )
        }
    }
}

private fun formatStartTime(isoDateTime: String): String {
    return try {
        // Extract HH:MM from ISO 8601 datetime
        val timePart = if (isoDateTime.contains("T")) {
            isoDateTime.substringAfter("T").take(5)
        } else {
            isoDateTime.takeLast(8).take(5)
        }
        timePart
    } catch (e: Exception) {
        ""
    }
}

