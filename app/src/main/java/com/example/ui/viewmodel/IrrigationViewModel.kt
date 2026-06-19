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
                    onComplete("رکورد تبخیر آب این تاریخ موجود بود و مقدار آن بروزرسانی گردید.")
                } else {
                    repository.insertEvaporation(Evaporation(date = cleanDate, evap = evapVal))
                    onComplete("رکورد روزانه تبخیر آب افزوده شد.")
                }
            } else {
                val currentRecord = repository.getEvaporationByDate(cleanDate)
                if (currentRecord != null && currentRecord.id != id) {
                    onComplete("خطا: قبلاً برای این تاریخ تبخیر آب ثبت شده است.")
                } else {
                    repository.insertEvaporation(Evaporation(id = id, date = cleanDate, evap = evapVal))
                    onComplete("رکورد تبخیر آب بروزرسانی شد.")
                }
            }
        }
    }

    fun deleteEvaporation(evapId: Int, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteEvaporationById(evapId)
            onComplete("رکورد تبخیر آب حذف شد.")
        }
    }

    // --- SUPPORT FOR BACKUP & EXCHANGE (Export/Import JSON) ---
    fun exportDataToJson(onComplete: (String, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val allFarmsList = farms.value
                val allEvapList = repository.getEvaporationsSync()

                val root = org.json.JSONObject()
                
                val farmsArray = org.json.JSONArray()
                for (f in allFarmsList) {
                    val fObj = org.json.JSONObject()
                    fObj.put("id", f.id)
                    fObj.put("name", f.name)
                    fObj.put("area", f.area)
                    fObj.put("target_cpe", f.target_cpe)
                    fObj.put("last_irrigation", f.last_irrigation)
                    farmsArray.put(fObj)
                }
                root.put("farms", farmsArray)

                val evapArray = org.json.JSONArray()
                for (e in allEvapList) {
                    val eObj = org.json.JSONObject()
                    eObj.put("id", e.id)
                    eObj.put("date", e.date)
                    eObj.put("evap", e.evap)
                    evapArray.put(eObj)
                }
                root.put("evaporation", evapArray)

                val backupString = root.toString(4)
                onComplete("کد پشتیبان‌گیری مزارع و تبخیر با موفقیت ساخته شد.", backupString)
            } catch (e: Exception) {
                onComplete("خطا در پشتیبان‌گیری: ${e.message}", null)
            }
        }
    }

    fun importDataFromJson(jsonStr: String, onComplete: (String) -> Unit) {
        val trimmed = jsonStr.trim()
        if (trimmed.isEmpty()) {
            onComplete("متن یا کد پشتیبان خالی است.")
            return
        }
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(trimmed)
                var farmsCount = 0
                var evapsCount = 0

                if (root.has("farms")) {
                    val farmsArray = root.getJSONArray("farms")
                    for (i in 0 until farmsArray.length()) {
                        val fObj = farmsArray.getJSONObject(i)
                        val idVal = fObj.optInt("id", 0)
                        val farmName = fObj.optString("name", "")
                        val farmArea = fObj.optDouble("area", 12.0)
                        val farmTargetCpe = fObj.optDouble("target_cpe", 0.0)
                        val farmLastIrrig = fObj.optString("last_irrigation", "")

                        if (farmName.isNotEmpty()) {
                            // Using standard replace inside Dao
                            repository.insertFarm(
                                Farm(
                                    id = if (idVal > 0) idVal else 0,
                                    name = farmName,
                                    area = farmArea,
                                    target_cpe = farmTargetCpe,
                                    last_irrigation = farmLastIrrig
                                )
                            )
                            farmsCount++
                        }
                    }
                }

                if (root.has("evaporation")) {
                    val evapArray = root.getJSONArray("evaporation")
                    for (i in 0 until evapArray.length()) {
                        val eObj = evapArray.getJSONObject(i)
                        val idVal = eObj.optInt("id", 0)
                        val eDate = eObj.optString("date", "")
                        val eEvap = eObj.optDouble("evap", 0.0)

                        if (eDate.isNotEmpty()) {
                            repository.insertEvaporation(
                                Evaporation(
                                    id = if (idVal > 0) idVal else 0,
                                    date = eDate,
                                    evap = eEvap
                                )
                            )
                            evapsCount++
                        }
                    }
                }

                onComplete("انتقال با موفقیت انجام شد: $farmsCount مزرعه و $evapsCount رکورد تبخیر بارگذاری شد.")
            } catch (e: Exception) {
                onComplete("خطا در بازخوانی کد پشتیبان: فرمت کد نامعتبر است.")
            }
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
