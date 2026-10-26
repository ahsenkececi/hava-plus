package com.ahsen.hava.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.RainIntensity
import com.ahsen.hava.domain.model.PrecipitationSlot
import com.ahsen.hava.domain.model.WeatherCondition
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.component.WeatherIcon
import com.ahsen.hava.ui.theme.CoolBlue
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary

/**
 * "Yağmur 40 dk sonra başlıyor" kartı. Sadece yağmur yaklaşırken görünür.
 * Önümüzdeki 2 saatte gerçekten yağış varsa altında 15 dakikalık dilimlerle bir çubuk grafik çizilir:
 * çubuğun boyu yağış miktarı, rengi şiddeti (hafif / orta / kuvvetli).
 */
@Composable
fun RainBanner(message: String, slots: List<PrecipitationSlot>, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth(), borderColor = CoolBlue.copy(alpha = 0.6f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(condition = WeatherCondition.RAIN, isDay = true, size = 30.dp)
            Text(
                text = message,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }
        val intensities = slots.map { RainIntensity.from(it.precipitationMm) }
        if (intensities.any { it != RainIntensity.None }) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.next_two_hours),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            RainBars(slots = slots, intensities = intensities)
            Spacer(Modifier.height(6.dp))
            TimeAxis()
            Spacer(Modifier.height(10.dp))
            val peak = intensities.max()
            Text(
                text = stringResource(R.string.rain_peak, stringResource(intensityLabel(peak))),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun RainBars(slots: List<PrecipitationSlot>, intensities: List<RainIntensity>) {
    // Ölçek en az 1 mm: hafif çiseleme grafikte kuvvetli yağmur gibi görünmesin.
    val scale = maxOf(1.0, slots.maxOf { it.precipitationMm })
    val description = stringResource(R.string.rain_chart_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        slots.forEachIndexed { i, slot ->
            // Her dilimin silik bir izi var; yağış varsa üstüne dolu çubuk.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val fraction = (slot.precipitationMm / scale).toFloat().coerceIn(0f, 1f)
                if (intensities[i] != RainIntensity.None) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.08f))
                            .background(intensityColor(intensities[i]), RoundedCornerShape(6.dp)),
                    )
                }
            }
        }
    }
}

/** 8 dilimin altında 5 etiket: Şimdi, 30 dk, 1 sa, 1,5 sa, 2 sa. */
@Composable
private fun TimeAxis() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(R.string.now, R.string.axis_30_min, R.string.axis_1_hour, R.string.axis_90_min, R.string.axis_2_hours)
            .forEach { label ->
                Text(text = stringResource(label), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
    }
}

private fun intensityColor(intensity: RainIntensity): Color = when (intensity) {
    RainIntensity.None -> Color.Transparent
    RainIntensity.Light -> CoolBlue.copy(alpha = 0.55f)
    RainIntensity.Moderate -> CoolBlue
    RainIntensity.Heavy -> Color(0xFF4DA3FF)
}

private fun intensityLabel(intensity: RainIntensity): Int = when (intensity) {
    RainIntensity.None, RainIntensity.Light -> R.string.rain_light
    RainIntensity.Moderate -> R.string.rain_moderate
    RainIntensity.Heavy -> R.string.rain_heavy
}
