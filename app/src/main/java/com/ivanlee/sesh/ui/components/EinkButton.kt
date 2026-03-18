package com.ivanlee.sesh.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.ivanlee.sesh.ui.theme.EinkColors

@Composable
fun EinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    backgroundColor: Color = EinkColors.OnBackground,
    contentColor: Color = EinkColors.Background
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
                disabledContainerColor = EinkColors.Disabled,
                disabledContentColor = EinkColors.Background
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            border = BorderStroke(2.dp, if (enabled) EinkColors.ButtonBorder else EinkColors.Disabled)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled,
            shape = RectangleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EinkColors.OnBackground,
                disabledContentColor = EinkColors.Disabled
            ),
            border = BorderStroke(2.dp, if (enabled) EinkColors.ButtonBorder else EinkColors.Disabled),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
