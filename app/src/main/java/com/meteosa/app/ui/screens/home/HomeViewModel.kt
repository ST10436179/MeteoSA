package com.meteosa.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meteosa.app.data.remote.dto.CurrentWeatherResponse
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.repository.DataSaverRepository
import com.meteosa.app.data.repository.ReportsRepository
import com.meteosa.app.data.repository.WeatherRepository
import com.meteosa.app.util.UiState
import com.meteosa.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiData(
    val current: CurrentWeatherResponse,
    val daily: List<DailyForecast>
)

class HomeViewModel(
    private val weatherRepository: WeatherRepository,
    private val dataSaverRepository: DataSaverRepository,
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeUiData>>(UiState.Idle)
    val uiState: StateFlow<UiState<HomeUiData>> = _uiState.asStateFlow()

    private val _locationLabel = MutableStateFlow("Johannesburg (default)")
    val locationLabel: StateFlow<String> = _locationLabel.asStateFlow()

    // Nearby community reports for the Home dashboard. Intentionally a plain list rather than a
    // UiState: this is a secondary, best-effort widget on Home, not the screen's primary content,
    // so a failed fetch just leaves it empty instead of showing an error over the weather UI.
    private val _nearbyReports = MutableStateFlow<List<ReportDto>>(emptyList())
    val nearbyReports: StateFlow<List<ReportDto>> = _nearbyReports.asStateFlow()

    private val _dataSaverActive = MutableStateFlow(false)
    /** True when Data-Saver Mode should be showing (Part 1's restricted-data/load-shedding-aware UI). */
    val dataSaverActive: StateFlow<Boolean> = _dataSaverActive.asStateFlow()

    private var manualDataSaverOverride = false

    init {
        viewModelScope.launch {
            dataSaverRepository.manualOverride.collect { manual ->
                manualDataSaverOverride = manual
                recomputeDataSaverState()
            }
        }
    }

    private fun recomputeDataSaverState() {
        viewModelScope.launch {
            val loadSheddingActive = dataSaverRepository.isLoadSheddingActive()
            _dataSaverActive.value =
                manualDataSaverOverride || dataSaverRepository.isRestrictedNetwork() || loadSheddingActive
        }
    }

    fun loadWeather(lat: Double, lon: Double, usingDeviceLocation: Boolean) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val current = weatherRepository.getCurrentWeather(lat, lon)
                val forecast = weatherRepository.getForecast(lat, lon)
                _locationLabel.value = if (usingDeviceLocation) current.name else "${current.name} (default)"
                UiState.Success(HomeUiData(current, forecast.toDailyForecasts()))
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
        viewModelScope.launch {
            _nearbyReports.value = try {
                reportsRepository.getReports(lat, lon).take(3)
            } catch (t: Throwable) {
                emptyList()
            }
        }
    }
}
