package com.ivanlee.sesh.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ivanlee.sesh.ui.theme.EinkColors

private const val SEGMENT_COUNT = 20

@Composable
fun EinkProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    isOverflow: Boolean = false
) {
    val effectiveColor = if (isOverflow) EinkColors.Overflow else color
    val filledSegments = if (isOverflow) SEGMENT_COUNT
        else (progress * SEGMENT_COUNT).toInt().coerceIn(0, SEGMENT_COUNT)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until SEGMENT_COUNT) {
            val isFilled = i < filledSegments
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .border(1.dp, EinkColors.OnBackground)
                    .then(
                        if (isFilled) {
                            Modifier
                                .background(effectiveColor)
                                .padding(1.dp)
                        } else {
                            Modifier
                                .background(EinkColors.Background)
                                .padding(1.dp)
                        }
                    )
            )
        }
    }
}
