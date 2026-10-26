package com.ahsen.hava.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.ahsen.hava.ui.theme.CoolBlue
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.Sky
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import java.time.format.DateTimeFormatter

/*
 * Widget'lar Compose Canvas'ı desteklemez (RemoteViews ile çizilir). Gradyan arka plan ve
 * sıcaklık eğrisi bu yüzden Android Canvas ile bir Bitmap'e çizilip resim olarak verilir.
 * Renkler uygulamanın temasından gelir: widget ile uygulama aynı dili konuşur.
 */

private val HOUR: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")

/** Uygulamadaki gökyüzü gradyanı, yuvarlak köşeli. */
internal fun skyBitmap(sky: Sky, widthPx: Int, heightPx: Int, density: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
    val radius = 24 * density
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, heightPx.toFloat(),
            sky.colors.map { it.toArgb() }.toIntArray(),
            null,
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRoundRect(rect, radius, radius, fill)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
        color = GlassBorder.toArgb()
    }
    val half = density / 2
    canvas.drawRoundRect(RectF(half, half, widthPx - half, heightPx - half), radius, radius, border)
    return bitmap
}

/**
 * Saatlik sıcaklık eğrisi: sarı yumuşak çizgi, üstünde dereceler, altında yağmur ihtimali
 * çubukları ve saatler. Uygulamadaki saatlik kartın küçük hali.
 */
internal fun hourlyCurveBitmap(hours: List<WidgetHour>, nowLabel: String, widthPx: Int, heightPx: Int, density: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    if (hours.size < 2) return bitmap
    val canvas = Canvas(bitmap)

    val padX = 14 * density
    val labelSize = 11 * density
    val top = labelSize + 8 * density // sıcaklık etiketlerine yer
    val hourRowTop = heightPx - labelSize - 2 * density
    val barMax = 8 * density
    val bottom = hourRowTop - barMax - 8 * density
    val step = (widthPx - 2 * padX) / (hours.size - 1)

    val min = hours.minOf { it.temperatureC }
    val max = hours.maxOf { it.temperatureC }
    val span = (max - min).takeIf { it > 0 }?.toFloat()
    val points = hours.mapIndexed { i, h ->
        val ratio = span?.let { (h.temperatureC - min) / it } ?: 0.5f
        padX + i * step to bottom - ratio * (bottom - top)
    }

    // Eğri: komşu noktalar arasında yatay teğetli kübik Bezier (uygulamadaki eğriyle aynı yöntem).
    val path = Path().apply {
        moveTo(points[0].first, points[0].second)
        for (i in 1 until points.size) {
            val (x0, y0) = points[i - 1]
            val (x1, y1) = points[i]
            val mid = (x0 + x1) / 2
            cubicTo(mid, y0, mid, y1, x1, y1)
        }
    }
    canvas.drawPath(
        path,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            strokeCap = Paint.Cap.ROUND
            color = HavaYellow.toArgb()
        },
    )

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = labelSize
        textAlign = Paint.Align.CENTER
    }
    val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CoolBlue.toArgb() }
    hours.forEachIndexed { i, hour ->
        val (x, y) = points[i]
        text.color = TextPrimary.toArgb()
        text.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("${hour.temperatureC}°", x, y - 6 * density, text)

        if (hour.precipitationProbability >= RAIN_BAR_MIN_PROBABILITY) {
            val h = 2 * density + barMax * hour.precipitationProbability / 100f
            val barBottom = hourRowTop - 6 * density
            canvas.drawRoundRect(RectF(x - 4 * density, barBottom - h, x + 4 * density, barBottom), 2 * density, 2 * density, bar)
        }

        text.color = TextSecondary.toArgb()
        text.typeface = Typeface.DEFAULT
        canvas.drawText(if (i == 0) nowLabel else hour.time.format(HOUR), x, heightPx - 2 * density, text)
    }
    return bitmap
}

/** Bu ihtimalin altındaki yağmur çubuk olarak gösterilmez (gürültü olmasın). */
private const val RAIN_BAR_MIN_PROBABILITY = 20
