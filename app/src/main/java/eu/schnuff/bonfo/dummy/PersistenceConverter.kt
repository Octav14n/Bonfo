package eu.schnuff.bonfo.dummy

import android.arch.persistence.room.TypeConverter
import org.json.JSONArray
import java.util.*

class PersistenceConverter {
    @TypeConverter
    fun fromStringArray(array: Array<String>): String {
        return JSONArray(array).toString()
    }

    @TypeConverter
    fun toStringArray(string: String): Array<String> {
        val array = JSONArray(string)
        return Array(array.length()) {
            array[it] as String
        }
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(long: Long): Date {
        return Date(long)
    }
}