package com.ivanlee.sesh.ui.screen.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.ivanlee.sesh.data.db.entity.CategoryEntity
import com.ivanlee.sesh.domain.model.BreakType
import com.ivanlee.sesh.domain.model.TimerPhase
import com.ivanlee.sesh.ui.components.CircularTimer
import com.ivanlee.sesh.ui.components.EinkButton
import com.ivanlee.sesh.ui.theme.EinkColors

@Composable
fun IdleContent(
    intention: String,
    onIntentionChange: (String) -> Unit,
    selectedCategory: CategoryEntity?,
    categories: List<CategoryEntity>,
    onCategorySelect: (CategoryEntity?) -> Unit,
    targetMinutes: Int,
    onAdjustDuration: (Int) -> Unit,
    onStartFocus: () -> Unit,
    onStartBreak: (BreakType) -> Unit,
    todayMinutes: Double,
    todaySessionCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Circular timer showing target duration (static, progress=0)
        CircularTimer(
            timeMs = targetMinutes * 60 * 1000L,
            phase = TimerPhase.Focus,
            progress = 0f,
            isOverflow = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Duration adjust buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            EinkButton(
                text = "-5",
                onClick = { onAdjustDuration(-5) },
                modifier = Modifier.width(64.dp),
                filled = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            EinkButton(
                text = "-1",
                onClick = { onAdjustDuration(-1) },
                modifier = Modifier.width(64.dp),
                filled = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            EinkButton(
                text = "+1",
                onClick = { onAdjustDuration(1) },
                modifier = Modifier.width(64.dp),
                filled = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            EinkButton(
                text = "+5",
                onClick = { onAdjustDuration(5) },
                modifier = Modifier.width(64.dp),
                filled = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Intention text field
        TextField(
            value = intention,
            onValueChange = onIntentionChange,
            placeholder = { Text("Set intention...") },
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, EinkColors.OnBackground),
            shape = RectangleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = EinkColors.Background,
                unfocusedContainerColor = EinkColors.Background,
                focusedIndicatorColor = EinkColors.OnBackground,
                unfocusedIndicatorColor = EinkColors.OnBackground
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category dropdown
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(2.dp, EinkColors.OnBackground)
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = selectedCategory?.title ?: "No category",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No category") },
                    onClick = {
                        onCategorySelect(null)
                        expanded = false
                    }
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.title) },
                        onClick = {
                            onCategorySelect(category)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start focus button
        EinkButton(
            text = "START FOCUS",
            onClick = onStartFocus,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = EinkColors.Focus,
            contentColor = EinkColors.Background
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Break buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EinkButton(
                text = "Short Break",
                onClick = { onStartBreak(BreakType.Short) },
                modifier = Modifier.weight(1f),
                filled = false
            )
            EinkButton(
                text = "Long Break",
                onClick = { onStartBreak(BreakType.Long) },
                modifier = Modifier.weight(1f),
                filled = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today stats
        val hours = (todayMinutes / 60).toInt()
        val mins = (todayMinutes % 60).toInt()
        val timeStr = if (hours > 0) "${hours}h${mins}m" else "${mins}m"
        Text(
            text = "Today: $timeStr  $todaySessionCount sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = EinkColors.Disabled
        )
    }
}
