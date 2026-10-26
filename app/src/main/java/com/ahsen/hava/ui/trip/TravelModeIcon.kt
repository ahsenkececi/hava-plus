package com.ahsen.hava.ui.trip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ahsen.hava.domain.model.TravelMode

/**
 * Ulaşım ikonları: hava ikonlarıyla aynı ince çizgi dili.
 * (Material'ın geniş ikon paketi derleme süresini uzattığı için kendimiz çiziyoruz.)
 */
@Composable
fun TravelModeIcon(mode: TravelMode, modifier: Modifier = Modifier, size: Dp = 26.dp, color: Color = Color.White) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), strokeWidth = stroke.width, cap = StrokeCap.Round)

        when (mode) {
            TravelMode.Walk -> {
                drawCircle(color, radius = s * 0.09f, center = Offset(s * 0.52f, s * 0.14f))
                line(0.50f, 0.28f, 0.44f, 0.58f) // gövde
                line(0.44f, 0.58f, 0.30f, 0.90f) // arka bacak
                line(0.44f, 0.58f, 0.60f, 0.74f) // ön bacak
                line(0.60f, 0.74f, 0.62f, 0.92f)
                line(0.48f, 0.36f, 0.30f, 0.48f) // kollar
                line(0.48f, 0.36f, 0.66f, 0.46f)
            }
            TravelMode.Bike -> {
                drawCircle(color, radius = s * 0.18f, center = Offset(s * 0.24f, s * 0.68f), style = stroke)
                drawCircle(color, radius = s * 0.18f, center = Offset(s * 0.76f, s * 0.68f), style = stroke)
                val frame = Path().apply {
                    moveTo(s * 0.24f, s * 0.68f)
                    lineTo(s * 0.42f, s * 0.40f)
                    lineTo(s * 0.64f, s * 0.40f)
                    lineTo(s * 0.50f, s * 0.68f)
                    close()
                }
                drawPath(frame, color, style = stroke)
                line(0.64f, 0.40f, 0.76f, 0.68f) // ön çatal
                line(0.60f, 0.28f, 0.70f, 0.28f) // gidon
                line(0.64f, 0.28f, 0.64f, 0.40f)
            }
            TravelMode.Car -> {
                val cabin = Path().apply {
                    moveTo(s * 0.26f, s * 0.48f)
                    lineTo(s * 0.36f, s * 0.28f)
                    lineTo(s * 0.64f, s * 0.28f)
                    lineTo(s * 0.74f, s * 0.48f)
                }
                drawPath(cabin, color, style = stroke)
                drawRoundRect(
                    color,
                    topLeft = Offset(s * 0.10f, s * 0.48f),
                    size = Size(s * 0.80f, s * 0.22f),
                    cornerRadius = CornerRadius(s * 0.08f),
                    style = stroke,
                )
                drawCircle(color, radius = s * 0.07f, center = Offset(s * 0.28f, s * 0.76f))
                drawCircle(color, radius = s * 0.07f, center = Offset(s * 0.72f, s * 0.76f))
            }
            TravelMode.Transit -> {
                drawRoundRect(
                    color,
                    topLeft = Offset(s * 0.18f, s * 0.10f),
                    size = Size(s * 0.64f, s * 0.66f),
                    cornerRadius = CornerRadius(s * 0.12f),
                    style = stroke,
                )
                line(0.18f, 0.46f, 0.82f, 0.46f) // cam alt çizgisi
                drawCircle(color, radius = s * 0.05f, center = Offset(s * 0.32f, s * 0.60f))
                drawCircle(color, radius = s * 0.05f, center = Offset(s * 0.68f, s * 0.60f))
                line(0.30f, 0.78f, 0.24f, 0.90f) // tekerlekler/ayaklar
                line(0.70f, 0.78f, 0.76f, 0.90f)
            }
        }
    }
}
