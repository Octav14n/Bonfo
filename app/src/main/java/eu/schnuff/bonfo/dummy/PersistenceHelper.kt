package eu.schnuff.bonfo.dummy

import android.arch.persistence.room.Room
import android.content.Context

object PersistenceHelper {
    private var appDatabase: AppDatabase? = null

    fun epub_items(context: Context): AppDatabase {
        if (appDatabase == null) {
            appDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "epub_items").build()
        }
        return appDatabase!!
    }
}