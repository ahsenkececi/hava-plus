package com.ahsen.hava.ui.forecast

import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.WeatherInsights
import com.ahsen.hava.domain.model.CurrentWeather
import com.ahsen.hava.domain.model.DailyForecast
import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.HourlyForecast
import com.ahsen.hava.sound.SoundscapeParams
import com.ahsen.hava.sound.WeatherSoundscape
import com.ahsen.hava.ui.CityInfo
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.common.rainOutlookText
import com.ahsen.hava.ui.component.EmptyView
import com.ahsen.hava.ui.component.ErrorView
import com.ahsen.hava.ui.component.ShimmerList
import com.ahsen.hava.ui.component.WeatherScene
import com.ahsen.hava.ui.theme.GlassFill
import com.ahsen.hava.ui.theme.HavaTheme
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.SkyBackground
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.rememberTimeOfDaySky
import com.ahsen.hava.ui.theme.skyFor
import java.time.LocalDateTime

/** Bağlayıcı katman: ViewModel'i edinir, state'i [ForecastScreen]'e verir. */
@Composable
fun ForecastRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForecastViewModel = viewModel { ForecastViewModel(createSavedStateHandle()) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    ForecastScreen(
        soundEnabled = soundEnabled,
        onToggleSound = viewModel::onToggleSound,
        cityName = viewModel.city.name,
        subtitle = viewModel.city.subtitle,
        uiState = uiState,
        isFavorite = isFavorite,
        onToggleFavorite = viewModel::onToggleFavorite,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

@Composable
fun ForecastScreen(
    cityName: String,
    subtitle: String,
    uiState: LoadState<Forecast>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    soundEnabled: Boolean = false,
    onToggleSound: () -> Unit = {},
) {
    // Dinamik arka plan: veri gelince gökyüzü o şehrin gerçek havasına döner.
    val fallbackSky = rememberTimeOfDaySky()
    val sky = if (uiState is LoadState.Success) {
        val current = uiState.data.current
        skyFor(current.condition, current.isDay)
    } else {
        fallbackSky
    }

    // Havayı dinle: ekrana girince o şehrin şu anki havası sese dönüşür.
    val forecast = (uiState as? LoadState.Success)?.data
    WeatherSoundEffect(forecast = forecast, enabled = soundEnabled)

    SkyBackground(sky = sky, modifier = modifier) {
        // Canlı gökyüzü sahnesi: içeriğin arkasında, yarı saydam.
        if (uiState is LoadState.Success) {
            val current = uiState.data.current
            WeatherScene(condition = current.condition, isDay = current.isDay, modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            TopBar(
                cityName = cityName,
                subtitle = subtitle,
                onBack = onBack,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when (uiState) {
                    LoadState.Loading -> ShimmerList(itemCount = 4)
                    is LoadState.Success -> ForecastContent(
                        forecast = uiState.data,
                    )
                    LoadState.Empty -> EmptyView(title = stringResource(R.string.forecast_empty))
                    is LoadState.Error -> ErrorView(message = uiState.message.asString(), onRetry = onRetry)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    cityName: String,
    subtitle: String,
    onBack: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(CircleShape)
                .background(GlassFill),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = TextPrimary,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(
            onClick = onToggleSound,
            modifier = Modifier
                .padding(end = 8.dp)
                .clip(CircleShape)
                .background(GlassFill),
        ) {
            SpeakerIcon(muted = !soundEnabled)
        }
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .clip(CircleShape)
                .background(GlassFill),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = stringResource(if (isFavorite) R.string.cd_remove_favorite else R.string.cd_add_favorite),
                tint = if (isFavorite) HavaYellow else TextPrimary,
            )
        }
    }
}

/**
 * Ekran açıkken hava sesini çalar, uygulama arka plana geçince ya da ekrandan çıkınca durdurur.
 *
 * Kullanıcıyı rahatsız etmemek için otomatik çalmanın kuralları:
 * - Telefon sessizde ya da titreşimdeyse çalmaz.
 * - Kullanıcı ekrana girdiğinde başka bir uygulama müzik çalıyorsa araya girmez.
 * - Ses birden değil, 2 saniyede yavaşça açılır (bkz. SoundSynth).
 * - Üst köşedeki hoparlör düğmesiyle tamamen kapatılabilir; tercih hatırlanır.
 */
@Composable
private fun WeatherSoundEffect(forecast: Forecast?, enabled: Boolean) {
    val context = LocalContext.current
    val soundscape = remember { WeatherSoundscape() }
    DisposableEffect(Unit) { onDispose { soundscape.release() } }

    val audio = remember { context.getSystemService(AudioManager::class.java) }
    // Ekrana girildiği an başka bir uygulama çalıyor muydu? (Kendi sesimiz başlamadan bakıyoruz.)
    val othersPlaying = remember { audio?.isMusicActive == true }
    val params = remember(forecast) { forecast?.let { SoundscapeParams.from(it) } }

    LifecycleResumeEffect(params, enabled) {
        val ringerNormal = audio?.ringerMode == AudioManager.RINGER_MODE_NORMAL
        if (params != null && enabled && ringerNormal && !othersPlaying) {
            soundscape.play(params)
        }
        onPauseOrDispose { soundscape.stop() }
    }
}

/** Hoparlör ikonu: açıkken ses dalgaları, kapalıyken çarpı. */
@Composable
private fun SpeakerIcon(muted: Boolean) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val stroke = Stroke(width = s * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val body = Path().apply {
            moveTo(s * 0.10f, s * 0.38f)
            lineTo(s * 0.28f, s * 0.38f)
            lineTo(s * 0.50f, s * 0.18f)
            lineTo(s * 0.50f, s * 0.82f)
            lineTo(s * 0.28f, s * 0.62f)
            lineTo(s * 0.10f, s * 0.62f)
            close()
        }
        drawPath(body, TextPrimary, style = stroke)
        if (muted) {
            drawLine(TextPrimary, Offset(s * 0.64f, s * 0.38f), Offset(s * 0.88f, s * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(TextPrimary, Offset(s * 0.88f, s * 0.38f), Offset(s * 0.64f, s * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        } else {
            drawArc(HavaYellow, -45f, 90f, useCenter = false, topLeft = Offset(s * 0.38f, s * 0.30f), size = Size(s * 0.40f, s * 0.40f), style = stroke)
            drawArc(HavaYellow, -50f, 100f, useCenter = false, topLeft = Offset(s * 0.30f, s * 0.14f), size = Size(s * 0.72f, s * 0.72f), style = stroke)
        }
    }
}

@Composable
private fun ForecastContent(forecast: Forecast) {
    val today = forecast.daily.firstOrNull()
    // Yorumlar saf Kotlin kurallarından gelir (domain/logic/WeatherInsights.kt). Veri değişmedikçe yeniden hesaplanmaz.
    val rainOutlook = remember(forecast) { WeatherInsights.rainOutlook(forecast) }
    val currentUv = remember(forecast) { WeatherInsights.currentUv(forecast) }
    val nextTwoHours = remember(forecast) { WeatherInsights.nextTwoHours(forecast) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { CurrentHero(current = forecast.current, today = today) }
        // Sadece işe yaradığında görünür: yağmur yaklaşmıyorsa bu kart hiç yok.
        rainOutlook?.let { outlook -> item { RainBanner(message = rainOutlookText(outlook), slots = nextTwoHours) } }
        item { DetailsRow(current = forecast.current, uvIndex = currentUv) }
        item { HourlyCard(hours = forecast.upcomingHours()) }
        today?.let { day ->
            val sunrise = day.sunrise
            val sunset = day.sunset
            if (sunrise != null && sunset != null) {
                item { SunArcCard(sunrise = sunrise, sunset = sunset, now = forecast.current.observedAt) }
            }
        }
        item { DailyCard(days = forecast.daily) }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ForecastScreenPreview() {
    val now = LocalDateTime.of(2026, 10, 25, 14, 0)
    val forecast = Forecast(
        current = CurrentWeather(
            temperatureC = 21.4,
            weatherCode = 2,
            isDay = true,
            observedAt = now,
            apparentTemperatureC = 20.1,
            humidityPercent = 58,
            windSpeedKmh = 14.0,
        ),
        hourly = (0 until 24).map { h ->
            HourlyForecast(now.plusHours(h.toLong()), 21.0 - h % 8, listOf(0, 1, 2, 61)[h % 4], h < 5, h * 3)
        },
        daily = (0 until 7).map { d ->
            DailyForecast(
                date = now.toLocalDate().plusDays(d.toLong()),
                minTemperatureC = 12.0 + d % 3,
                maxTemperatureC = 20.0 + d % 4,
                weatherCode = listOf(0, 2, 61, 3)[d % 4],
                precipitationProbability = d * 10,
                sunrise = now.toLocalDate().plusDays(d.toLong()).atTime(7, 41),
                sunset = now.toLocalDate().plusDays(d.toLong()).atTime(18, 23),
            )
        },
    )
    HavaTheme {
        ForecastScreen(
            cityName = "İzmir",
            subtitle = "İzmir, Türkiye",
            uiState = LoadState.Success(forecast),
            onBack = {},
            onRetry = {},
        )
    }
}
