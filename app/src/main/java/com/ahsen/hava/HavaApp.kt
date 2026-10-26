package com.ahsen.hava

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.ahsen.hava.di.AppContainer
import com.ahsen.hava.routine.RoutineScheduler
import com.ahsen.hava.routine.RoutineSync
import com.ahsen.hava.widget.HavaWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Uygulamanın kendisi. Activity'den önce bir kez oluşturulur.
 * Favoriler (DataStore) ve konum servisi uygulama genelinde Context istediği için
 * onları burada bir kez hazırlıyoruz. Manifest'te android:name=".HavaApp" ile bağlı.
 */
class HavaApp : Application() {

    /** Uygulama yaşadıkça süren işler için (ekranlardan bağımsız). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        // Rutin kaydedildikçe/silindikçe arka plan kontrolü kendiliğinden kurulur.
        RoutineSync(AppContainer.routineRepository, RoutineScheduler(this), appScope).start()
        // Uygulama açılınca widget da tazelensin (ekranda widget yoksa hiçbir şey yapmaz).
        appScope.launch { HavaWidget().updateAll(this@HavaApp) }
    }
}
