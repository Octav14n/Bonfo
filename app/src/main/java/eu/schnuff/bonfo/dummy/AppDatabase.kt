package eu.schnuff.bonfo.dummy

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [EPubItem::class], version = 1, exportSchema = false)
@TypeConverters(PersistenceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ePubItemDao() : EPubItemDAO
}