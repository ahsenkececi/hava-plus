package com.ahsen.hava.ui.forecast

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Tahmin ekranlarının ortak biçimlendiricileri.
 */

internal fun Double.asDegree(): String = "${roundToInt()}°"

internal val TURKISH: Locale = Locale.forLanguageTag("tr")
internal val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
