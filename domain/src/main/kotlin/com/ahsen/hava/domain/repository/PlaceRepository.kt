package com.ahsen.hava.domain.repository

import com.ahsen.hava.domain.model.Place

/** Yer arama kapısı. */
interface PlaceRepository {
    /** [near] verilirse yakındaki sonuçlar öne gelir. */
    suspend fun search(query: String, near: Place? = null): List<Place>
}
