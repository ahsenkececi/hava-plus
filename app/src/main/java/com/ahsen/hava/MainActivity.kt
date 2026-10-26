package com.ahsen.hava

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahsen.hava.ui.HavaNavHost
import com.ahsen.hava.ui.theme.HavaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {

    /** Widget'taki Yolculuk+ satırına her dokunuşta bir artar: Yolculuk+ sekmesi açılır. */
    private val openTripRequests = MutableStateFlow(0)

    /** Rutin bildirimine (ya da widget'taki rutin satırına) her dokunuşta bir artar: rutin rotası açılır. */
    private val openRoutineRequests = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Açılış ekranı: super.onCreate'ten ÖNCE kurulmalı.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Açılış ekranı kaybolurken yumuşakça solsun.
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(350L)
                .withEndAction { provider.remove() }
                .start()
        }
        // Arka planımız hep koyu tonlu gradyan: durum çubuğu ikonları her zaman beyaz olsun.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Ekran döndüğünde (savedInstanceState dolu) aynı bildirim ikinci kez işlenmesin.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val openTripRequest by openTripRequests.collectAsStateWithLifecycle()
            val openRoutineRequest by openRoutineRequests.collectAsStateWithLifecycle()
            HavaTheme {
                HavaNavHost(openTripRequest = openTripRequest, openRoutineRequest = openRoutineRequest)
            }
        }
    }

    /** Uygulama zaten açıkken bildirime dokunulursa buraya gelir. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            AppActions.OPEN_TRIP -> openTripRequests.update { it + 1 }
            AppActions.OPEN_ROUTINE -> openRoutineRequests.update { it + 1 }
        }
    }
}
