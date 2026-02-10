package com.example.storepromax.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.DashboardStats
import com.example.storepromax.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminStatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(DashboardStats())
    val stats = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _filterText = MutableStateFlow("7 ngày qua")
    val filterText = _filterText.asStateFlow()
    init {
        loadStats()
    }

    fun loadStats(start: Long? = null, end: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            if (start != null && end != null) {
                val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                _filterText.value = "${sdf.format(start)} - ${sdf.format(end)}"
            } else {
                _filterText.value = "7 ngày qua"
            }
            val result = statsRepository.getDashboardStats(start, end)
            result.onSuccess { _stats.value = it }
            _isLoading.value = false
        }
    }
}