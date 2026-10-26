package com.ahsen.hava.ui.forecast

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsen.hava.di.AppContainer
import com.ahsen.hava.domain.error.DataError
import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.repository.FavoritesRepository
import com.ahsen.hava.domain.repository.SettingsRepository
import com.ahsen.hava.domain.repository.WeatherRepository
import com.ahsen.hava.ui.CityInfo
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.common.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tahmin ekranının beyni. Hangi şehir olduğunu navigasyondan öğrenir:
 * rota parametreleri [SavedStateHandle] içinde gelir.
 */
class ForecastViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: WeatherRepository = AppContainer.weatherRepository,
    private val favoritesRepository: FavoritesRepository = AppContainer.favoritesRepository,
    private val settingsRepository: SettingsRepository = AppContainer.settingsRepository,
) : ViewModel() {

    /** Hangi şehir: rotadan gelir. */
    val city: CityInfo = CityInfo.from(savedStateHandle)

    /** Bu şehir favorilerde mi? Favoriler değiştikçe güncellenir. */
    val isFavorite: StateFlow<Boolean> = favoritesRepository.favorites
        .map { list -> list.any { it.id == city.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Hava sesleri açık mı? (Üst köşedeki hoparlör düğmesi; tercih hatırlanır.) */
    val soundEnabled: StateFlow<Boolean> = settingsRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow<LoadState<Forecast>>(LoadState.Loading)
    val uiState: StateFlow<LoadState<Forecast>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onToggleSound() {
        viewModelScope.launch { settingsRepository.setSoundEnabled(!soundEnabled.value) }
    }

    fun onToggleFavorite() {
        viewModelScope.launch { favoritesRepository.toggle(city.toCity()) }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = LoadState.Loading
            _uiState.value = try {
                val forecast = repository.getForecast(city.latitude, city.longitude)
                if (forecast.daily.isEmpty() && forecast.hourly.isEmpty()) {
                    LoadState.Empty
                } else {
                    LoadState.Success(forecast)
                }
            } catch (e: DataError) {
                Log.e(TAG, "Tahmin alınamadı", e)
                LoadState.Error(e.userMessage())
            }
        }
    }

    private companion object {
        const val TAG = "ForecastViewModel"
    }
}
