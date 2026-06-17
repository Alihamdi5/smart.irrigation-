package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "farms")
data class Farm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val area: Double = 0.0,
    val target_cpe: Double = 0.0,
    val last_irrigation: String // Jalali date string "YYYY/MM/DD"
) : Serializable

@Entity(
    tableName = "evaporation",
    indices = [Index(value = ["date"], unique = true)]
)
data class Evaporation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // Jalali date string "YYYY/MM/DD", unique
    val evap: Double = 0.0
) : Serializable
