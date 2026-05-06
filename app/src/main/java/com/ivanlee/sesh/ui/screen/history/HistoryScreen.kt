package com.ivanlee.sesh.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.ivanlee.sesh.data.db.dao.SessionWithCategory
import com.ivanlee.sesh.data.db.entity.CategoryEntity
import com.ivanlee.sesh.ui.FormatUtils
import com.ivanlee.sesh.ui.components.EinkButton
import com.ivanlee.sesh.ui.theme.EinkColors
import com.ivanlee.sesh.ui.theme.EinkTypography

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val groups by viewModel.groupedSessions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val editState by viewModel.editState.collectAsState()

    editState?.let { state ->
        EditSessionDialog(
            state = state,
            categories = categories,
            onTitleChange = viewModel::updateEditTitle,
            onNotesChange = viewModel::updateEditNotes,
            onCategoryChange = viewModel::updateEditCategory,
            onDismiss = viewModel::cancelEditing,
            onSave = viewModel::saveEdit
        )
    }

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
                        SessionCard(
                            session = session,
                            onEdit = { viewModel.startEditing(session) }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionWithCategory,
    onEdit: () -> Unit
) {
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
    val canEdit = session.sessionType == "full_focus" || session.sessionType == "partial_focus"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, EinkColors.OnBackground, RectangleShape)
            .background(EinkColors.Background)
            .padding(12.dp)
    ) {
        // Top row: time + intention + edit action
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
            if (canEdit) {
                Text(
                    text = "EDIT",
                    style = EinkTypography.bodyMedium,
                    color = EinkColors.OnBackground,
                    modifier = Modifier
                        .border(1.dp, EinkColors.OnBackground, RectangleShape)
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
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

        if (!session.notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = session.notes,
                style = EinkTypography.bodyMedium,
                color = EinkColors.Disabled,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun EditSessionDialog(
    state: EditSessionUiState,
    categories: List<CategoryEntity>,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val selectedCategory = categories.firstOrNull { it.id == state.selectedCategoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = EinkColors.Background,
        titleContentColor = EinkColors.OnBackground,
        textContentColor = EinkColors.OnBackground,
        title = {
            Text(
                text = "Edit focus details",
                style = EinkTypography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "Intention",
                    style = EinkTypography.bodyMedium,
                    color = EinkColors.Disabled
                )
                TextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    placeholder = { Text("Focus Session") },
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

                Text(
                    text = "Category",
                    style = EinkTypography.bodyMedium,
                    color = EinkColors.Disabled
                )
                CategoryPicker(
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategoryChange = onCategoryChange
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Notes",
                    style = EinkTypography.bodyMedium,
                    color = EinkColors.Disabled
                )
                TextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Optional notes...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .border(2.dp, EinkColors.OnBackground),
                    shape = RectangleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = EinkColors.Background,
                        unfocusedContainerColor = EinkColors.Background,
                        focusedIndicatorColor = EinkColors.OnBackground,
                        unfocusedIndicatorColor = EinkColors.OnBackground
                    )
                )
            }
        },
        confirmButton = {
            EinkButton(
                text = "SAVE",
                onClick = onSave,
                modifier = Modifier.width(120.dp),
                backgroundColor = EinkColors.OnBackground,
                contentColor = EinkColors.Background
            )
        },
        dismissButton = {
            EinkButton(
                text = "CANCEL",
                onClick = onDismiss,
                modifier = Modifier.width(120.dp),
                filled = false
            )
        }
    )
}

@Composable
private fun CategoryPicker(
    selectedCategory: CategoryEntity?,
    categories: List<CategoryEntity>,
    onCategoryChange: (String?) -> Unit
) {
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
                style = MaterialTheme.typography.bodyLarge,
                color = EinkColors.OnBackground
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("No category") },
                onClick = {
                    onCategoryChange(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.title) },
                    onClick = {
                        onCategoryChange(category.id)
                        expanded = false
                    }
                )
            }
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
