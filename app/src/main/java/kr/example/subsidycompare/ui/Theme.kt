package kr.example.subsidycompare.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kr.example.subsidycompare.data.Carrier
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F3A5F),
    secondary = Color(0xFF3A6EA5),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC3F0),
    secondary = Color(0xFF7FA9D6),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/**
 * 통신사 구분용 색. 상표 문제를 피하려고 각 통신사의 공식 브랜드 색은 쓰지 않았습니다.
 */
fun Carrier?.chipColor(): Color = when (this) {
    Carrier.SKT -> Color(0xFF4C6EF5)
    Carrier.KT -> Color(0xFF12B886)
    Carrier.LGU -> Color(0xFFF08C00)
    null -> Color.Gray
}

private val wonFormat: NumberFormat = NumberFormat.getInstance(Locale.KOREA)
fun Long.won(): String = wonFormat.format(this) + "원"
fun Int.won(): String = toLong().won()

fun formatUpdatedAt(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
}.getOrDefault(iso)
