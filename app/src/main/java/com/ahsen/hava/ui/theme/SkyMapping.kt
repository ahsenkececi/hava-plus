package com.ahsen.hava.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ahsen.hava.domain.model.WeatherCondition
import java.time.LocalTime

/** Gerçek havayı gökyüzü rengine çevirir: dinamik arka planın kalbi. */
fun skyFor(condition: WeatherCondition, isDay: Boolean): Sky = when (condition) {
    WeatherCondition.CLEAR,
    WeatherCondition.MAINLY_CLEAR,
    WeatherCondition.PARTLY_CLOUDY,
    -> if (isDay) Sky.ClearDay else Sky.ClearNight
    WeatherCondition.OVERCAST,
    WeatherCondition.FOG,
    WeatherCondition.UNKNOWN,
    -> if (isDay) Sky.Cloudy else Sky.CloudyNight
    WeatherCondition.DRIZZLE,
    WeatherCondition.RAIN,
    WeatherCondition.RAIN_SHOWERS,
    -> Sky.Rainy
    WeatherCondition.SNOW,
    WeatherCondition.SNOW_SHOWERS,
    -> Sky.Snow
    WeatherCondition.THUNDERSTORM -> Sky.Storm
}

/** Veri yokken kullanılan gökyüzü: gündüz mavi, akşam lacivert. */
@Composable
fun rememberTimeOfDaySky(): Sky = remember {
    if (LocalTime.now().hour in 6..18) Sky.ClearDay else Sky.ClearNight
}
