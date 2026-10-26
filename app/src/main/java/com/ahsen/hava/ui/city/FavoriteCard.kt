package com.ahsen.hava.ui.city

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.CurrentWeather
import com.ahsen.hava.domain.model.PopularCities
import com.ahsen.hava.domain.model.WeatherSnapshot
import com.ahsen.hava.ui.common.label
import com.ahsen.hava.ui.component.SkyCard
import com.ahsen.hava.ui.theme.HavaTheme
import com.ahsen.hava.ui.theme.Sky
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.rememberTimeOfDaySky
import com.ahsen.hava.ui.theme.skyFor
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Bir favori şehir: kendi gökyüzünün renginde, büyük sıcaklık, hissedilen ve günün en yüksek/en düşüğü.
 * Hava henüz gelmediyse kart günün saatine göre renk alır ve sıcaklık yerine "–" yazar.
 */
@Composable
fun FavoriteCard(city: City, snapshot: WeatherSnapshot?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val current = snapshot?.current
    CityWeatherCard(
        sky = current?.let { skyFor(it.condition, it.isDay) } ?: rememberTimeOfDaySky(),
        title = city.name,
        subtitle = current?.condition?.label() ?: city.subtitle,
        temperature = current?.let { "${it.temperatureC.roundToInt()}°" } ?: "–",
        note = current?.let { feelsLikeText(it) },
        range = snapshot?.let { rangeText(it.todayMinC, it.todayMaxC) },
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * Konum kartı ve favori kartlarının ortak iskeleti:
 * üstte isim + alt yazı ve büyük sıcaklık, altta bir not (hissedilen ya da yağmur cümlesi) ve Y/D.
 */
@Composable
internal fun CityWeatherCard(
    sky: Sky,
    title: String,
    subtitle: String,
    temperature: String,
    note: String?,
    range: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMyLocation: Boolean = false,
    noteIsAlert: Boolean = false,
) {
    SkyCard(sky = sky, onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMyLocation) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = stringResource(R.string.cd_my_location),
                            tint = TextPrimary,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(18.dp),
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CardSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(text = temperature, fontSize = 48.sp, fontWeight = FontWeight.ExtraLight, color = TextPrimary, lineHeight = 48.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = note.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (noteIsAlert) FontWeight.SemiBold else FontWeight.Normal,
                color = if (noteIsAlert) TextPrimary else CardSecondary,
                maxLines = 2,
            )
            if (range != null) {
                Text(text = range, style = MaterialTheme.typography.bodyMedium, color = CardSecondary)
            }
        }
    }
}

/** Renkli kart üzerinde okunur kalan ikincil yazı rengi (%80 beyaz). */
private val CardSecondary = Color(0xCCFFFFFF)

@Composable
internal fun feelsLikeText(current: CurrentWeather): String =
    stringResource(R.string.feels_like_value, (current.apparentTemperatureC ?: current.temperatureC).roundToInt())

@Composable
internal fun rangeText(min: Double?, max: Double?): String? =
    if (min == null || max == null) null else stringResource(R.string.high_low_short, max.roundToInt(), min.roundToInt())

@Preview
@Composable
private fun FavoriteCardPreview() {
    val now = LocalDateTime.of(2026, 10, 25, 14, 0)
    HavaTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FavoriteCard(PopularCities.all[0], WeatherSnapshot(CurrentWeather(14.0, 3, true, now), 10.0, 16.0), onClick = {})
            FavoriteCard(PopularCities.all[8], WeatherSnapshot(CurrentWeather(11.0, 81, true, now), 8.0, 13.0), onClick = {})
            FavoriteCard(PopularCities.all[1], WeatherSnapshot(CurrentWeather(9.0, 0, false, now), 3.0, 15.0), onClick = {})
            FavoriteCard(PopularCities.all[2], snapshot = null, onClick = {})
        }
    }
}
