package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Evaporation
import com.example.data.model.Farm
import com.example.data.repository.IrrigationRepository
import com.example.util.JalaliCalendarHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FarmMetrics(
    val cumulativeEvap: Double,
    val percentage: Double,
    val status: String, // "ایمن", "هشدار", "بحرانی", "بدون هدف", "نامشخص"
    val statusColor: Long, // Material 3 theme colors / custom hex code
    val message: String
)

class IrrigationViewModel(private val repository: IrrigationRepository) : ViewModel() {

    val farms: StateFlow<List<Farm>> = repository.allFarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val evaporations: StateFlow<List<Evaporation>> = repository.allEvaporations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Helper to calculate metrics for a specific farm
    fun calculateMetrics(farm: Farm): FarmMetrics {
        val evapList = evaporations.value
        val lastIrrigationDate = JalaliCalendarHelper.jalaliToGregorian(farm.last_irrigation)
            ?: return FarmMetrics(0.0, 0.0, "نامشخص", 0xFF7A7A7A, "تاریخ آخرین آبیاری نامعتبر است")

        var cumulativeEvap = 0.0
        for (e in evapList) {
            val readingDate = JalaliCalendarHelper.jalaliToGregorian(e.date)
            if (readingDate != null && !readingDate.before(lastIrrigationDate)) {
                cumulativeEvap += e.evap
            }
        }

        val target = farm.target_cpe
        val percent = if (target > 0) (cumulativeEvap / target * 100.0) else 0.0

        if (target <= 0) {
            return FarmMetrics(
                cumulativeEvap = Math.round(cumulativeEvap * 10) / 10.0,
                percentage = 0.0,
                status = "بدون هدف",
                statusColor = 0xFF7A8D8A,
                message = "هدف CPE تعریف نشده است"
            )
        }

        return when {
            percent < 75.0 -> {
                FarmMetrics(
                    cumulativeEvap = Math.round(cumulativeEvap * 10) / 10.0,
                    percentage = Math.round(percent * 10) / 10.0,
                    status = "ایمن",
                    statusColor = 0xFF4CAF50, // Vibrant success green
                    message = "وضعیت مناسب است"
                )
            }
            percent < 100.0 -> {
                FarmMetrics(
                    cumulativeEvap = Math.round(cumulativeEvap * 10) / 10.0,
                    percentage = Math.round(percent * 10) / 10.0,
                    status = "هشدار",
                    statusColor = 0xFFFF9800, // Amber warning
                    message = "نزدیک به آستانه آبیاری"
                )
            }
            else -> {
                FarmMetrics(
                    cumulativeEvap = Math.round(cumulativeEvap * 10) / 10.0,
                    percentage = Math.round(percent * 10) / 10.0,
                    status = "بحرانی",
                    statusColor = 0xFFF44336, // Critical red
                    message = "نیاز فوری به آبیاری"
                )
            }
        }
    }

    // --- FARMS CONTROLLERS ---
    fun saveFarm(id: Int, name: String, area: Double, targetCpe: Double, lastIrrigation: String, onComplete: (String) -> Unit) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            onComplete("نام مزرعه نمی‌تواند خالی باشد.")
            return
        }
        if (!JalaliCalendarHelper.isValidJalaliDate(lastIrrigation)) {
            onComplete("تاریخ آخرین آبیاری نامعتبر است. نمونه صحیح: 1403/01/01")
            return
        }

        viewModelScope.launch {
            if (id == 0) {
                // Add new
                repository.insertFarm(
                    Farm(
                        name = cleanName,
                        area = area,
                        target_cpe = targetCpe,
                        last_irrigation = lastIrrigation
                    )
                )
                onComplete("مزرعه با موفقیت افزوده شد.")
            } else {
                // Edit existing
                val existing = repository.getFarmById(id)
                if (existing != null) {
                    repository.updateFarm(
                        existing.copy(
                            name = cleanName,
                            area = area,
                            target_cpe = targetCpe,
                            last_irrigation = lastIrrigation
                        )
                    )
                    onComplete("مزرعه با موفقیت بروزرسانی شد.")
                } else {
                    onComplete("مزرعه مورد نظر یافت نشد.")
                }
            }
        }
    }

    fun restartIrrigation(farmId: Int, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val farm = repository.getFarmById(farmId)
            if (farm != null) {
                val todayJalali = JalaliCalendarHelper.getTodayJalali()
                repository.updateFarm(farm.copy(last_irrigation = todayJalali))
                onComplete("چرخه آبیاری از امروز (${todayJalali}) از سر گرفته شد.")
            } else {
                onComplete("مزرعه یافت نشد.")
            }
        }
    }

    fun deleteFarm(farmId: Int, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteFarmById(farmId)
            onComplete("مزرعه با موفقیت حذف شد.")
        }
    }


    // --- EVAPORATION CONTROLLERS ---
    fun saveEvaporation(id: Int, dateStr: String, evapVal: Double, onComplete: (String) -> Unit) {
        val cleanDate = dateStr.trim()
        if (!JalaliCalendarHelper.isValidJalaliDate(cleanDate)) {
            onComplete("تاریخ وارد شده نامعتبر است. نمونه صحیح: 1403/01/01")
            return
        }

        viewModelScope.launch {
            // Check for duplicate date only if inserting a new record or date changed
            val existingByDate = repository.getEvaporationByDate(cleanDate)
            if (id == 0) {
                if (existingByDate != null) {
                    // Update the existing record according to Kivy save logic
                    repository.updateEvaporation(existingByDate.copy(evap = evapVal))
                    onComplete("رکورد تبخیر این تاریخ موجود بود و مقدار آن بروزرسانی گردید.")
                } else {
                    repository.insertEvaporation(Evaporation(date = cleanDate, evap = evapVal))
                    onComplete("رکورد روزانه تبخیر افزوده شد.")
                }
            } else {
                val currentRecord = repository.getEvaporationByDate(cleanDate)
                if (currentRecord != null && currentRecord.id != id) {
                    onComplete("خطا: قبلاً برای این تاریخ تبخیر ثبت شده است.")
                } else {
                    repository.insertEvaporation(Evaporation(id = id, date = cleanDate, evap = evapVal))
                    onComplete("رکورد تبخیر بروزرسانی شد.")
                }
            }
        }
    }

    fun deleteEvaporation(evapId: Int, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteEvaporationById(evapId)
            onComplete("رکورد تبخیر حذف شد.")
        }
    }
}

class IrrigationViewModelFactory(private val repository: IrrigationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IrrigationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IrrigationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
