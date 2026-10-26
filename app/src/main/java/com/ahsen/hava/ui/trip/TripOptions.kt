package com.ahsen.hava.ui.trip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.ModeOption
import com.ahsen.hava.domain.logic.RideHazard
import com.ahsen.hava.domain.model.RouteWeather
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
import com.ahsen.hava.ui.theme.GlassFill
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.WarmOrange

/*
 * Yolculuk+ alt kısmı: önerilen seçenek, bütün ulaşım seçenekleri ve yol boyunca hava şeridi.
 */

/** İki nokta seçilince: önerilen seçenek, çıkış saati önerisi ve bütün ulaşım seçenekleri. */
@Composable
internal fun RouteOptions(
    routes: LoadState<TripRoutes>?,
    plan: TripPlan?,
    isWeatherLoading: Boolean,
    departureOffset: Int,
    onDepartureChange: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                recommendation.best?.let { best ->
                    item(key = "recommendation") {
                        RecommendationCard(best = best, headline = recommendation.headline.text(), departureOffset = departureOffset)
                    }
                }
                tripPlan.departureAdvice?.let { advice ->
                    item(key = "departure") {
                        DepartureCard(advice = advice, selectedOffset = departureOffset, onSelect = onDepartureChange)
                    }
                }
                items(recommendation.options, key = { it.mode.name }) { option ->
                    OptionCard(
                        option = option,
                        isBest = option == recommendation.best,
                        alternatives = (trip.byMode[option.mode]?.size ?: 1) - 1,
                        isWeatherLoading = isWeatherLoading,
                    )
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

/** En üstte: önerilen seçenek ve "neden?" cümlesi. */
@Composable
private fun RecommendationCard(best: ModeOption, headline: String, departureOffset: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = HavaYellow, fill = SolidColor(CardSurface)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.recommended_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = HavaYellow,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (departureOffset == 0) stringResource(R.string.departure_now) else stringResource(R.string.departure_after, departureOffsetText(departureOffset)),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TravelModeIcon(mode = best.mode, size = 32.dp, color = HavaYellow)
            Text(
                text = best.mode.label(),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMinutes(best.durationMinutes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light, color = TextPrimary)
                Text(formatKm(best.distanceKm), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(headline, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}

/** Bir seçenek: ikon, süre, puan, gerekçeler ve yol boyunca hava şeridi. */
@Composable
private fun OptionCard(option: ModeOption, isBest: Boolean, alternatives: Int, isWeatherLoading: Boolean) {
    val accent = option.mode.accent()
    // Opak, sade kart; tür sadece ikonun renginden ayırt edilir. En iyisi sarı kenarlıkla işaretli.
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isBest) HavaYellow.copy(alpha = 0.7f) else GlassBorder,
        fill = SolidColor(CardSurface),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                TravelModeIcon(mode = option.mode, size = 26.dp, color = accent)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(option.mode.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    ScoreBadge(score = option.score)
                    if (option.isEstimate) {
                        Text(
                            text = stringResource(R.string.estimate_badge),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassFill)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
                Text(
                    text = if (alternatives > 0) stringResource(R.string.cost_with_alternatives, option.cost.label(), alternatives) else option.cost.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            // Süre ve gidilecek yol mesafesi yan yana değil alt alta: ikisi de bir bakışta okunur.
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMinutes(option.durationMinutes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light, color = TextPrimary)
                Text(formatKm(option.distanceKm), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
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
        Spacer(Modifier.height(10.dp))
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
    }
}

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

