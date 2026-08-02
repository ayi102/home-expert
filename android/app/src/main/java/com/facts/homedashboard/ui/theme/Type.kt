package com.facts.homedashboard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Slightly larger, bolder defaults — this is read from across a room.
val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 84.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
)
