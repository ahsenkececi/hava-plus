package com.ahsen.hava.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ahsen.hava.domain.model.WeatherCondition
import com.ahsen.hava.ui.theme.CoolBlue
import com.ahsen.hava.ui.theme.HavaYellow

/**
 * İnce çizgili (outline) hava ikonu. Emoji yerine Canvas ile çizilir; her telefonda
 * aynı görünür. Beyaz çizgi + tek bir vurgu rengi: sade ve profesyonel.
 */
@Composable
fun WeatherIcon(condition: WeatherCondition, isDay: Boolean, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (condition) {
            WeatherCondition.CLEAR ->
                if (isDay) sun(Offset(s / 2, s / 2), s * 0.20f, stroke) else moon(Offset(s / 2, s / 2), s * 0.30f, stroke)

            WeatherCondition.MAINLY_CLEAR, WeatherCondition.PARTLY_CLOUDY -> {
                val cloud = cloudPath(left = 0.20f * s, top = 0.30f * s, width = 0.76f * s)
                // Güneş/ay bulutun arkasında kalsın: bulutun olduğu yer kesilir.
                clipPath(cloud, clipOp = ClipOp.Difference) {
                    val c = Offset(s * 0.36f, s * 0.36f)
                    if (isDay) sun(c, s * 0.15f, stroke) else moon(c, s * 0.22f, stroke)
                }
                drawPath(cloud, LINE, style = stroke)
            }

            WeatherCondition.OVERCAST, WeatherCondition.UNKNOWN -> {
                val front = cloudPath(left = 0.12f * s, top = 0.26f * s, width = 0.84f * s)
                clipPath(front, clipOp = ClipOp.Difference) {
                    drawPath(cloudPath(left = 0.02f * s, top = 0.10f * s, width = 0.60f * s), LINE_DIM, style = stroke)
                }
                drawPath(front, LINE, style = stroke)
            }

            WeatherCondition.FOG -> fog(s, stroke)

            WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> {
                drawPath(cloudPath(left = 0.08f * s, top = 0.06f * s, width = 0.84f * s), LINE, style = stroke)
                rain(s, stroke)
            }

            WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> {
                drawPath(cloudPath(left = 0.08f * s, top = 0.06f * s, width = 0.84f * s), LINE, style = stroke)
                snow(s)
            }

            WeatherCondition.THUNDERSTORM -> {
                drawPath(cloudPath(left = 0.08f * s, top = 0.04f * s, width = 0.84f * s), LINE, style = stroke)
                bolt(s)
            }
        }
    }
}

private val LINE = Color(0xF2FFFFFF)
private val LINE_DIM = Color(0x80FFFFFF)

private fun DrawScope.sun(c: Offset, r: Float, stroke: Stroke) {
    drawCircle(HavaYellow, radius = r, center = c, style = stroke)
    repeat(8) { i ->
        rotate(i * 45f, pivot = c) {
            drawLine(
                HavaYellow,
                Offset(c.x, c.y - r * 1.55f),
                Offset(c.x, c.y - r * 2.0f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.moon(c: Offset, r: Float, stroke: Stroke) {
    val full = Path().apply { addOval(Rect(c, r)) }
    val cut = Path().apply { addOval(Rect(Offset(c.x + r * 0.55f, c.y - r * 0.35f), r * 0.85f)) }
    drawPath(Path().apply { op(full, cut, PathOperation.Difference) }, HavaYellow, style = stroke)
}

/** Bulut silueti: yuvarlatılmış taban + üç daire, tek bir kenar çizgisinde birleşir (Union). */
private fun cloudPath(left: Float, top: Float, width: Float): Path {
    val w = width
    val base = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top + 0.34f * w,
                right = left + w,
                bottom = top + 0.62f * w,
                cornerRadius = CornerRadius(0.14f * w),
            ),
        )
    }
    fun circle(cx: Float, cy: Float, r: Float) = Path().apply {
        addOval(Rect(Offset(left + cx * w, top + cy * w), r * w))
    }
    var result = base
    listOf(circle(0.30f, 0.40f, 0.19f), circle(0.56f, 0.32f, 0.25f), circle(0.80f, 0.45f, 0.16f)).forEach { puff ->
        result = Path().apply { op(result, puff, PathOperation.Union) }
    }
    return result
}

private fun DrawScope.rain(s: Float, stroke: Stroke) {
    listOf(0.32f, 0.50f, 0.68f).forEachIndexed { i, x ->
        val y = s * (0.72f + if (i == 1) 0.05f else 0f)
        drawLine(CoolBlue, Offset(x * s, y), Offset(x * s - s * 0.05f, y + s * 0.14f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

private fun DrawScope.snow(s: Float) {
    listOf(0.32f to 0.78f, 0.50f to 0.88f, 0.68f to 0.78f).forEach { (x, y) ->
        drawCircle(Color.White, radius = s * 0.045f, center = Offset(x * s, y * s))
    }
}

private fun DrawScope.bolt(s: Float) {
    val p = Path().apply {
        moveTo(s * 0.54f, s * 0.60f)
        lineTo(s * 0.42f, s * 0.80f)
        lineTo(s * 0.54f, s * 0.80f)
        lineTo(s * 0.46f, s * 0.97f)
    }
    drawPath(p, HavaYellow, style = Stroke(width = s * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.fog(s: Float, stroke: Stroke) {
    listOf(0.32f to 0.70f, 0.50f to 0.80f, 0.68f to 0.60f).forEach { (y, w) ->
        val start = (1f - w) / 2f * s
        drawLine(LINE, Offset(start, y * s), Offset(start + w * s, y * s), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}
