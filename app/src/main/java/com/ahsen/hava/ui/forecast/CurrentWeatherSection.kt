package com.ahsen.hava.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.DewPointComfort
import com.ahsen.hava.domain.logic.UvLevel
import com.ahsen.hava.domain.logic.VisibilityLevel
import com.ahsen.hava.domain.logic.WeatherInsights
import com.ahsen.hava.domain.model.CurrentWeather
import com.ahsen.hava.domain.model.DailyForecast
import com.ahsen.hava.ui.common.label
import com.ahsen.hava.ui.common.shortLabel
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.component.WindCompass
import com.ahsen.hava.ui.theme.HavaNavy
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.WarmOrange
import kotlin.math.roundToInt

/*
 * Tahmin ekranının üst kısmı: büyük sıcaklık, detay kutucukları (nem, rüzgâr, UV) ve Zaman Makinesi girişi.
 */

/** Büyük sıcaklık. Hava görseli arka plandaki canlı sahnede. */
@Composable
internal fun CurrentHero(current: CurrentWeather, today: DailyForecast?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = current.temperatureC.asDegree(),
            fontSize = 104.sp,
            fontWeight = FontWeight.Thin,
            color = TextPrimary,
        )
        Text(
            text = current.condition.label(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = HavaYellow,
        )
        if (today != null) {
            Text(
                text = stringResource(
                    R.string.high_low,
                    today.maxTemperatureC.asDegree(),
                    today.minTemperatureC.asDegree(),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }
    }
}

/** Hissedilen, nem, UV ve pusulalı rüzgâr: dört küçük cam kart. */
@Composable
internal fun DetailsRow(current: CurrentWeather, uvIndex: Double?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryDetails(current = current, uvIndex = uvIndex)
        SecondaryDetails(current = current)
    }
}

/** İlk sıra: hissedilen, nem, UV, rüzgâr. */
@Composable
private fun PrimaryDetails(current: CurrentWeather, uvIndex: Double?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailTile(
            label = stringResource(R.string.detail_feels_like),
            value = current.apparentTemperatureC?.asDegree() ?: "–",
            modifier = Modifier.weight(1f),
        )
        DetailTile(
            label = stringResource(R.string.detail_humidity),
            value = current.humidityPercent?.let { stringResource(R.string.percent_value, it) } ?: "–",
            modifier = Modifier.weight(1f),
        )
        val uvLevel = uvIndex?.let { UvLevel.from(it) }
        DetailTile(
            label = stringResource(R.string.detail_uv),
            value = uvIndex?.roundToInt()?.toString() ?: "–",
            unit = uvLevel?.label(),
            unitColor = when (uvLevel) {
                UvLevel.High, UvLevel.VeryHigh, UvLevel.Extreme -> WarmOrange
                UvLevel.Moderate -> HavaYellow
                else -> TextSecondary
            },
            modifier = Modifier.weight(1f),
        )
        WindTile(current = current, modifier = Modifier.weight(1f))
    }
}

/** İkinci sıra: basınç, görüş, çiy noktası, bulut. Sayının altında anlamı da yazar ("Puslu", "Bunaltıcı"). */
@Composable
private fun SecondaryDetails(current: CurrentWeather) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailTile(
            label = stringResource(R.string.detail_pressure),
            value = current.pressureHpa?.roundToInt()?.toString() ?: "–",
            unit = stringResource(R.string.unit_hpa),
            modifier = Modifier.weight(1f),
        )
        val visibility = current.visibilityMeters
        DetailTile(
            label = stringResource(R.string.detail_visibility),
            value = visibility?.let { formatVisibility(it) } ?: "–",
            unit = visibility?.let { stringResource(visibilityLabel(VisibilityLevel.from(it))) },
            modifier = Modifier.weight(1f),
        )
        val dewPoint = current.dewPointC
        DetailTile(
            label = stringResource(R.string.detail_dew_point),
            value = dewPoint?.asDegree() ?: "–",
            unit = dewPoint?.let { stringResource(dewPointLabel(DewPointComfort.from(it))) },
            unitColor = if (dewPoint != null && DewPointComfort.from(dewPoint) == DewPointComfort.Oppressive) WarmOrange else TextSecondary,
            modifier = Modifier.weight(1f),
        )
        DetailTile(
            label = stringResource(R.string.detail_cloud),
            value = current.cloudCoverPercent?.let { stringResource(R.string.percent_value, it) } ?: "–",
            modifier = Modifier.weight(1f),
        )
    }
}

/** 24 140 m → "24 km", 2 400 m → "2,4 km" (ondalık ayracı cihazın diline göre). */
@Composable
private fun formatVisibility(meters: Double): String {
    val km = meters / 1000
    return if (km >= 10) stringResource(R.string.distance_km_whole, km.roundToInt()) else stringResource(R.string.distance_km, km)
}

private fun visibilityLabel(level: VisibilityLevel): Int = when (level) {
    VisibilityLevel.Fog -> R.string.visibility_fog
    VisibilityLevel.Haze -> R.string.visibility_haze
    VisibilityLevel.Good -> R.string.visibility_good
    VisibilityLevel.Excellent -> R.string.visibility_excellent
}

private fun dewPointLabel(comfort: DewPointComfort): Int = when (comfort) {
    DewPointComfort.Dry -> R.string.dew_dry
    DewPointComfort.Comfortable -> R.string.dew_comfortable
    DewPointComfort.Humid -> R.string.dew_humid
    DewPointComfort.Oppressive -> R.string.dew_oppressive
}

/** Rüzgâr kutusu: hız + yön + telefonun pusulasıyla dönen ok. */
@Composable
private fun WindTile(current: CurrentWeather, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.detail_wind),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val direction = current.windDirectionDeg
            if (direction != null) {
                WindCompass(fromDegrees = direction, size = 26.dp)
                Spacer(Modifier.width(6.dp))
            }
            Column {
                Text(
                    text = current.windSpeedKmh?.roundToInt()?.toString() ?: "–",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                )
                Text(
                    text = direction?.let { WeatherInsights.compassPoint(it).shortLabel() } ?: stringResource(R.string.unit_kmh_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DetailTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    unitColor: Color = TextSecondary,
) {
    GlassCard(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
        )
        if (unit != null) {
            Text(text = unit, style = MaterialTheme.typography.labelSmall, color = unitColor, maxLines = 1)
        }
    }
}

