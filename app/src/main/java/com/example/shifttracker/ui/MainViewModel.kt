package com.example.shifttracker.ui

import android.content.Context
import androidx.lifecycle.*
import com.example.shifttracker.data.Repo
import com.example.shifttracker.data.ProjectEntity
import com.example.shifttracker.data.ShiftEntity
import com.example.shifttracker.data.ShiftWithProject
import com.example.shifttracker.domain.Summary
import com.example.shifttracker.domain.summarize
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class YM(val year: Int, val month: java.time.Month)

class MainViewModel(private val repo: Repo) : ViewModel() {
    private val _currentYM = MutableStateFlow(YearMonth.now())
    val currentYM: StateFlow<YearMonth> = _currentYM

    val projects = repo.observeProjects().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val rangeFlow = currentYM.map { ym ->
        ym.atDay(1) to ym.atEndOfMonth()
    }

    val shifts: StateFlow<List<ShiftWithProject>> =
        rangeFlow.flatMapLatest { (from, to) -> repo.observeShifts(from, to) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val summary: StateFlow<Summary> =
        shifts.map { summarize(it) }
            .stateIn(viewModelScope, SharingStarted.Lazily, com.example.shifttracker.domain.Summary(0,0.0))

    fun prevMonth() { _currentYM.value = _currentYM.value.minusMonths(1) }
    fun nextMonth() { _currentYM.value = _currentYM.value.plusMonths(1) }

    fun addProject(name: String, hourly: Double, fixed: Double) = viewModelScope.launch {
        repo.addProject(name, hourly, fixed)
    }

    fun addShift(date: LocalDate, projectId: Long, hours: Double, customPay: Double, note: String) = viewModelScope.launch {
        repo.addShift(date, projectId, hours, customPay, note)
    }

    fun deleteShift(s: ShiftEntity) = viewModelScope.launch { repo.deleteShift(s) }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(Repo.get(appContext)) as T
        }
    }
}
