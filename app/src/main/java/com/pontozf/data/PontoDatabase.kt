package com.pontozf.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Ponto::class], version = 1, exportSchema = false)
abstract class PontoDatabase : RoomDatabase() {

    abstract fun pontoDao(): PontoDao

    companion object {
        @Volatile
        private var instancia: PontoDatabase? = null

        fun get(context: Context): PontoDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    PontoDatabase::class.java,
                    "pontozf.db"
                ).build().also { instancia = it }
            }
    }
}
