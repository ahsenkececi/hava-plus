package com.ahsen.hava.domain.repository

import com.ahsen.hava.domain.model.City

/** Bulunduğun yer. İzin verilmemişse ya da konum bulunamazsa null. */
interface LocationRepository {
    suspend fun currentCity(): City?

    /**
     * En son başarıyla bulunan konum (cihazda saklanır). GPS'e gitmez: arka planda
     * (widget gibi) konum izni ya da zamanı olmayan yerlerde kullanılır. Hiç bulunmadıysa null.
     */
    suspend fun lastKnownCity(): City?
}
