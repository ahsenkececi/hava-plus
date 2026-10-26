package com.ahsen.hava.domain.logic

import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.PrecipitationSlot
import com.ahsen.hava.domain.model.WeatherCondition
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** UV indeksi seviyeleri (Dünya Sağlık Örgütü ölçeği). */
enum class UvLevel {
    Low,
    Moderate,
    High,
    VeryHigh,
    Extreme,
    ;

    companion object {
        fun from(uv: Double): UvLevel = when {
            uv < 3 -> Low
            uv < 6 -> Moderate
            uv < 8 -> High
            uv < 11 -> VeryHigh
            else -> Extreme
        }
    }
}

/**
 * 15 dakikalık bir dilimdeki yağışın şiddeti. Eşikler meteorolojideki saatlik sınıfların
 * (hafif < 2,5 mm/sa, orta < 7,6 mm/sa) 15 dakikaya bölünmüş hali.
 */
enum class RainIntensity {
    None,
    Light,
    Moderate,
    Heavy,
    ;

    companion object {
        fun from(mmPer15Minutes: Double): RainIntensity = when {
            mmPer15Minutes < 0.1 -> None
            mmPer15Minutes < 0.625 -> Light
            mmPer15Minutes < 1.9 -> Moderate
            else -> Heavy
        }
    }
}

/** Görüş mesafesinin anlamı. */
enum class VisibilityLevel {
    Fog, // < 1 km
    Haze, // 1–4 km
    Good, // 4–10 km
    Excellent, // > 10 km
    ;

    companion object {
        fun from(meters: Double): VisibilityLevel = when {
            meters < 1_000 -> Fog
            meters < 4_000 -> Haze
            meters < 10_000 -> Good
            else -> Excellent
        }
    }
}

/** Çiy noktasına göre havanın nasıl hissettirdiği: nem oranından daha iyi bir "bunaltı" göstergesi. */
enum class DewPointComfort {
    Dry, // < 10°
    Comfortable, // 10–16°
    Humid, // 16–20°
    Oppressive, // ≥ 20°
    ;

    companion object {
        fun from(dewPointC: Double): DewPointComfort = when {
            dewPointC < 10 -> Dry
            dewPointC < 16 -> Comfortable
            dewPointC < 20 -> Humid
            else -> Oppressive
        }
    }
}

/** Sekiz ana yön, kuzeyden saat yönünde. */
enum class CompassPoint { N, NE, E, SE, S, SW, W, NW }

/** Önümüzdeki yağmurun durumu. Süreler dakika cinsinden ve 5 dakikaya yuvarlanmış. */
sealed interface RainOutlook {
    /** Yağıyor ve önümüzdeki 2 saat dinmiyor. */
    data object RainingForAWhile : RainOutlook

    /** Yağıyor, [minutes] dakika sonra diniyor. */
    data class RainingStopsIn(val minutes: Int) : RainOutlook

    /**
     * [minutes] dakika sonra başlıyor, yaklaşık [durationMinutes] dakika sürüyor.
     * [lastsBeyondForecast]: 2 saatlik verinin sonuna kadar sürüyor, yani "en az" o kadar.
     */
    data class StartsIn(val minutes: Int, val durationMinutes: Int, val lastsBeyondForecast: Boolean) : RainOutlook

    /** Yakın zamanda değil ama saatlik tahminde [time] civarı, [probability] ihtimalle. */
    data class ExpectedAround(val time: LocalDateTime, val probability: Int) : RainOutlook
}

/**
 * Ham tahmin verisini anlamlı sonuçlara çeviren kurallar.
 *
 * Bilerek saf Kotlin: Android'e, ağa ya da ekrana bağımlı değil. Girdi bir [Forecast],
 * çıktı tipli bir sonuç (metin değil). Bu yüzden test etmesi ve kuralları değiştirmesi kolay.
 */
object WeatherInsights {

    private const val WET_MM = 0.1
    private val WET_CONDITIONS = setOf(
        WeatherCondition.DRIZZLE,
        WeatherCondition.RAIN,
        WeatherCondition.RAIN_SHOWERS,
        WeatherCondition.SNOW,
        WeatherCondition.SNOW_SHOWERS,
        WeatherCondition.THUNDERSTORM,
    )

    // ---------------------------------------------------------------- Yağmur durumu

    /**
     * Önümüzdeki yağmurun durumu. Önce 15 dakikalık veriye (2 saat), orada yağmur yoksa saatlik tahmine
     * (12 saat) bakar; ikisinde de yoksa null döner ve ekranda hiçbir şey görünmez.
     * Cümleyi ekran kurar ("Yağmur 40 dk sonra başlıyor"); burası sadece ne olduğunu söyler.
     */
    fun rainOutlook(forecast: Forecast): RainOutlook? {
        val now = forecast.current.observedAt
        val slots = forecast.minutely15.filter { !it.time.isBefore(now.minusMinutes(14)) }
        if (slots.isNotEmpty()) {
            val firstWet = slots.indexOfFirst { it.precipitationMm >= WET_MM }
            if (firstWet == 0) {
                val firstDry = slots.indexOfFirst { it.precipitationMm < WET_MM }
                return if (firstDry == -1) {
                    RainOutlook.RainingForAWhile
                } else {
                    RainOutlook.RainingStopsIn(minutesUntil(now, slots[firstDry].time))
                }
            }
            if (firstWet > 0) {
                val wetCount = slots.drop(firstWet).takeWhile { it.precipitationMm >= WET_MM }.size
                return RainOutlook.StartsIn(
                    minutes = minutesUntil(now, slots[firstWet].time),
                    durationMinutes = wetCount * 15,
                    lastsBeyondForecast = firstWet + wetCount == slots.size,
                )
            }
        }
        val rainyHour = forecast.upcomingHours(12).drop(1).firstOrNull { (it.precipitationProbability ?: 0) >= 60 }
        return rainyHour?.let { RainOutlook.ExpectedAround(it.time, it.precipitationProbability ?: 0) }
    }

    /** 5 dakikaya yuvarlanmış süre: "42 dk" yerine "40 dk" daha doğal okunur. */
    private fun minutesUntil(from: LocalDateTime, to: LocalDateTime): Int {
        val minutes = Duration.between(from, to).toMinutes().toInt().coerceAtLeast(0)
        return (minutes + 2) / 5 * 5
    }

    /**
     * Şu andan itibaren 2 saatlik yağış: 8 adet 15 dakikalık dilim. Grafik için.
     * İçinde bulunduğumuz dilim de dahil (14 dk öncesine kadar başlamış olabilir).
     */
    fun nextTwoHours(forecast: Forecast): List<PrecipitationSlot> {
        val now = forecast.current.observedAt
        return forecast.minutely15.filter { !it.time.isBefore(now.minusMinutes(14)) }.take(8)
    }

    /** Önümüzdeki 2 saatte grafiğe değecek kadar yağış var mı? */
    fun hasRainSoon(forecast: Forecast): Boolean =
        nextTwoHours(forecast).any { RainIntensity.from(it.precipitationMm) != RainIntensity.None }

    // ---------------------------------------------------------------- UV ve rüzgâr

    /** Şu anki saatin UV indeksi. */
    fun currentUv(f: Forecast): Double? {
        val thisHour = f.current.observedAt.truncatedTo(ChronoUnit.HOURS)
        return f.hourly.firstOrNull { it.time == thisHour }?.uvIndex
    }

    /** Rüzgârın geldiği yön, sekiz ana yönden en yakını (ekranda "KB" gibi kısaltılır). */
    fun compassPoint(degrees: Double): CompassPoint =
        CompassPoint.entries[(((degrees % 360) + 360) % 360 / 45 + 0.5).toInt() % 8]
}
