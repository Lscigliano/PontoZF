package com.pontozf.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pontos")
data class Ponto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long
)
