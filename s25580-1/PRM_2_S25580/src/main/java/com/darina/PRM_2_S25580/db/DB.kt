package com.darina.PRM_2_S25580.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.darina.PRM_2_S25580.dao.DaoEntity
import com.darina.PRM_2_S25580.model.Entity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Database(entities = [Entity::class], version = 4, exportSchema = false)
abstract class DB : RoomDatabase() {

    abstract fun entityDao(): DaoEntity

    companion object {
        @Volatile
        private var INSTANCE: DB? = null

        fun getIstance(context: Context): DB {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabaseInstance(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabaseInstance(appContext: Context): DB {
            return Room.databaseBuilder(
                appContext,
                DB::class.java,
                "app_db"
            )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseInitializer(appContext))
                .build()
        }

        private class DatabaseInitializer(private val context: Context) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabaseAsync(db, context)
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabaseAsync(db, context)
                }
            }

            private fun populateDatabaseAsync(db: SupportSQLiteDatabase, context: Context) {
                try {
                    val inputStream = context.assets.open(DATABASE_FILE_PATH)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    db.beginTransaction()
                    readLinesAndExecuteSQL(reader, db)
                    db.setTransactionSuccessful()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    db.endTransaction()
                }
            }

            private fun readLinesAndExecuteSQL(reader: BufferedReader, db: SupportSQLiteDatabase) {
                reader.use { bufferedReader ->
                    var line: String? = bufferedReader.readLine()
                    while (line != null) {
                        db.execSQL(line)
                        line = bufferedReader.readLine()
                    }
                }
            }

            companion object {
                private const val DATABASE_FILE_PATH = "com/darina/PRM_2_S25580/db/app_database-diary_entries.sql"
            }
        }
    }
}
