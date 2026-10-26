package com.ahsen.hava.ui.trip

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsen.hava.R
import com.ahsen.hava.di.AppContainer
import com.ahsen.hava.domain.error.DataError
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.RouteWeatherTimeline
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.domain.repository.LocationRepository
import com.ahsen.hava.domain.repository.PlaceRepository
import com.ahsen.hava.domain.repository.RoutineRepository
import com.ahsen.hava.domain.usecase.PlanTripUseCase
import com.ahsen.hava.domain.usecase.TripPlan
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.common.userMessage
import java.time.LocalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Hangi kutuya yazılıyor? */
enum class TripField { Origin, Destination }

/** Rutin düzenleme sayfası açıkken: başlangıç değerleri ve seçilebilecek ulaşım türleri. */
data class RoutineEditor(val initial: Routine, val modes: List<TravelMode>, val isExisting: Boolean)

/**
 * Yolculuk planlayıcının beyni: nereden–nereye seçimi, rotalar, yol boyunca hava ve karar.
 * İş akışının kendisi [PlanTripUseCase]'de; burası sadece ekran durumunu tutar.
 *
 * Başlangıç noktası null ise "Konumum" demektir; gerçek koordinat [myLocation]'dan gelir.
 */
class TripViewModel(
    private val placeRepository: PlaceRepository = AppContainer.placeRepository,
    private val locationRepository: LocationRepository = AppContainer.locationRepository,
    private val planTrip: PlanTripUseCase = AppContainer.planTrip,
    private val routineRepository: RoutineRepository = AppContainer.routineRepository,
) : ViewModel() {

    private val _origin = MutableStateFlow<Place?>(null)
    val origin: StateFlow<Place?> = _origin.asStateFlow()

    private val _destination = MutableStateFlow<Place?>(null)
    val destination: StateFlow<Place?> = _destination.asStateFlow()

    /** Kullanıcının şu anki konumu (izin verildiyse). "Konumum" seçiliyken başlangıç budur. */
    private val _myLocation = MutableStateFlow<Place?>(null)
    val myLocation: StateFlow<Place?> = _myLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _activeField = MutableStateFlow<TripField?>(TripField.Destination)
    val activeField: StateFlow<TripField?> = _activeField.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** null: henüz bir şey yazılmadı (liste gösterilmez). */
    private val _results = MutableStateFlow<LoadState<List<Place>>?>(null)
    val results: StateFlow<LoadState<List<Place>>?> = _results.asStateFlow()

    private var searchJob: Job? = null

    /** İki nokta da hazırsa rotalar; değilse null. */
    private val _routes = MutableStateFlow<LoadState<TripRoutes>?>(null)
    val routes: StateFlow<LoadState<TripRoutes>?> = _routes.asStateFlow()

    /** Her ulaşım türünün en iyi rotası için hava zaman çizelgesi. Rotalardan sonra, ayrıca yüklenir. */
    private val _timelines = MutableStateFlow<Map<TravelMode, RouteWeatherTimeline>>(emptyMap())

    /** Seçilen çıkış: şimdiden kaç dakika sonra. 0 = şimdi. */
    private val _departureOffset = MutableStateFlow(0)
    val departureOffset: StateFlow<Int> = _departureOffset.asStateFlow()

    /** Seçilen çıkış saatine göre yol boyunca hava (ağa gitmeden, çizelgeden hesaplanır). */
    private val routeWeather = combine(_timelines, _departureOffset) { timelines, offset ->
        timelines.mapValues { it.value.at(offset) }
    }

    /**
     * Ekranın göstereceği karar (öneri + çıkış saati). Rota, hava ya da çıkış saati değiştikçe
     * yeniden hesaplanır. Rotalar henüz yoksa null.
     */
    val plan: StateFlow<TripPlan?> =
        combine(_routes, routeWeather, _timelines) { routes, weather, timelines ->
            val trip = when (routes) {
                is LoadState.Success -> routes.data
                else -> null
            }
            trip?.let { planTrip.evaluate(it, weather, timelines) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    init {
        // Başlangıç (ya da Konumum) veya varış değişince rotaları yeniden hesapla.
        // collectLatest: kullanıcı hızlıca değiştirirse eski istek iptal edilir.
        viewModelScope.launch {
            combine(_origin, _myLocation, _destination) { origin, me, destination -> (origin ?: me) to destination }
                .distinctUntilChanged()
                .collectLatest { (from, to) -> loadRoutes(from, to) }
        }
    }

    private suspend fun loadRoutes(from: Place?, to: Place?) {
        if (from == null || to == null) {
            _routes.value = null
            return
        }
        _routes.value = LoadState.Loading
        _timelines.value = emptyMap()
        _departureOffset.value = 0
        _routes.value = try {
            val trip = planTrip.findRoutes(from, to)
            loadRouteWeather(trip)
            LoadState.Success(trip)
        } catch (e: DataError) {
            Log.e(TAG, "Rota alınamadı", e)
            LoadState.Error(e.userMessage(service = R.string.service_routing))
        }
    }

    /**
     * Rota kartları hemen görünsün, hava bir saniye sonra gelsin: ayrı bir coroutine'de.
     * Hava alınamazsa kartlar yine çalışır, sadece hava şeridi görünmez.
     */
    private fun loadRouteWeather(trip: TripRoutes) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            _timelines.value = try {
                planTrip.weatherAlong(trip)
            } catch (e: DataError) {
                Log.e(TAG, "Rota havası alınamadı", e)
                emptyMap()
            }
            _isWeatherLoading.value = false
        }
    }

    /** Zaman şeridinde bir çıkış saatine dokunuldu. */
    fun onDepartureChange(offsetMinutes: Int) {
        _departureOffset.value = offsetMinutes
    }

    fun onRetryRoutes() {
        val from = resolvedOrigin
        val to = _destination.value
        viewModelScope.launch { loadRoutes(from, to) }
    }

    /** Gerçek başlangıç noktası: seçilmiş bir yer ya da Konumum. */
    val resolvedOrigin: Place? get() = _origin.value ?: _myLocation.value

    fun onFieldClick(field: TripField) {
        _activeField.value = field
        _query.value = ""
        _results.value = null
        searchJob?.cancel()
    }

    fun onQueryChange(text: String) {
        _query.value = text
        searchJob?.cancel()
        if (text.trim().length < MIN_QUERY) {
            _results.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _results.value = LoadState.Loading
            _results.value = try {
                val places = placeRepository.search(text, near = _myLocation.value)
                if (places.isEmpty()) LoadState.Empty else LoadState.Success(places)
            } catch (e: DataError) {
                Log.e(TAG, "Yer araması başarısız", e)
                LoadState.Error(e.userMessage(service = R.string.service_places))
            }
        }
    }

    fun onPlaceSelected(place: Place) {
        when (_activeField.value) {
            TripField.Origin -> _origin.value = place
            TripField.Destination -> _destination.value = place
            null -> Unit
        }
        closeSearch()
    }

    /** Başlangıcı tekrar "Konumum" yap. */
    fun onUseMyLocation() {
        _origin.value = null
        closeSearch()
    }

    /** Nereden ile nereye yer değiştirir (dönüş yolculuğu). */
    fun onSwap() {
        val from = resolvedOrigin ?: return
        val to = _destination.value ?: return
        _origin.value = to
        _destination.value = from
    }

    /** Ekran izin sonucunu bildirir; izin varsa konumu bul. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted || _myLocation.value != null || _isLocating.value) return
        viewModelScope.launch {
            _isLocating.value = true
            val city = try {
                locationRepository.currentCity()
            } catch (e: SecurityException) {
                null
            }
            _myLocation.value = city?.let { Place(it.name, it.subtitle, it.latitude, it.longitude) }
            _isLocating.value = false
        }
    }

    // ------------------------------------------------------------------ Rutinim

    /** Kayıtlı rutin. Kaydetmek/silmek yeter: arka plan kontrolünü [com.ahsen.hava.routine.RoutineSync] kurar. */
    val routine: StateFlow<Routine?> = routineRepository.routine
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _routineEditor = MutableStateFlow<RoutineEditor?>(null)
    val routineEditor: StateFlow<RoutineEditor?> = _routineEditor.asStateFlow()

    /**
     * Rutin sayfasını açar. Ekranda bir yolculuk varsa rutin o yolculuk için kurulur
     * (önerilen ulaşım türüyle); yoksa kayıtlı rutin düzenlenir.
     */
    fun onEditRoutine() {
        val existing = routine.value
        val from = resolvedOrigin
        val to = _destination.value
        val modes = plan.value?.recommendation?.options?.map { it.mode }.orEmpty()
        _routineEditor.value = when {
            from != null && to != null -> {
                val sameTrip = existing?.takeIf { it.from == from && it.to == to }
                RoutineEditor(
                    initial = Routine(
                        from = from,
                        to = to,
                        mode = sameTrip?.mode ?: plan.value?.recommendation?.best?.mode ?: TravelMode.Bike,
                        days = existing?.days ?: Routine.WEEKDAYS,
                        departure = existing?.departure ?: DEFAULT_DEPARTURE,
                    ),
                    modes = modes.ifEmpty { TravelMode.entries },
                    isExisting = sameTrip != null,
                )
            }
            existing != null -> RoutineEditor(existing, TravelMode.entries, isExisting = true)
            else -> null
        }
    }

    fun onDismissRoutineEditor() {
        _routineEditor.value = null
    }

    fun onSaveRoutine(routine: Routine) {
        _routineEditor.value = null
        viewModelScope.launch { routineRepository.save(routine.copy(enabled = true)) }
    }

    fun onDeleteRoutine() {
        _routineEditor.value = null
        viewModelScope.launch { routineRepository.clear() }
    }

    fun onRoutineEnabledChange(enabled: Boolean) {
        val current = routine.value ?: return
        viewModelScope.launch { routineRepository.save(current.copy(enabled = enabled)) }
    }

    /** Rutin yolculuğunu ekrana getirir (rutin kartına ya da bildirime dokunulunca). */
    fun onOpenRoutine() {
        viewModelScope.launch {
            // Bildirimden gelindiyse kayıt henüz okunmamış olabilir: ilk değeri bekle.
            val saved = routineRepository.routine.first() ?: return@launch
            _origin.value = saved.from
            _destination.value = saved.to
            closeSearch()
        }
    }

    private fun closeSearch() {
        searchJob?.cancel()
        _query.value = ""
        _results.value = null
        // Başlangıç seçildiyse sıra varışa geçsin; ikisi de hazırsa arama kapansın.
        _activeField.value = if (_destination.value == null) TripField.Destination else null
    }

    private companion object {
        const val TAG = "TripViewModel"
        const val MIN_QUERY = 3
        const val DEBOUNCE_MS = 450L
        val DEFAULT_DEPARTURE: LocalTime = LocalTime.of(8, 0)
    }
}
