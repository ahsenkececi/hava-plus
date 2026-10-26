package com.ahsen.hava.routine

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahsen.hava.domain.model.Routine
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Bir sonraki kontrol: hangi çıkış için, şimdiden ne kadar sonra. */
data class RoutineCheck(val departure: LocalDateTime, val delay: Duration)

/**
 * Rutin kontrolünü WorkManager'a kurar. WorkManager işi telefon yeniden başlasa bile
 * hatırlar ve pil tasarrufu kurallarına uyar.
 *
 * Tek seferlik iş zinciri: her kontrol çıkıştan 30 dk önce çalışır, bitince bir sonrakini kurar.
 * (Periyodik iş belirli bir saate denk getirilemez; bu yüzden tek seferlik.)
 */
class RoutineScheduler(private val context: Context) {

    /**
     * @param routine null ya da kapalıysa bekleyen kontrol iptal edilir
     * @param policy [ExistingWorkPolicy.REPLACE]: rutin değişti, eskisini at.
     *   [ExistingWorkPolicy.KEEP]: uygulama açıldı, bekleyen iş varsa dokunma.
     *   [ExistingWorkPolicy.APPEND_OR_REPLACE]: çalışan iş kendinden sonrakini kuruyor.
     */
    fun schedule(
        routine: Routine?,
        policy: ExistingWorkPolicy,
        after: LocalDateTime = LocalDateTime.now(),
    ) {
        val workManager = WorkManager.getInstance(context)
        val check = routine?.let { nextCheck(it, after, LocalDateTime.now()) }
        if (check == null) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = OneTimeWorkRequestBuilder<RoutineWorker>()
            .setInitialDelay(check.delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(RoutineWorker.KEY_DEPARTURE to check.departure.toString()))
            // Hava ve rota için internet şart; yoksa WorkManager bağlantı gelince çalıştırır.
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, policy, request)
    }

    companion object {
        private const val WORK_NAME = "routine-check"

        /**
         * [after]'dan sonraki ilk çıkış ve kontrolüne kalan süre. Kontrol saati geçmiş ama çıkış
         * henüz gelmemişse (ör. rutin çıkışa 10 dk kala kaydedildi) hemen kontrol edilir.
         */
        fun nextCheck(routine: Routine, after: LocalDateTime, now: LocalDateTime): RoutineCheck? {
            val departure = routine.nextDeparture(after) ?: return null
            val delay = Duration.between(now, routine.checkTimeFor(departure))
            return RoutineCheck(departure, if (delay.isNegative) Duration.ZERO else delay)
        }
    }
}
