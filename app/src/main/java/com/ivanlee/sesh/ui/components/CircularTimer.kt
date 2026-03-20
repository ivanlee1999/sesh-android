package com.ivanlee.sesh.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivanlee.sesh.domain.model.TimerPhase
import com.ivanlee.sesh.service.TimerService
import com.ivanlee.sesh.ui.theme.EinkColors

@Composable
fun CircularTimer(
    timeMs: Long,
    phase: TimerPhase,
    progress: Float,
    isOverflow: Boolean,
    modifier: Modifier = Modifier
) {
    val phaseColor = phaseColor(phase)
    val timeText = TimerService.formatTime(timeMs)
    val displayText = if (isOverflow) "+$timeText" else timeText

    Box(
        modifier = modifier
            .fillMaxWidth(0.6f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize().padding(8.dp)) {
            val strokeWidth = 12.dp.toPx()
            val halfStroke = strokeWidth / 2f
            val arcSize = size.minDimension - strokeWidth

            // Background ring
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize)
            )

            // Progress arc
            val sweep = if (isOverflow) {
                // Pulse between 0 and 360 for overflow — just fill completely
                360f
            } else {
                (progress * 360f).coerceIn(0f, 360f)
            }

            if (sweep > 0f) {
                drawArc(
                    color = phaseColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke),
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize)
                )
            }
        }

        Text(
            text = displayText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            ),
            color = phaseColor
        )
    }
}
