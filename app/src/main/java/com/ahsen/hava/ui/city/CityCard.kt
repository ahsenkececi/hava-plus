package com.ahsen.hava.ui.city

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.CurrentWeather
import com.ahsen.hava.domain.model.PopularCities
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.component.WeatherIcon
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.HavaNavy
import com.ahsen.hava.ui.theme.HavaTheme
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.Sky
import com.ahsen.hava.ui.theme.SkyBackground
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Listedeki tek bir şehir: plaka rozeti + ad + alt yazı + ok.
 *
 * Bileşen state tutmaz: seçili olup olmadığını dışarıdan alır, tıklanınca
 * sadece haber verir (state hoisting: değer aşağı, olay yukarı).
 */
@Composable
fun CityCard(
    city: City,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    weather: CurrentWeather? = null,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (isSelected) HavaYellow else GlassBorder,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlateBadge(text = city.badge, isSelected = isSelected)
            // weight(1f): kalan bütün genişliği alır, oku sağ kenara iter.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = city.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Favori kartlarında o şehrin şu anki havası: ikon + sıcaklık.
            if (weather != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherIcon(condition = weather.condition, isDay = weather.isDay, size = 24.dp)
                    Text(
                        text = "${weather.temperatureC.roundToInt()}°",
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary,
                    )
                }
            }
            if (onToggleFavorite != null) {
                // Yıldız: favoriye ekle / çıkar. Karta dokunmak tahmine gider, yıldıza dokunmak sadece favoriler.
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = stringResource(if (isFavorite) R.string.cd_remove_favorite else R.string.cd_add_favorite),
                        tint = if (isFavorite) HavaYellow else TextSecondary,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (isSelected) HavaYellow else TextSecondary,
                )
            }
        }
    }
}

/** Plaka rozeti. Seçiliyken sarıya dolar. */
@Composable
private fun PlateBadge(text: String, isSelected: Boolean, modifier: Modifier = Modifier) {
    val background by animateColorAsState(
        if (isSelected) HavaYellow else Color(0x33FFC900),
        label = "badgeBg",
    )
    val content by animateColorAsState(
        if (isSelected) HavaNavy else HavaYellow,
        label = "badgeText",
    )
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

@Preview
@Composable
private fun CityCardPreview() {
    HavaTheme {
        SkyBackground(sky = Sky.ClearNight) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CityCard(city = PopularCities.all[1], isSelected = false, onClick = {}, onToggleFavorite = {})
                CityCard(city = PopularCities.all[2], isSelected = true, onClick = {}, isFavorite = true, onToggleFavorite = {})
            }
        }
    }
}
