package com.ivanlee.sesh.ui.theme

import androidx.compose.ui.graphics.Color

object EinkColors {
    val Background = Color.White
    val OnBackground = Color.Black
    val Surface = Color(0xFFF0F0F0)
    val OnSurface = Color.Black

    // Phase colors
    val Focus = Color(0xFF2E7D32)
    val Overflow = Color(0xFFFFA000)
    val Break = Color(0xFF1565C0)
    val Paused = Color(0xFF6A1B9A)
    val Abandoned = Color(0xFFC62828)

    // Category colors (saturated for e-ink)
    val Development = Color(0xFF1976D2)
    val Writing = Color(0xFFC62828)
    val Design = Color(0xFF7B1FA2)
    val Research = Color(0xFFF9A825)
    val Meeting = Color(0xFF00838F)
    val Exercise = Color(0xFF2E7D32)
    val Reading = Color(0xFFE65100)
    val Admin = Color(0xFF616161)

    // UI elements
    val ButtonBorder = Color.Black
    val Disabled = Color(0xFF9E9E9E)
}
