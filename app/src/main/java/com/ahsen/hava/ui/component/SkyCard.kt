package com.ahsen.hava.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.ahsen.hava.ui.theme.CoolBlue
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.Sky

/**
 * O yerin gökyüzünü taşıyan kart: arka plan havaya göre renk alır, üstünde silik bir süs çizilir
 * (güneş ışıltısı, bulut, yağmur çizgileri, yıldız). Şehir listesinin "Apple Hava" hissi buradan gelir.
 *
 * Tam ekran [com.ahsen.hava.ui.theme.SkyBackground] ile aynı [Sky] durumlarını kullanır, ama kartta
 * daha canlı, çapraz bir gradyan: koyu ekran zemininde kartlar birbirinden ayrışsın.
 */
@Composable
fun SkyCard(
    sky: Sky,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(cardColors(sky)))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) { drawDecoration(sky) }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), content = content)
    }
}

/** Kart gradyanı: sol üstten sağ alta. */
private fun cardColors(sky: Sky): List<Color> = when (sky) {
    Sky.ClearDay -> listOf(Color(0xFF2A63B7), Color(0xFF4F8AD6))
    // Gece kartları ekranın lacivert zemininden bir ton açık: kart zemine karışmasın.
    Sky.ClearNight -> listOf(Color(0xFF13295A), Color(0xFF23437F))
    Sky.Cloudy -> listOf(Color(0xFF55698F), Color(0xFF8C9DBD))
    Sky.CloudyNight -> listOf(Color(0xFF2A3550), Color(0xFF48567A))
    Sky.Rainy -> listOf(Color(0xFF1F2E45), Color(0xFF3B5170))
    Sky.Storm -> listOf(Color(0xFF15152C), Color(0xFF38315C))
    Sky.Snow -> listOf(Color(0xFF41587A), Color(0xFF7D93B5))
}

/** Süsler kartın boyutuna oranla çizilir; her kart boyunda aynı görünür. Konumlar sabit: kart titremesin. */
private fun DrawScope.drawDecoration(sky: Sky) {
    val w = size.width
    val h = size.height
    when (sky) {
        Sky.ClearDay -> {
            // Sağ üst köşeden taşan güneş ışıltısı
            drawCircle(HavaYellow.copy(alpha = 0.14f), radius = h * 0.75f, center = Offset(w, 0f))
            drawCircle(HavaYellow.copy(alpha = 0.12f), radius = h * 0.42f, center = Offset(w, 0f))
        }
        Sky.Cloudy, Sky.CloudyNight -> {
            val cloud = Color.White.copy(alpha = if (sky == Sky.CloudyNight) 0.06f else 0.10f)
            drawOval(cloud, topLeft = Offset(w * 0.53f, h * 0.06f), size = Size(w * 0.39f, h * 0.34f))
            drawOval(cloud, topLeft = Offset(w * 0.66f, h * 0.15f), size = Size(w * 0.33f, h * 0.30f))
            drawOval(cloud, topLeft = Offset(-w * 0.05f, h * 0.72f), size = Size(w * 0.50f, h * 0.40f))
        }
        Sky.Rainy, Sky.Storm -> {
            val drop = CoolBlue.copy(alpha = if (sky == Sky.Storm) 0.22f else 0.28f)
            RAIN.forEach { (x, y) ->
                drawLine(
                    color = drop,
                    start = Offset(w * x, h * y),
                    end = Offset(w * x - w * 0.022f, h * y + h * 0.14f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            if (sky == Sky.Storm) {
                drawCircle(Color(0xFFB9A8FF).copy(alpha = 0.10f), radius = h * 0.6f, center = Offset(w * 0.85f, h * 0.1f))
            }
        }
        Sky.ClearNight -> STARS.forEach { (x, y, r) ->
            drawCircle(Color.White.copy(alpha = 0.6f), radius = r.dp.toPx(), center = Offset(w * x, h * y))
        }
        Sky.Snow -> STARS.forEach { (x, y, r) ->
            drawCircle(Color.White.copy(alpha = 0.35f), radius = (r + 1.2f).dp.toPx(), center = Offset(w * x, h * y))
        }
    }
}

private val RAIN = listOf(
    0.11f to 0.08f, 0.25f to 0.31f, 0.42f to 0.04f, 0.58f to 0.38f, 0.75f to 0.12f, 0.92f to 0.35f,
    0.17f to 0.65f, 0.33f to 0.77f, 0.53f to 0.69f, 0.69f to 0.77f, 0.86f to 0.73f,
)

private val STARS = listOf(
    Triple(0.11f, 0.15f, 1.4f), Triple(0.33f, 0.31f, 1f), Triple(0.55f, 0.12f, 1.6f), Triple(0.70f, 0.55f, 1f),
    Triple(0.78f, 0.27f, 1.1f), Triple(0.92f, 0.09f, 1.3f), Triple(0.44f, 0.62f, 1f), Triple(0.62f, 0.85f, 1.2f),
)
