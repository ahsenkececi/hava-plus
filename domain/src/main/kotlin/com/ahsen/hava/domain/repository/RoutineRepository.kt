package com.ahsen.hava.domain.repository

import com.ahsen.hava.domain.model.Routine
import kotlinx.coroutines.flow.Flow

/** Kullanıcının tek düzenli yolculuğu ("Rutinim"). Yoksa null. */
interface RoutineRepository {
    val routine: Flow<Routine?>
    suspend fun save(routine: Routine)
    suspend fun clear()
}
