package com.example.data.repository

import com.example.data.database.IrrigationDao
import com.example.data.model.Evaporation
import com.example.data.model.Farm
import com.example.util.JalaliCalendarHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class IrrigationRepository(private val irrigationDao: IrrigationDao) {

    val allFarms: Flow<List<Farm>> = irrigationDao.getAllFarms()
    val allEvaporations: Flow<List<Evaporation>> = irrigationDao.getAllEvaporations()

    suspend fun getFarmById(id: Int): Farm? = irrigationDao.getFarmById(id)

    suspend fun insertFarm(farm: Farm) = irrigationDao.insertFarm(farm)

    suspend fun updateFarm(farm: Farm) = irrigationDao.updateFarm(farm)

    suspend fun deleteFarm(farm: Farm) = irrigationDao.deleteFarm(farm)

    suspend fun deleteFarmById(id: Int) = irrigationDao.deleteFarmById(id)

    suspend fun insertEvaporation(evaporation: Evaporation) = irrigationDao.insertEvaporation(evaporation)

    suspend fun updateEvaporation(evaporation: Evaporation) = irrigationDao.updateEvaporation(evaporation)

    suspend fun deleteEvaporation(evaporation: Evaporation) = irrigationDao.deleteEvaporation(evaporation)

    suspend fun deleteEvaporationById(id: Int) = irrigationDao.deleteEvaporationById(id)

    suspend fun getEvaporationsSync(): List<Evaporation> = irrigationDao.getEvaporationsSync()

    suspend fun getEvaporationByDate(date: String): Evaporation? = irrigationDao.getEvaporationByDate(date)

    // Seeds beautiful default farms and historical evaporation readings (No longer seeding mock data to ensure empty start)
    suspend fun seedDatabaseIfEmpty() {
        // Kept empty to satisfy user request of having absolutely no preset default farms or evaporation data upon install.
    }
}
