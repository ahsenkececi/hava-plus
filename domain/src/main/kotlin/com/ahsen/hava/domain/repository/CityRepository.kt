package com.ahsen.hava.domain.repository

import com.ahsen.hava.domain.model.City

/**
 * Şehir verisinin tek kapısı (Bonus: araya CityRepository ekle).
 * ViewModel verinin nereden geldiğini bilmez; testlerde sahtesi verilebilir.
 */
interface CityRepository {
    fun popularCities(): List<City>
    suspend fun search(query: String): List<City>
}
