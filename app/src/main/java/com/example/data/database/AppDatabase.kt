package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Evaporation
import com.example.data.model.Farm

@Database(
    entities = [Farm::class, Evaporation::class],
    version = 2, // Upgraded version for schema migration
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun irrigationDao(): IrrigationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "irrigation_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
