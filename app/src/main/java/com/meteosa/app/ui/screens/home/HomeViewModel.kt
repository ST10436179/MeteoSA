package com.meteosa.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meteosa.app.data.local.db.CachedDailyForecast
import com.meteosa.app.data.local.db.SavedLocationEntity
import com.meteosa.app.data.local.db.WeatherCacheEntity
import com.meteosa.app.data.remote.dto.CurrentWeatherResponse
import com.meteosa.app.data.remote.dto.MainWeather
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.remote.dto.WeatherCondition
import com.meteosa.app.data.remote.dto.Wind
import com.meteosa.app.data.repository.DataSaverRepository
import com.meteosa.app.data.repository.ReportsRepository
import com.meteosa.app.data.repository.SavedLocationsRepository
import com.meteosa.app.data.repository.WeatherRepository
import com.meteosa.app.util.UiState
import com.meteosa.app.util.toUserMessage
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiData(
    val current: CurrentWeatherResponse,
    val daily: List<DailyForecast>,
    /** True when this is cached data served because the live fetch failed (offline mode). */
    val isFromCache: Boolean = false
)

class HomeViewModel(
    private val weatherRepository: WeatherRepository,
    private val dataSaverRepository: DataSaverRepository,
    private val reportsRepository: ReportsRepository,
    private val savedLocationsRepository: SavedLocationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeUiData>>(UiState.Idle)
    val uiState: StateFlow<UiState<HomeUiData>> = _uiState.asStateFlow()

    private val _locationLabel = MutableStateFlow("Johannesburg (default)")
    val locationLabel: StateFlow<String> = _locationLabel.asStateFlow()

    /** User Defined feature (Part 3): named locations the user has saved for quick access. */
    val savedLocations: StateFlow<List<SavedLocationEntity>> = savedLocationsRepository.savedLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

    /** [savedLocationName], when set, overrides the geocoded city name in [locationLabel] - used
     *  when the weather being shown is for a user-saved location rather than GPS/default. */
    fun loadWeather(lat: Double, lon: Double, usingDeviceLocation: Boolean, savedLocationName: String? = null) {
        currentLat = lat
        currentLon = lon
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val current = weatherRepository.getCurrentWeather(lat, lon)
                // Prefer One Call 3.0 (a real 5+ day forecast). If the key lacks that
                // subscription or the call fails, fall back to the 5-day/3-hour endpoint.
                val daily = try {
                    weatherRepository.getDailyForecast(lat, lon).toDailyForecasts()
                        .also { check(it.size > 1) { "One Call returned too few days" } }
                } catch (e: Exception) {
                    android.util.Log.w("HomeViewModel", "One Call forecast unavailable, falling back: ${e.message}")
                    weatherRepository.getForecast(lat, lon).toDailyForecasts()
                }
                _locationLabel.value = when {
                    savedLocationName != null -> savedLocationName
                    usingDeviceLocation -> current.name
                    else -> "${current.name} (default)"
                }
                weatherRepository.cacheWeather(toCacheEntity(current, daily))
                UiState.Success(HomeUiData(current, daily, isFromCache = false))
            } catch (t: Throwable) {
                // Offline-first fallback (Part 1 design doc): if the live call fails, serve the
                // last successfully cached weather instead of just showing an error screen.
                val cached = weatherRepository.getCachedWeather()
                if (cached != null) {
                    _locationLabel.value = "${cached.locationName} (offline)"
                    UiState.Success(HomeUiData(cached.toCurrentWeatherResponse(), cached.toDailyForecastList(), isFromCache = true))
                } else {
                    UiState.Error(t.toUserMessage())
                }
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

    fun saveCurrentLocation(name: String) {
        viewModelScope.launch {
            savedLocationsRepository.save(name, currentLat, currentLon)
        }
    }

    fun deleteSavedLocation(location: SavedLocationEntity) {
        viewModelScope.launch {
            savedLocationsRepository.delete(location)
        }
    }

    fun selectSavedLocation(location: SavedLocationEntity) {
        loadWeather(location.latitude, location.longitude, usingDeviceLocation = false, savedLocationName = location.name)
    }
}

private val gson = Gson()

private fun toCacheEntity(current: CurrentWeatherResponse, daily: List<DailyForecast>): WeatherCacheEntity {
    val cachedDaily = daily.map { CachedDailyForecast(it.dayLabel, it.icon, it.minTemp, it.maxTemp) }
    val condition = current.weather.firstOrNull()
    return WeatherCacheEntity(
        locationName = current.name,
        temperature = current.main.temp,
        feelsLike = current.main.feelsLike,
        humidity = current.main.humidity,
        windSpeed = current.wind.speed,
        conditionDescription = condition?.description ?: "",
        conditionIcon = condition?.icon ?: "01d",
        conditionId = condition?.id ?: 800,
        dailyForecastJson = gson.toJson(cachedDaily),
        timestamp = System.currentTimeMillis()
    )
}

private fun WeatherCacheEntity.toCurrentWeatherResponse() = CurrentWeatherResponse(
    name = locationName,
    weather = listOf(WeatherCondition(conditionId, "", conditionDescription, conditionIcon)),
    main = MainWeather(temp = temperature, feelsLike = feelsLike, tempMin = temperature, tempMax = temperature, humidity = humidity),
    wind = Wind(windSpeed),
    dt = timestamp / 1000
)

private fun WeatherCacheEntity.toDailyForecastList(): List<DailyForecast> {
    // Decode as an array: a TypeToken subclass gets stripped by R8 in release builds and
    // crashes ("Missing type parameter") on the offline-cache path.
    val cached: List<CachedDailyForecast> =
        gson.fromJson(dailyForecastJson, Array<CachedDailyForecast>::class.java)?.toList() ?: emptyList()
    return cached.map { DailyForecast(it.dayLabel, it.icon, it.minTemp, it.maxTemp) }
}
