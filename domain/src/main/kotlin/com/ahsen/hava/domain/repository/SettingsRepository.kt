package com.ahsen.hava.domain.repository

import kotlinx.coroutines.flow.Flow

/** Kullanıcı tercihleri. Şimdilik tek bir tane: hava sesleri açık mı? */
interface SettingsRepository {
    val soundEnabled: Flow<Boolean>
    suspend fun setSoundEnabled(enabled: Boolean)
}
