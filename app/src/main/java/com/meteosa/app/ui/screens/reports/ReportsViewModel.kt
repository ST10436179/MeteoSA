package com.meteosa.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.repository.ReportsRepository
import com.meteosa.app.util.UiState
import com.meteosa.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportsViewModel(private val reportsRepository: ReportsRepository) : ViewModel() {

    private val _reportsState = MutableStateFlow<UiState<List<ReportDto>>>(UiState.Idle)
    val reportsState: StateFlow<UiState<List<ReportDto>>> = _reportsState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<Int>>(UiState.Idle)
    val submitState: StateFlow<UiState<Int>> = _submitState.asStateFlow()

    fun loadReports(lat: Double? = null, lon: Double? = null) {
        _reportsState.value = UiState.Loading
        viewModelScope.launch {
            _reportsState.value = try {
                UiState.Success(reportsRepository.getReports(lat, lon))
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun submitReport(lat: Double, lon: Double, reportType: String, description: String) {
        if (description.isBlank()) {
            _submitState.value = UiState.Error("Please add a short description.")
            return
        }
        _submitState.value = UiState.Loading
        viewModelScope.launch {
            _submitState.value = try {
                val response = reportsRepository.submitReport(lat, lon, reportType, description.trim())
                loadReports(lat, lon)
                UiState.Success(response.points)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetSubmitState() {
        _submitState.value = UiState.Idle
    }
}
