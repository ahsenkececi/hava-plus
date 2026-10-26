package com.ahsen.hava.ui.city

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsen.hava.R
import com.ahsen.hava.di.AppContainer
import com.ahsen.hava.domain.error.DataError
import com.ahsen.hava.domain.logic.RainOutlook
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.CurrentWeather
import com.ahsen.hava.domain.model.WeatherSnapshot
import com.ahsen.hava.domain.repository.CityRepository
import com.ahsen.hava.domain.repository.FavoritesRepository
import com.ahsen.hava.domain.repository.WeatherRepository
import com.ahsen.hava.domain.usecase.GetLocalWeatherUseCase
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.common.UiText
import com.ahsen.hava.ui.common.userMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Şehir ekranının beyni: state'i tutar, kararları verir.
 *
 * Arama metni ve seçili şehir [SavedStateHandle] içinde durur. Bu,
 * `rememberSaveable`'ın ViewModel'deki karşılığıdır: ekran dönünce de,
 * Android uygulamayı arka planda öldürüp geri açınca da kaybolmazlar.
 */
class CityViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val repository: CityRepository = AppContainer.cityRepository,
    private val favoritesRepository: FavoritesRepository = AppContainer.favoritesRepository,
    private val weatherRepository: WeatherRepository = AppContainer.weatherRepository,
    private val getLocalWeather: GetLocalWeatherUseCase = AppContainer.getLocalWeather,
) : ViewModel() {

    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    val selectedCityId: StateFlow<Long?> = savedStateHandle.getStateFlow<Long?>(KEY_SELECTED, null)

    private val _uiState = MutableStateFlow<LoadState<List<City>>>(
        LoadState.Success(repository.popularCities()),
    )
    val uiState: StateFlow<LoadState<List<City>>> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Favori şehirler: DataStore değiştikçe kendiliğinden güncellenir. */
    val favorites: StateFlow<List<City>> = favoritesRepository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Arama kutusunun altındaki "konumunda şimdi" kartı. */
    private val _localWeather = MutableStateFlow<LocalWeather>(LocalWeather.NeedsPermission)
    val localWeather: StateFlow<LocalWeather> = _localWeather.asStateFlow()

    /** Favori şehirlerin kısa hava özeti (şehir kimliği -> hava). Gökyüzü kartları için. */
    private val _favoriteWeather = MutableStateFlow<Map<Long, WeatherSnapshot>>(emptyMap())
    val favoriteWeather: StateFlow<Map<Long, WeatherSnapshot>> = _favoriteWeather.asStateFlow()

    init {
        // Favori listesi her değiştiğinde, havası henüz bilinmeyen şehirleri getir.
        viewModelScope.launch {
            favoritesRepository.favorites.collectLatest { list -> loadMissingWeather(list) }
        }
    }

    /** Hepsi aynı anda (async) istenir. Biri başarısız olursa o kart sadece sıcaklıksız kalır. */
    private suspend fun loadMissingWeather(cities: List<City>) {
        val missing = cities.filter { it.id !in _favoriteWeather.value }
        if (missing.isEmpty()) return
        val results = coroutineScope {
            missing.map { city ->
                async {
                    val weather = try {
                        weatherRepository.getSnapshot(city.latitude, city.longitude)
                    } catch (e: DataError) {
                        null
                    }
                    city.id to weather
                }
            }.awaitAll()
        }
        _favoriteWeather.update { current ->
            current + results.mapNotNull { (id, weather) -> weather?.let { id to it } }
        }
    }

    init {
        // Uygulama geri açıldığında kutuda yazı kalmışsa sonuçları tekrar getir.
        if (query.value.isNotBlank()) search()
    }

    /** Her harfte çağrılır. Kullanıcı yazmayı bırakınca (400 ms) arama başlar. */
    fun onQueryChange(newQuery: String) {
        savedStateHandle[KEY_QUERY] = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.value = LoadState.Success(repository.popularCities())
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(newQuery)
        }
    }

    /** Klavyedeki "Ara" tuşu ve hata ekranındaki "Tekrar dene" bunu çağırır: beklemeden arar. */
    fun search() {
        searchJob?.cancel()
        val current = query.value
        if (current.isBlank()) {
            _uiState.value = LoadState.Success(repository.popularCities())
            return
        }
        searchJob = viewModelScope.launch { runSearch(current) }
    }

    /** Asıl arama. Sonuç yoksa Empty, hata olursa kullanıcının anlayacağı bir mesaj. */
    private suspend fun runSearch(text: String) {
        _uiState.value = LoadState.Loading
        _uiState.value = try {
            val cities = repository.search(text)
            if (cities.isEmpty()) LoadState.Empty else LoadState.Success(cities)
        } catch (e: DataError) {
            Log.e(TAG, "Arama başarısız", e)
            LoadState.Error(e.userMessage())
        }
    }

    fun onCityClick(city: City) {
        Log.d(TAG, "Tıklandı: ${city.name}")
        savedStateHandle[KEY_SELECTED] = city.id
    }

    fun onToggleFavorite(city: City) {
        viewModelScope.launch { favoritesRepository.toggle(city) }
    }

    /**
     * Ekran izin sonucunu bildirir. İzin varsa: konumu bul, o yerin tahminini getir.
     * Kart bu sırada "yükleniyor", sonra o yerin havasını gösterir.
     */
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            _localWeather.value = LocalWeather.NeedsPermission
            return
        }
        if (_localWeather.value == LocalWeather.Loading) return
        viewModelScope.launch {
            _localWeather.value = LocalWeather.Loading
            _localWeather.value = try {
                getLocalWeather()
                    ?.let { LocalWeather.Ready(it.city, it.current, it.rainOutlook, it.todayMinC, it.todayMaxC) }
                    ?: LocalWeather.Failed(UiText(R.string.error_location_not_found))
            } catch (e: SecurityException) {
                Log.e(TAG, "Konum izni yok", e)
                LocalWeather.NeedsPermission
            } catch (e: DataError) {
                Log.e(TAG, "Konumdaki hava alınamadı", e)
                LocalWeather.Failed(e.userMessage(service = R.string.service_weather))
            }
        }
    }

    /** "Konumunda şimdi" kartının durumları. */
    sealed interface LocalWeather {
        data object NeedsPermission : LocalWeather
        data object Loading : LocalWeather
        data class Ready(
            val city: City,
            val current: CurrentWeather,
            val rainOutlook: RainOutlook?,
            val todayMinC: Double? = null,
            val todayMaxC: Double? = null,
        ) : LocalWeather
        data class Failed(val message: UiText) : LocalWeather
    }

    private companion object {
        const val TAG = "CityViewModel"
        const val KEY_QUERY = "query"
        const val KEY_SELECTED = "selectedCityId"
        const val DEBOUNCE_MS = 400L
    }
}
