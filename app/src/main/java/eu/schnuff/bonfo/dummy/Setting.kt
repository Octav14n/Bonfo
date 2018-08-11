package eu.schnuff.bonfo.dummy

import android.content.Context
import android.os.Environment
import org.jetbrains.anko.apply
import java.util.*
import kotlin.collections.HashSet

private const val SETTING_NAME = "general"
private const val WATCHED_DIR = "directories"
private const val FILTER_LARGE = "filterLarge"

object Setting {
    private var loaded = false
    val watchedDirectory: MutableSet<String> = HashSet()
    var filterLargeFiles = false
        private set

    fun load(context: Context) {
        if (loaded)
            return
        loaded = true
        val pref = context.getPref()
        watchedDirectory.addAll(pref.getStringSet(WATCHED_DIR, Collections.singleton(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        )))
        filterLargeFiles = pref.getBoolean(FILTER_LARGE, false)
    }

    private fun Context.getPref() = this.getSharedPreferences(SETTING_NAME, Context.MODE_PRIVATE)

    fun addPath(context: Context, path: String) {
        val pref = context.getPref()
        watchedDirectory.add(path)
        pref.apply { putStringSet(WATCHED_DIR, watchedDirectory) }
    }

    fun setFilterLargeFiles(context: Context, filterLargeFiles: Boolean) {
        if (this.filterLargeFiles == filterLargeFiles)
            return
        this.filterLargeFiles = filterLargeFiles
        context.getPref().apply { putBoolean(FILTER_LARGE, filterLargeFiles) }
    }
}