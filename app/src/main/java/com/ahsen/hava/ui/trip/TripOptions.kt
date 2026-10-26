package com.ahsen.hava.ui.trip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.DepartureAdvice
import com.ahsen.hava.domain.logic.ModeOption
import com.ahsen.hava.domain.logic.RideHazard
import com.ahsen.hava.domain.model.RouteWeather
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.domain.usecase.TripPlan
import com.ahsen.hava.ui.BottomBarSpace
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.common.departureOffsetText
import com.ahsen.hava.ui.common.isRainWarning
import com.ahsen.hava.ui.common.label
import com.ahsen.hava.ui.common.summaryText
import com.ahsen.hava.ui.common.text
import com.ahsen.hava.ui.common.uiText
import com.ahsen.hava.ui.component.ErrorView
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.theme.CardSurface
import com.ahsen.hava.ui.theme.CoolBlue
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.HavaNavy
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.WarmOrange
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * Yolculuk+ alt kısmı, Google Maps düzeninde:
 *   1. ulaşım türü sekmeleri (süreleriyle)
 *   2. seçili türün detay kartı (süre, varış, neden, yol boyunca hava, ne zaman çıkmalıyım)
 *   3. diğer türler sade bir liste
 */

/** İki nokta seçilince: tür sekmeleri, seçili türün detayı ve diğer türler. */
@Composable
internal fun RouteOptions(
    routes: LoadState<TripRoutes>?,
    plan: TripPlan?,
    selectedMode: TravelMode?,
    isWeatherLoading: Boolean,
    departureOffset: Int,
    onModeSelect: (TravelMode) -> Unit,
    onDepartureChange: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (routes) {
            null, LoadState.Loading -> item(key = "loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HavaYellow)
                }
            }
            LoadState.Empty -> item(key = "empty") {
                Text(stringResource(R.string.route_not_found), color = TextPrimary, modifier = Modifier.padding(8.dp))
            }
            is LoadState.Error -> item(key = "error") {
                ErrorView(message = routes.message.asString(), onRetry = onRetry)
            }
            is LoadState.Success -> {
                val trip = routes.data
                // Puanlama ve "ne zaman çıkmalıyım?" ViewModel'de hesaplanır; ekran sadece gösterir.
                val tripPlan = plan ?: return@LazyColumn
                val recommendation = tripPlan.recommendation
                val best = recommendation.best ?: return@LazyColumn
                // Kullanıcı bir sekme seçmediyse önerilen tür seçilidir.
                val selected = recommendation.options.firstOrNull { it.mode == selectedMode } ?: best

                item(key = "modes") {
                    ModeTabs(options = recommendation.options, best = best, selected = selected, onSelect = onModeSelect)
                }
                item(key = "detail") {
                    ModeDetailCard(
                        option = selected,
                        headline = if (selected == best) recommendation.headline.text() else null,
                        alternatives = (trip.byMode[selected.mode]?.size ?: 1) - 1,
                        departureOffset = departureOffset,
                        // "Ne zaman çıkmalıyım?" açıkta giden tür için hesaplanır; o tür seçiliyse gösterilir.
                        advice = tripPlan.departureAdvice?.takeIf { it.mode == selected.mode },
                        onDepartureChange = onDepartureChange,
                        isWeatherLoading = isWeatherLoading,
                    )
                }
                val others = recommendation.options.filter { it != selected }
                if (others.isNotEmpty()) {
                    item(key = "others") { OtherModesList(options = others, onSelect = onModeSelect) }
                }
                if (trip.unavailable.isNotEmpty()) {
                    item(key = "unavailable") {
                        Text(
                            text = stringResource(R.string.route_unavailable, trip.unavailable.map { it.label() }.joinToString()),
                            modifier = Modifier.padding(start = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

/** Ulaşım türü sekmeleri: ikon + süre. Seçili sekme sarı; önerilen türün üstünde "ÖNERİLEN" yazar. */
@Composable
private fun ModeTabs(options: List<ModeOption>, best: ModeOption, selected: ModeOption, onSelect: (TravelMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.sortedBy { it.mode.ordinal }.forEach { option ->
            val isSelected = option == selected
            val shape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (isSelected) HavaYellow else CardSurface)
                    .border(1.dp, if (isSelected) HavaYellow else GlassBorder, shape)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(option.mode) })
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (option == best) stringResource(R.string.recommended_title) else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) HavaNavy else HavaYellow,
                    maxLines = 1,
                )
                TravelModeIcon(mode = option.mode, size = 22.dp, color = if (isSelected) HavaNavy else option.mode.accent())
                Text(
                    formatMinutes(option.durationMinutes),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) HavaNavy else TextPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Seçili türün bütün ayrıntısı tek kartta. */
@Composable
private fun ModeDetailCard(
    option: ModeOption,
    headline: String?,
    alternatives: Int,
    departureOffset: Int,
    advice: DepartureAdvice?,
    onDepartureChange: (Int) -> Unit,
    isWeatherLoading: Boolean,
) {
    val arrival = remember(departureOffset, option.durationMinutes) {
        LocalTime.now().plusMinutes((departureOffset + option.durationMinutes).toLong()).format(CLOCK)
    }
    GlassCard(modifier = Modifier.fillMaxWidth(), fill = SolidColor(CardSurface)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TravelModeIcon(mode = option.mode, size = 20.dp, color = option.mode.accent())
            Text(
                option.mode.label(),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = if (departureOffset == 0) stringResource(R.string.departure_now) else stringResource(R.string.departure_after, departureOffsetText(departureOffset)),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatMinutes(option.durationMinutes), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Light, color = TextPrimary)
                Text(
                    stringResource(R.string.trip_distance_arrival, formatKm(option.distanceKm), arrival),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                ScoreBadge(score = option.score)
                Text(
                    text = if (alternatives > 0) stringResource(R.string.cost_with_alternatives, option.cost.label(), alternatives) else option.cost.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (option.isEstimate) {
                    Text(stringResource(R.string.estimate_badge), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
        headline?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        // Gerekçeler: "+6 dk · Yağmura 4 dk açıkta kalırsın · ..."
        option.reasons.forEach { reason ->
            Text(
                text = stringResource(R.string.bullet_item, reason.text()),
                style = MaterialTheme.typography.bodySmall,
                color = if (reason.isRainWarning()) CoolBlue else TextSecondary,
            )
        }
        // Yol güvenliği (bisiklet/scooter): savrulma ve kayma riski
        option.hazards.forEach { hazard -> HazardRow(hazard) }
        val weather = option.weather
        Spacer(Modifier.height(12.dp))
        when {
            weather != null && weather.points.isNotEmpty() -> {
                RouteWeatherStrip(weather = weather)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = weather.summaryText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (weather.wetMinutes > 0) CoolBlue else TextSecondary,
                )
            }
            isWeatherLoading -> Text(stringResource(R.string.route_weather_loading), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        advice?.let {
            Spacer(Modifier.height(16.dp))
            DepartureSection(advice = it, selectedOffset = departureOffset, onSelect = onDepartureChange)
        }
    }
}

/** Seçili olmayan türler: Maps'teki gibi sade satırlar. Dokununca o tür seçilir. */
@Composable
private fun OtherModesList(options: List<ModeOption>, onSelect: (TravelMode) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), fill = SolidColor(CardSurface), contentPadding = 0.dp) {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlassBorder),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option.mode) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(option.mode.accent().copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    TravelModeIcon(mode = option.mode, size = 22.dp, color = option.mode.accent())
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(option.mode.label(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    option.reasons.firstOrNull()?.let { reason ->
                        Text(
                            reason.text(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (reason.isRainWarning()) CoolBlue else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMinutes(option.durationMinutes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light, color = TextPrimary)
                    Text(formatKm(option.distanceKm), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }
        }
    }
}

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 100 üzerinden puan: yüksekse sarı. */
@Composable
private fun ScoreBadge(score: Int) {
    val color = when {
        score >= 80 -> HavaYellow
        score >= 60 -> TextPrimary
        else -> TextSecondary
    }
    Text(text = stringResource(R.string.score_out_of_100, score), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
}

/**
 * Yol boyunca hava şeridi: soldan sağa yolculuk. Her parça, o bölümden geçeceğin andaki hava.
 * Kuru kısımlar sade, yağmurlu kısımlar mavi; yağmur ne kadar şiddetliyse mavi o kadar koyu.
 */
@Composable
private fun RouteWeatherStrip(weather: RouteWeather) {
    val segments = weather.segments()
    val total = segments.sumOf { it.second }.coerceAtLeast(1)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(GlassBorder, cornerRadius = radius)
        var x = 0f
        segments.forEach { (point, minutes) ->
            val w = size.width * minutes / total
            if (point.isWet) {
                val intensity = (point.precipitationMm / 1.0).coerceIn(0.35, 1.0).toFloat()
                drawRoundRect(
                    CoolBlue.copy(alpha = intensity),
                    topLeft = Offset(x, 0f),
                    size = Size(w, size.height),
                    cornerRadius = radius,
                )
            }
            x += w
        }
    }
}

/** Tek bir yol güvenliği uyarısı: turuncu üçgen + cümle. */
@Composable
private fun HazardRow(hazard: RideHazard) {
    val text = hazard.uiText().asString()
    Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = stringResource(R.string.cd_hazard),
            tint = WarmOrange,
            modifier = Modifier
                .padding(top = 1.dp, end = 6.dp)
                .size(16.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = WarmOrange)
    }
}

