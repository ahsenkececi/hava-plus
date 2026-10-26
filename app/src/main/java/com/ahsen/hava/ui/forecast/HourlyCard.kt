package com.ahsen.hava.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.HourlyForecast
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.component.WeatherIcon
import com.ahsen.hava.ui.theme.CoolBlue
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.WarmOrange
import kotlin.math.roundToInt

/*
 * Saatlik tahmin kartı: saat + ikon, altında sıcaklık eğrisi, en altta yağış ihtimali çubukları.
 * Hepsi aynı sütun genişliğini kullanır; böylece eğrideki her nokta kendi saatinin tam altına düşer.
 */

/** Kartların ortak başlığı. */
@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = HavaYellow,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

private val ColumnWidth = 56.dp
private val CurveHeight = 84.dp
private val RainBarMaxHeight = 22.dp

/** Önümüzdeki 24 saat, yatay kaydırılır. */
@Composable
internal fun HourlyCard(hours: List<HourlyForecast>) {
    if (hours.isEmpty()) return
    val temperatures = remember(hours) { hours.map { it.temperatureC } }
    val chartDescription = stringResource(
        R.string.hourly_chart_cd,
        temperatures.max().roundToInt(),
        temperatures.min().roundToInt(),
    )
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.section_hourly))
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            // 1) Saat + ikon
            Row {
                hours.forEachIndexed { index, hour ->
                    val isNow = index == 0
                    Column(modifier = Modifier.width(ColumnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isNow) stringResource(R.string.now) else hour.time.format(HOUR_FORMAT),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                            color = if (isNow) HavaYellow else TextSecondary,
                        )
                        WeatherIcon(condition = hour.condition, isDay = hour.isDay, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            // 2) Sıcaklık eğrisi (değerler noktaların üstünde yazılı)
            TemperatureCurve(
                temperatures = temperatures,
                modifier = Modifier
                    .width(ColumnWidth * hours.size)
                    .height(CurveHeight)
                    .semantics { contentDescription = chartDescription },
            )
            // 3) Yağış ihtimali çubukları
            Row(verticalAlignment = Alignment.Bottom) {
                hours.forEach { hour ->
                    RainColumn(probability = hour.precipitationProbability ?: 0)
                }
            }
        }
    }
}

/**
 * Noktaları yumuşak bir eğriyle (kübik Bezier) birleştirir. Çizgi rengi sıcaklığa göre:
 * grafiğin üst kısmı (sıcak) turuncu, alt kısmı (serin) mavi. Altına hafif bir dolgu.
 */
@Composable
private fun TemperatureCurve(temperatures: List<Double>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val levels = remember(temperatures) { normalized(temperatures) }
    val labelStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    Canvas(modifier = modifier) {
        val column = size.width / temperatures.size
        val top = 26.dp.toPx() // sıcaklık yazısına yer
        val bottom = size.height - 8.dp.toPx()
        val points = levels.mapIndexed { i, level ->
            Offset(column * (i + 0.5f), bottom - level * (bottom - top))
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val cur = points[i]
                val midX = (prev.x + cur.x) / 2
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent), startY = top, endY = size.height))
        drawPath(
            line,
            Brush.verticalGradient(listOf(WarmOrange, HavaYellow, CoolBlue), startY = top, endY = bottom),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        points.forEachIndexed { i, p ->
            val isNow = i == 0
            drawCircle(if (isNow) HavaYellow else TextPrimary, radius = (if (isNow) 5f else 3.5f).dp.toPx(), center = p)
            val label = textMeasurer.measure("${temperatures[i].roundToInt()}°", labelStyle)
            drawText(label, topLeft = Offset(p.x - label.size.width / 2f, p.y - label.size.height - 6.dp.toPx()))
        }
    }
}

/** Bir saatin yağış ihtimali: yükseklik ihtimalle orantılı, %20 ve üstünde yüzde yazılır. */
@Composable
private fun RainColumn(probability: Int) {
    Column(modifier = Modifier.width(ColumnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(RainBarMaxHeight), contentAlignment = Alignment.BottomCenter) {
            if (probability > 0) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(RainBarMaxHeight * (probability / 100f))
                        .background(CoolBlue.copy(alpha = 0.35f + probability / 200f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (probability >= 20) stringResource(R.string.percent_value, probability) else " ",
            style = MaterialTheme.typography.labelSmall,
            color = CoolBlue,
        )
    }
}

/**
 * Değerleri 0..1 aralığına yayar (en düşük 0, en yüksek 1). Hepsi aynıysa eğri ortada düz durur.
 * Saf fonksiyon: Compose'a bağımlı değil, birim testi var.
 */
internal fun normalized(values: List<Double>): List<Float> {
    if (values.isEmpty()) return emptyList()
    val min = values.min()
    val span = values.max() - min
    if (span < 0.0001) return List(values.size) { 0.5f }
    return values.map { ((it - min) / span).toFloat() }
}
