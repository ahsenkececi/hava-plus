package com.ahsen.hava.ui.city

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.ui.common.label
import com.ahsen.hava.ui.common.rainOutlookText
import com.ahsen.hava.ui.component.SkyCard
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.rememberTimeOfDaySky
import com.ahsen.hava.ui.theme.skyFor
import kotlin.math.roundToInt

/**
 * "Konumunda şimdi" kartı: bulunduğun yerin gökyüzü, sıcaklığı ve varsa yağmur uyarısı.
 * Dokununca o yerin tahmin ekranı açılır. İzin yoksa ya da konum bulunamadıysa dokununca tekrar denenir.
 */
@Composable
fun LocalWeatherCard(state: CityViewModel.LocalWeather, onClick: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        is CityViewModel.LocalWeather.Ready -> {
            val current = state.current
            CityWeatherCard(
                sky = skyFor(current.condition, current.isDay),
                title = state.city.name,
                subtitle = listOfNotNull(state.city.admin1, current.condition.label()).joinToString(" · "),
                temperature = "${current.temperatureC.roundToInt()}°",
                note = state.rainOutlook?.let { rainOutlookText(it) } ?: feelsLikeText(current),
                noteIsAlert = state.rainOutlook != null,
                range = rangeText(state.todayMinC, state.todayMaxC),
                onClick = onClick,
                isMyLocation = true,
                modifier = modifier,
            )
        }
        CityViewModel.LocalWeather.Loading -> MessageCard(onClick = onClick, modifier = modifier, loading = true) {
            stringResource(R.string.local_loading)
        }
        CityViewModel.LocalWeather.NeedsPermission -> MessageCard(onClick = onClick, modifier = modifier) {
            stringResource(R.string.local_needs_permission)
        }
        is CityViewModel.LocalWeather.Failed -> MessageCard(onClick = onClick, modifier = modifier) {
            stringResource(R.string.local_failed, state.message.asString())
        }
    }
}

/** Hava henüz yokken: günün saatine göre gökyüzü ve tek satır açıklama. */
@Composable
private fun MessageCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    text: @Composable () -> String,
) {
    SkyCard(sky = rememberTimeOfDaySky(), onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HavaYellow, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = HavaYellow, modifier = Modifier.size(20.dp))
            }
            Text(
                text = text(),
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
        }
    }
}
