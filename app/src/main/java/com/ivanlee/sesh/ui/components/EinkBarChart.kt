package com.ivanlee.sesh.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivanlee.sesh.ui.theme.EinkColors
import com.ivanlee.sesh.ui.theme.EinkTypography

/**
 * E-ink optimized vertical bar chart.
 * Solid black bars, sharp corners, no animations.
 *
 * @param data List of (label, value) pairs (e.g., day abbreviation + hours)
 * @param maxValue Maximum value for scaling bars. If 0, auto-calculated from data.
 */
@Composable
fun EinkBarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    maxValue: Float = 0f,
    barColor: Color = EinkColors.OnBackground,
    maxBarHeight: Int = 120
) {
    val effectiveMax = if (maxValue > 0f) maxValue
        else data.maxOfOrNull { it.second }?.coerceAtLeast(1f) ?: 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Value label above bar
                Text(
                    text = if (value >= 1f) "%.0fh".format(value)
                        else if (value > 0f) "%.0fm".format(value * 60)
                        else "",
                    style = EinkTypography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = EinkColors.OnBackground
                )

                // Bar
                val barHeight = if (effectiveMax > 0f)
                    (value / effectiveMax * maxBarHeight).toInt().coerceAtLeast(if (value > 0f) 4 else 0)
                else 0

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .width(28.dp)
                        .height(barHeight.dp)
                        .then(
                            if (barHeight > 0) Modifier
                                .background(barColor, RectangleShape)
                                .border(1.dp, EinkColors.OnBackground, RectangleShape)
                            else Modifier
                                .background(EinkColors.Surface, RectangleShape)
                                .border(1.dp, EinkColors.Disabled, RectangleShape)
                                .height(4.dp)
                        )
                )

                // Day label below bar
                Text(
                    text = label,
                    style = EinkTypography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = EinkColors.OnBackground
                )
            }
        }
    }
}
