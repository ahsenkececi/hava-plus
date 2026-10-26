package com.ahsen.hava.domain.repository

import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.TripRoutes

/** Rota kapısı. */
interface RouteRepository {
    suspend fun routes(from: Place, to: Place): TripRoutes
}
