package com.ahsen.hava.routine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.ahsen.hava.di.AppContainer
import com.ahsen.hava.domain.error.DataError
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.first

/**
 * Çıkıştan 30 dk önce çalışır: rutin yolun havasına bakar, söylenecek bir şey varsa bildirir,
 * sonra bir sonraki çıkışın kontrolünü kurar. İş kuralları domain'de ([CheckRoutineUseCase]);
 * burası sadece Android tarafının yapıştırıcısı.
 */
class RoutineWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val routine = AppContainer.routineRepository.routine.first() ?: return Result.success()
        val departure = inputData.getString(KEY_DEPARTURE)?.let(::parseOrNull) ?: LocalDateTime.now()

        try {
            val report = AppContainer.checkRoutine.check(routine, departure)
            if (report != null && report.isNotable) {
                RoutineNotifier(applicationContext).show(routine, departure, report)
            }
        } catch (e: DataError) {
            // Servis anlık cevap vermediyse bir kez daha dene; yine olmazsa bu çıkışı atla.
            if (runAttemptCount < MAX_RETRIES) return Result.retry()
        }

        RoutineScheduler(applicationContext).schedule(routine, ExistingWorkPolicy.APPEND_OR_REPLACE, after = departure)
        return Result.success()
    }

    private fun parseOrNull(text: String): LocalDateTime? =
        try {
            LocalDateTime.parse(text)
        } catch (e: DateTimeParseException) {
            null
        }

    companion object {
        const val KEY_DEPARTURE = "departure"
        private const val MAX_RETRIES = 2
    }
}
