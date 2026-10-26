package com.ahsen.hava.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.ui.common.durationText
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.WarmOrange
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.sin

/*
 * Gün doğumu → gün batımı yayı.
 */

/**
 * Gün doğumu → gün batımı yayı. Güneş, şehrin yerel saatine göre yayın üzerindeki
 * yerinde durur. Gece ise yay soluk kalır ve güneş ufkun altına iner.
 */
@Composable
internal fun SunArcCard(sunrise: LocalDateTime, sunset: LocalDateTime, now: LocalDateTime) {
    val total = Duration.between(sunrise, sunset).toMinutes().coerceAtLeast(1)
    val passed = Duration.between(sunrise, now).toMinutes()
    val progress = (passed.toFloat() / total).coerceIn(0f, 1f)
    val isDaytime = now.isAfter(sunrise) && now.isBefore(sunset)
    val daylight = Duration.ofMinutes(total)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.section_sun))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
        ) {
            val pad = 12.dp.toPx()
            val horizon = size.height - 8.dp.toPx()
            val left = pad
            val right = size.width - pad
            val radiusX = (right - left) / 2
            val radiusY = horizon - pad
            val centerX = left + radiusX

            // Ufuk çizgisi
            drawLine(Color(0x33FFFFFF), Offset(0f, horizon), Offset(size.width, horizon), strokeWidth = 1.dp.toPx())

            // Yayın tamamı: kesikli, soluk
            val arcTopLeft = Offset(left, horizon - radiusY)
            val arcSize = Size(radiusX * 2, radiusY * 2)
            drawArc(
                color = Color(0x40FFFFFF),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
                ),
            )
            // Geçen kısım: sarıdan turuncuya
            if (progress > 0f) {
                drawArc(
                    brush = Brush.horizontalGradient(listOf(HavaYellow, WarmOrange), startX = left, endX = right),
                    startAngle = 180f,
                    sweepAngle = 180f * progress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            // Güneşin yeri: yay üzerinde açıya göre
            val angle = Math.PI * (1 - progress)
            val sun = Offset(
                x = centerX + (radiusX * cos(angle)).toFloat(),
                y = horizon - (radiusY * sin(angle)).toFloat(),
            )
            if (isDaytime) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0x66FFC900), Color.Transparent), center = sun, radius = 22.dp.toPx()),
                    radius = 22.dp.toPx(),
                    center = sun,
                )
                drawCircle(HavaYellow, radius = 7.dp.toPx(), center = sun)
            } else {
                drawCircle(Color(0x66FFFFFF), radius = 5.dp.toPx(), center = Offset(sun.x, horizon))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            SunTime(label = stringResource(R.string.sunrise), time = sunrise.format(HOUR_FORMAT))
            Spacer(Modifier.weight(1f))
            SunTime(label = stringResource(R.string.sunset), time = sunset.format(HOUR_FORMAT), alignEnd = true)
        }
        Spacer(Modifier.height(8.dp))
        val remaining = Duration.between(now, sunset)
        Text(
            text = if (isDaytime) {
                stringResource(R.string.sunset_in, durationText(remaining.toMinutes().toInt()))
            } else {
                stringResource(R.string.daylight_length, durationText(daylight.toMinutes().toInt()))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SunTime(label: String, time: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
