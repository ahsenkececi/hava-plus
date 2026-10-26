package com.ahsen.hava.routine

import androidx.work.ExistingWorkPolicy
import com.ahsen.hava.domain.repository.RoutineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Rutin değiştikçe kontrolü yeniden kurar. Ekranların zamanlamayı bilmesine gerek kalmaz:
 * rutini kaydetmek ya da silmek yeter.
 *
 * İlk değer (uygulama açılışı) bekleyen işe dokunmaz: bildirime dokunup uygulamayı açmak
 * aynı çıkışı yeniden kontrol edip ikinci bir bildirim göndermesin.
 */
class RoutineSync(
    private val repository: RoutineRepository,
    private val scheduler: RoutineScheduler,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            var first = true
            repository.routine.distinctUntilChanged().collect { routine ->
                scheduler.schedule(routine, if (first) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE)
                first = false
            }
        }
    }
}
