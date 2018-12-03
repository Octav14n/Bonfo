package eu.schnuff.bonfo.dummy

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters

@Database(entities = [EPubItem::class], version = 1, exportSchema = false)
@TypeConverters(PersistenceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ePubItemDao() : EPubItemDAO
}