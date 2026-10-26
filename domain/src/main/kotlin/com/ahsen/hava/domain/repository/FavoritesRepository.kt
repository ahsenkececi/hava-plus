package com.ahsen.hava.domain.repository

import com.ahsen.hava.domain.model.City
import kotlinx.coroutines.flow.Flow

/** Favori şehirler. Uygulama kapanıp açılsa da kalır. */
interface FavoritesRepository {
    /** Değiştikçe yeni listeyi yayar (Flow): ekran kendiliğinden güncellenir. */
    val favorites: Flow<List<City>>

    /** Favorideyse çıkarır, değilse en başa ekler. */
    suspend fun toggle(city: City)
}
