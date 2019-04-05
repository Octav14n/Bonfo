package eu.schnuff.bonfo.dummy

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

/**
 * A dummy item representing a piece of title.
 */
@Entity
data class EPubItem(
        @PrimaryKey val filePath: String,
        val fileName: String,
        val modified: Date,
        val fileSize: Long,
        val opfCrc: Long,
        val url: String,
        val title: String,
        val author: String?,
        val fandom: String?,
        val description: String?,
        val genres: Array<String>,
        val characters: Array<String>
) {
    @Ignore
    val size: String

    init {
        val b = fileSize
        val k = b / 1024.0
        val m = k / 1024.0
        val g = m / 1024.0
        val t = g / 1024.0

        size = when {
            t > 1 -> "%.2f TB".format(t)
            g > 1 -> "%.2f GB".format(g)
            m > 1 -> "%.2f MB".format(m)
            k > 1 -> "%.2f KB".format(k)
            else -> "%.2f Bytes".format(b)
        }
    }

    val name get() = if (fandom !== null) "$fandom - $title" else fileName
    override fun toString(): String = name

    fun contains(str: String): Boolean {
        if (name.contains(str, true)) return true
        if (author?.contains(str, true) == true) return true
        if (description?.contains(str, true) == true) return true
        if (genres.any { s -> s.contains(str, true) }) return true
        if (characters.any { s -> s.contains(str, true) }) return true
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EPubItem

        if (filePath != other.filePath) return false
        if (fileName != other.fileName) return false
        if (modified != other.modified) return false
        if (fileSize != other.fileSize) return false
        if (opfCrc != other.opfCrc) return false
        if (url != other.url) return false
        if (title != other.title) return false
        if (author != other.author) return false
        if (fandom != other.fandom) return false
        if (description != other.description) return false
        if (!Arrays.equals(genres, other.genres)) return false
        if (!Arrays.equals(characters, other.characters)) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + modified.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + opfCrc.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (fandom?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + Arrays.hashCode(genres)
        result = 31 * result + Arrays.hashCode(characters)
        result = 31 * result + size.hashCode()
        return result
    }
}