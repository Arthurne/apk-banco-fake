package com.purplebank.simulator.ui.theme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object BankColors {
    val Purple = Color(0xFF820AD1)
    val DarkPurple = Color(0xFF4B0082)
    val LightBg = Color(0xFFF7F5F9)
    val DarkBg = Color(0xFF15121A)
    val Text = Color(0xFF242129)
    val In = Color(0xFF168A4A)
    val Out = Color(0xFFD64545)
    fun parse(hex: String?): Color = runCatching { Color(android.graphics.Color.parseColor(hex ?: "#820AD1")) }.getOrDefault(Purple)
}
@Composable
fun PurpleBankTheme(primary: Color, dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = lerp(BankColors.DarkBg, primary, 0.30f), onPrimaryContainer = Color.White,
        secondary = BankColors.DarkPurple, background = BankColors.DarkBg, onBackground = Color(0xFFF5F5F7),
        surface = Color(0xFF211D28), onSurface = Color(0xFFF5F5F7),
        surfaceVariant = Color(0xFF2B2633), onSurfaceVariant = Color(0xFFCAC4D0)
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = lerp(Color.White, primary, 0.14f), onPrimaryContainer = BankColors.DarkPurple,
        secondary = BankColors.DarkPurple, background = BankColors.LightBg, onBackground = BankColors.Text,
        surface = Color.White, onSurface = BankColors.Text,
        surfaceVariant = Color(0xFFECE8F0), onSurfaceVariant = Color(0xFF625B69)
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
            headlineSmall = TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        ),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)),
        content = content
    )
}
