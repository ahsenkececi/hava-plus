package com.ahsen.hava.ui.trip

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahsen.hava.R
import com.ahsen.hava.ui.common.durationText

/*
 * Yolculuk+ ekranlarının ortak biçimlendiricileri (süre ve mesafe). Birimler strings.xml'de.
 */

/** 0,8 km → "800 m", 9,2 km → "9,2 km" (ondalık ayracı cihazın diline göre). */
@Composable
internal fun formatKm(km: Double): String =
    if (km < 1) stringResource(R.string.distance_meters, (km * 1000).toInt()) else stringResource(R.string.distance_km, km)

@Composable
internal fun formatMinutes(minutes: Int): String = durationText(minutes)
