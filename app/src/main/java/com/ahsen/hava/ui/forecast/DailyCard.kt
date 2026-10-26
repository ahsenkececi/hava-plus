package com.ahsen.hava.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.DailyForecast
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.component.WeatherIcon
import com.ahsen.hava.ui.theme.CoolBlue
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.WarmOrange
import java.time.LocalDate
import java.time.format.TextStyle

/*
 * Günlük tahmin: her gün için sıcaklık aralığı çubuğu.
 */

/** 7 günlük tahmin: her satırda o haftanın sıcaklık aralığına göre çizilmiş bir çubuk. */
@Composable
internal fun DailyCard(days: List<DailyForecast>) {
    if (days.isEmpty()) return
    val weekMin = days.minOf { it.minTemperatureC }
    val weekMax = days.maxOf { it.maxTemperatureC }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.section_daily))
        days.forEachIndexed { index, day ->
            DailyRow(day = day, dayLabel = dayLabel(day.date, index), weekMin = weekMin, weekMax = weekMax)
        }
    }
}

@Composable
private fun DailyRow(day: DailyForecast, dayLabel: String, weekMin: Double, weekMax: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dayLabel,
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Box(modifier = Modifier.width(40.dp)) {
            WeatherIcon(condition = day.condition, isDay = true, size = 26.dp)
        }
        Text(
            text = day.minTemperatureC.asDegree(),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        TemperatureBar(
            min = day.minTemperatureC,
            max = day.maxTemperatureC,
            weekMin = weekMin,
            weekMax = weekMax,
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
        )
        Text(
            text = day.maxTemperatureC.asDegree(),
            modifier = Modifier
                .width(44.dp)
                .padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
    }
}

/** Canvas ile çizilen sıcaklık aralığı: soğuk uç mavi, sıcak uç sarı-turuncu. */
@Composable
private fun TemperatureBar(min: Double, max: Double, weekMin: Double, weekMax: Double, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val span = (weekMax - weekMin).takeIf { it > 0 } ?: 1.0
        val start = ((min - weekMin) / span).toFloat() * size.width
        val end = ((max - weekMin) / span).toFloat() * size.width
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = Color(0x26FFFFFF), cornerRadius = radius)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(CoolBlue, HavaYellow, WarmOrange),
                startX = 0f,
                endX = size.width,
            ),
            topLeft = Offset(start, 0f),
            size = Size((end - start).coerceAtLeast(size.height), size.height),
            cornerRadius = radius,
        )
    }
}

@Composable
private fun dayLabel(date: LocalDate, index: Int): String = when (index) {
    0 -> stringResource(R.string.today)
    1 -> stringResource(R.string.tomorrow)
    else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, TURKISH)
}
