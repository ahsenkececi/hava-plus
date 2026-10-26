package com.ahsen.hava.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.CompassPoint
import com.ahsen.hava.domain.logic.CostLevel
import com.ahsen.hava.domain.logic.DepartureMessage
import com.ahsen.hava.domain.logic.RainOutlook
import com.ahsen.hava.domain.logic.RideHazard
import com.ahsen.hava.domain.logic.TripHeadline
import com.ahsen.hava.domain.logic.TripReason
import com.ahsen.hava.domain.logic.UvLevel
import com.ahsen.hava.domain.model.RouteRain
import com.ahsen.hava.domain.model.RouteWeather
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.WeatherCondition
import java.time.format.DateTimeFormatter

/*
 * Domain → metin çevirmeni.
 *
 * Domain katmanı cümle üretmez; "yağmur 45 dk sonra başlıyor" yerine RainOutlook.StartsIn(45) döner.
 * O sonuçları kullanıcının dilinde cümleye çeviren TEK yer burası ve bütün metinler strings.xml'de.
 * Uygulamayı başka bir dile çevirmek için values-en/strings.xml eklemek yeter; bu dosya değişmez.
 */

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// ------------------------------------------------------------------ etiketler

@StringRes
fun WeatherCondition.labelRes(): Int = when (this) {
    WeatherCondition.CLEAR -> R.string.condition_clear
    WeatherCondition.MAINLY_CLEAR -> R.string.condition_mainly_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.condition_partly_cloudy
    WeatherCondition.OVERCAST -> R.string.condition_overcast
    WeatherCondition.FOG -> R.string.condition_fog
    WeatherCondition.DRIZZLE -> R.string.condition_drizzle
    WeatherCondition.RAIN -> R.string.condition_rain
    WeatherCondition.SNOW -> R.string.condition_snow
    WeatherCondition.RAIN_SHOWERS -> R.string.condition_rain_showers
    WeatherCondition.SNOW_SHOWERS -> R.string.condition_snow_showers
    WeatherCondition.THUNDERSTORM -> R.string.condition_thunderstorm
    WeatherCondition.UNKNOWN -> R.string.condition_unknown
}

@Composable
fun WeatherCondition.label(): String = stringResource(labelRes())

@StringRes
fun TravelMode.labelRes(): Int = when (this) {
    TravelMode.Walk -> R.string.mode_walk
    TravelMode.Bike -> R.string.mode_bike
    TravelMode.Car -> R.string.mode_car
    TravelMode.Transit -> R.string.mode_transit
}

@Composable
fun TravelMode.label(): String = stringResource(labelRes())

/** Cümle içinde kullanılan küçük harfli hali: "... ama bisiklet ile 12 dk yağmurda kalırdın". */
@StringRes
fun TravelMode.inlineLabelRes(): Int = when (this) {
    TravelMode.Walk -> R.string.mode_walk_inline
    TravelMode.Bike -> R.string.mode_bike_inline
    TravelMode.Car -> R.string.mode_car_inline
    TravelMode.Transit -> R.string.mode_transit_inline
}

@Composable
private fun TravelMode.inlineLabel(): String = stringResource(inlineLabelRes())

@Composable
fun CostLevel.label(): String = stringResource(
    when (this) {
        CostLevel.Free -> R.string.cost_free
        CostLevel.Ticket -> R.string.cost_ticket
        CostLevel.Fuel -> R.string.cost_fuel
    },
)

@Composable
fun UvLevel.label(): String = stringResource(
    when (this) {
        UvLevel.Low -> R.string.uv_low
        UvLevel.Moderate -> R.string.uv_moderate
        UvLevel.High -> R.string.uv_high
        UvLevel.VeryHigh -> R.string.uv_very_high
        UvLevel.Extreme -> R.string.uv_extreme
    },
)

@Composable
fun CompassPoint.shortLabel(): String = stringResource(
    when (this) {
        CompassPoint.N -> R.string.compass_n
        CompassPoint.NE -> R.string.compass_ne
        CompassPoint.E -> R.string.compass_e
        CompassPoint.SE -> R.string.compass_se
        CompassPoint.S -> R.string.compass_s
        CompassPoint.SW -> R.string.compass_sw
        CompassPoint.W -> R.string.compass_w
        CompassPoint.NW -> R.string.compass_nw
    },
)

// ------------------------------------------------------------------ süreler

/** 45 → "45 dk", 60 → "1 sa", 90 → "1 sa 30 dk". Compose dışında (widget, bildirim) da kullanılır. */
fun durationUiText(minutes: Int): UiText = when {
    minutes < 60 -> UiText(R.string.duration_minutes, minutes)
    minutes % 60 == 0 -> UiText(R.string.duration_hours, minutes / 60)
    else -> UiText(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}

@Composable
fun durationText(minutes: Int): String = durationUiText(minutes).asString()

/** Çıkış şeridi için kısa hali: 0 → "Şimdi", 45 → "45 dk", 90 → "1,5 sa". */
@Composable
fun departureOffsetText(minutes: Int): String = when {
    minutes == 0 -> stringResource(R.string.now)
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
    else -> stringResource(R.string.duration_hours_fraction, minutes / 60, minutes % 60 * 10 / 60)
}

// ------------------------------------------------------------------ cümleler

/** Yağmur cümlesi. [UiText] döner: ekranda, widget'ta ve bildirimde aynı cümle. */
fun RainOutlook.uiText(): UiText = when (this) {
    RainOutlook.RainingForAWhile -> UiText(R.string.rain_for_a_while)
    is RainOutlook.RainingStopsIn -> UiText(R.string.rain_stops_in, durationUiText(minutes))
    is RainOutlook.StartsIn -> {
        val start = if (minutes < 5) UiText(R.string.rain_starts_soon) else UiText(R.string.rain_starts_in, durationUiText(minutes))
        UiText(
            if (lastsBeyondForecast) R.string.rain_lasts_at_least else R.string.rain_lasts_about,
            start,
            durationUiText(durationMinutes),
        )
    }
    is RainOutlook.ExpectedAround -> UiText(R.string.rain_expected_around, time.format(CLOCK), probability)
}

@Composable
fun rainOutlookText(outlook: RainOutlook): String = outlook.uiText().asString()

@Composable
fun departureMessageText(message: DepartureMessage): String = when (message) {
    DepartureMessage.DryForTwoHours -> stringResource(R.string.departure_dry_two_hours)
    is DepartureMessage.LeaveNowToStayDry ->
        stringResource(R.string.departure_leave_now, departureOffsetText(message.wetFromOffsetMinutes))
    is DepartureMessage.WaitToStayDry -> stringResource(
        R.string.departure_wait_dry,
        departureOffsetText(message.best.offsetMinutes),
        message.best.time.format(CLOCK),
        message.wetNowMinutes,
    )
    is DepartureMessage.WaitForLessRain -> stringResource(
        R.string.departure_wait_less,
        departureOffsetText(message.best.offsetMinutes),
        message.best.time.format(CLOCK),
        message.wetNowMinutes,
        message.best.wetMinutes,
    )
    DepartureMessage.RainDoesNotStop -> stringResource(R.string.departure_rain_not_stop)
}

@Composable
fun TripReason.text(): String = when (this) {
    TripReason.Fastest -> stringResource(R.string.reason_fastest)
    is TripReason.SlowerBy -> stringResource(R.string.reason_slower_by, minutes)
    is TripReason.WetFor -> stringResource(R.string.reason_wet_for, minutes)
    TripReason.DryAllAlong -> stringResource(R.string.reason_dry_all_along)
    is TripReason.StrongWind -> stringResource(R.string.reason_strong_wind, kmh)
    is TripReason.Cold -> stringResource(R.string.reason_cold, feelsLikeC)
    is TripReason.Hot -> stringResource(R.string.reason_hot, feelsLikeC)
    TripReason.LongWalk -> stringResource(R.string.reason_long_walk)
    TripReason.LongRide -> stringResource(R.string.reason_long_ride)
    TripReason.ShelteredFromRain -> stringResource(R.string.reason_sheltered)
    is TripReason.CarOverhead -> stringResource(R.string.reason_car_overhead, minutes)
    TripReason.TransitEstimate -> stringResource(R.string.reason_transit_estimate)
    is TripReason.TransitWalkWet -> if (minutes > 0) {
        stringResource(R.string.reason_transit_walk_wet, minutes)
    } else {
        stringResource(R.string.reason_transit_walk_dry)
    }
}

/** Gerekçe yağmurla ilgili ve olumsuz mu? (Ekranda mavi gösterilir.) */
fun TripReason.isRainWarning(): Boolean =
    this is TripReason.WetFor || (this is TripReason.TransitWalkWet && minutes > 0)

@Composable
fun TripHeadline.text(): String = when (this) {
    TripHeadline.NoOptions -> stringResource(R.string.headline_no_options)
    is TripHeadline.FastestAndComfortable -> stringResource(R.string.headline_fastest_comfortable, mode.label())
    is TripHeadline.FastestButWet -> stringResource(R.string.headline_fastest_wet, mode.label(), wetMinutes)
    is TripHeadline.SlowerButDrier -> stringResource(
        R.string.headline_slower_drier,
        mode.label(),
        extraMinutes,
        quickest.inlineLabel(),
        quickestWetMinutes,
    )
    is TripHeadline.SlowerButBalanced -> stringResource(R.string.headline_slower_balanced, mode.label(), extraMinutes)
}

/** "Son 10 dk yağmur · hissedilen 17°" */
@Composable
fun RouteWeather.summaryText(): String {
    val rainText = when (val rain = rain()) {
        is RouteRain.Dry -> rain.highProbability?.let { stringResource(R.string.route_dry_risky, it) }
            ?: stringResource(R.string.route_dry)
        RouteRain.AllAlong -> stringResource(R.string.route_rain_all_along)
        is RouteRain.FirstMinutes -> stringResource(R.string.route_rain_first, rain.minutes)
        is RouteRain.LastMinutes -> stringResource(R.string.route_rain_last, rain.minutes)
        is RouteRain.After -> stringResource(R.string.route_rain_after, rain.startMinute, rain.minutes)
    }
    return stringResource(R.string.route_summary, rainText, feelsLikeC)
}

// ------------------------------------------------------------------ yol güvenliği

/**
 * Yol riski cümlesi. Composable değil, [UiText] döner: hem ekranda hem bildirimde
 * (Context ile) aynı cümle kullanılır.
 */
fun RideHazard.uiText(): UiText {
    val time = if (minutesFromStart == 0) UiText(R.string.hazard_at_start) else UiText(R.string.hazard_at_minute, minutesFromStart)
    return when (this) {
        is RideHazard.CrossWind -> UiText(
            if (severe) R.string.hazard_crosswind_severe else R.string.hazard_crosswind,
            crossWindKmh,
            time,
        )
        is RideHazard.SlipperyFirstRain -> UiText(R.string.hazard_slippery, time)
    }
}

