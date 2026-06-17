package com.example.data.database

import androidx.room.*
import com.example.data.model.Evaporation
import com.example.data.model.Farm
import kotlinx.coroutines.flow.Flow

@Dao
interface IrrigationDao {

    // --- FARMS ---
    @Query("SELECT * FROM farms ORDER BY id DESC")
    fun getAllFarms(): Flow<List<Farm>>

    @Query("SELECT * FROM farms WHERE id = :id")
    suspend fun getFarmById(id: Int): Farm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarm(farm: Farm)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarms(farms: List<Farm>)

    @Update
    suspend fun updateFarm(farm: Farm)

    @Delete
    suspend fun deleteFarm(farm: Farm)

    @Query("DELETE FROM farms WHERE id = :id")
    suspend fun deleteFarmById(id: Int)

    // --- EVAPORATION ---
    @Query("SELECT * FROM evaporation ORDER BY date DESC")
    fun getAllEvaporations(): Flow<List<Evaporation>>

    @Query("SELECT * FROM evaporation")
    suspend fun getEvaporationsSync(): List<Evaporation>

    @Query("SELECT * FROM evaporation WHERE date = :date LIMIT 1")
    suspend fun getEvaporationByDate(date: String): Evaporation?

    @Query("SELECT * FROM evaporation WHERE id = :id")
    suspend fun getEvaporationById(id: Int): Evaporation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaporation(evaporation: Evaporation)

    @Update
    suspend fun updateEvaporation(evaporation: Evaporation)

    @Delete
    suspend fun deleteEvaporation(evaporation: Evaporation)

    @Query("DELETE FROM evaporation WHERE id = :id")
    suspend fun deleteEvaporationById(id: Int)
}
