package eu.schnuff.bonfo.dummy

import android.content.Context
import androidx.room.Room

object PersistenceHelper {
    private var appDatabase: AppDatabase? = null

    fun epub_items(context: Context): AppDatabase {
        if (appDatabase == null) {
            appDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "epub_items").build()
        }
        return appDatabase!!
    }
}