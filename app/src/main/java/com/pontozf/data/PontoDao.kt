package com.pontozf.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PontoDao {

    @Query("SELECT * FROM pontos ORDER BY timestamp DESC")
    fun observarTodos(): Flow<List<Ponto>>

    @Query("SELECT * FROM pontos WHERE timestamp BETWEEN :inicio AND :fim ORDER BY timestamp ASC")
    suspend fun entre(inicio: Long, fim: Long): List<Ponto>

    @Insert
    suspend fun inserir(ponto: Ponto)

    @Delete
    suspend fun excluir(ponto: Ponto)
}
