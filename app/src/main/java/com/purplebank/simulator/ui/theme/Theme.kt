package com.purplebank.simulator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

object BankColors {
    val Purple = Color(0xFF820AD1)
    val DarkPurple = Color(0xFF4B0082)
    val LightBg = Color(0xFFF5F5F7)
    val DarkBg = Color(0xFF17141C)
    val Text = Color(0xFF242129)
    val In = Color(0xFF1B9E4B)
    val Out = Color(0xFFD93025)

    fun parse(hex: String?): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex ?: "#820AD1")) }.getOrDefault(Purple)
}

@Composable
fun PurpleBankTheme(primary: Color, dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = lerp(BankColors.DarkBg, primary, 0.30f), onPrimaryContainer = Color.White,
        secondary = BankColors.DarkPurple,
        background = BankColors.DarkBg, onBackground = Color(0xFFF5F5F7),
        surface = Color(0xFF211D28), onSurface = Color(0xFFF5F5F7),
        surfaceVariant = Color(0xFF2B2633), onSurfaceVariant = Color(0xFFCAC4D0)
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = lerp(Color.White, primary, 0.14f), onPrimaryContainer = BankColors.DarkPurple,
        secondary = BankColors.DarkPurple,
        background = BankColors.LightBg, onBackground = BankColors.Text,
        surface = Color.White, onSurface = BankColors.Text,
        surfaceVariant = Color(0xFFECE8F0), onSurfaceVariant = Color(0xFF5E5966)
    )
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
