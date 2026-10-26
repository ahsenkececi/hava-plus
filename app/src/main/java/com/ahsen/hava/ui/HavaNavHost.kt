package com.ahsen.hava.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ahsen.hava.ui.city.CityRoute
import com.ahsen.hava.ui.component.HavaBottomBar
import com.ahsen.hava.ui.component.HavaTab
import com.ahsen.hava.ui.forecast.ForecastRoute
import com.ahsen.hava.ui.trip.TripRoute

/**
 * Uygulamanın ekranları.
 *
 *  Alt menü:  [Hava]  [Yolculuk+]
 *                └─ Tahmin
 *
 * Alt menü sadece iki ana sekmede görünür; detay ekranlarında gizlenir.
 * Sekme değişince her sekmenin durumu saklanır (saveState/restoreState):
 * Yolculuk'ta seçtiğin rota, Hava'ya gidip dönünce kaybolmaz.
 *
 * @param openTripRequest widget'tan gelince artar: Yolculuk+ sekmesine geçilir.
 * @param openRoutineRequest rutin bildirimine dokunulunca artar: Yolculuk+ sekmesine geçilir ve rutin açılır.
 */
@Composable
fun HavaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openTripRequest: Int = 0,
    openRoutineRequest: Int = 0,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = when (backStackEntry?.destination?.route) {
        TRIP_ROUTE -> HavaTab.Trip
        CITY_ROUTE -> HavaTab.Weather
        else -> null
    }

    val selectTab: (HavaTab) -> Unit = { tab ->
        val route = if (tab == HavaTab.Trip) TRIP_ROUTE else CITY_ROUTE
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(openTripRequest, openRoutineRequest) {
        if (openTripRequest > 0 || openRoutineRequest > 0) selectTab(HavaTab.Trip)
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = CITY_ROUTE) {
            composable(TRIP_ROUTE) {
                TripRoute(onBack = null, openRoutineRequest = openRoutineRequest)
            }
            composable(CITY_ROUTE) {
                CityRoute(onCityClick = { city -> navController.navigate("$FORECAST?${CityArgs.query(CityInfo.from(city))}") })
            }
            composable(route = "$FORECAST?${CityArgs.TEMPLATE}", arguments = CityArgs.navArguments) {
                ForecastRoute(onBack = { navController.popBackStack() })
            }
        }

        if (currentTab != null) {
            HavaBottomBar(
                selected = currentTab,
                onSelect = selectTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
            )
        }
    }
}

/** Alt menünün kapladığı alan: listeler en alttaki öğe menünün altında kalmasın diye bu kadar boşluk bırakır. */
val BottomBarSpace = 96.dp

private const val TRIP_ROUTE = "trip"
private const val CITY_ROUTE = "city"
private const val FORECAST = "forecast"
