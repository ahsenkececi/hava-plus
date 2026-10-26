package com.ahsen.hava.domain.repository

import com.ahsen.hava.domain.model.Route
import com.ahsen.hava.domain.model.RouteWeatherTimeline
import java.time.LocalDateTime

/** Rota boyunca hava kapısı. */
interface RouteWeatherRepository {
    /**
     * Her rota için bir zaman çizelgesi: tahmin bir kez indirilir, sonra istenen her çıkış saati
     * için yol boyunca hava yeni istek atmadan hesaplanır ("Ne zaman çıkmalıyım?").
     */
    suspend fun timelines(routes: List<Route>, now: LocalDateTime): Map<Route, RouteWeatherTimeline>
}
