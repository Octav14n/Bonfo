package eu.schnuff.bonfo.dummy

import org.jetbrains.anko.doAsync
import org.w3c.dom.Element
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

/**
 * Helper class for providing sample title for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
private const val LARGE_FILE_MIN_SIZE: Long = (1024 shl 1) * 100
private const val OPF_FILE_PATH = "OEBPS/Content.opf"
private const val METADATA_PARENT_NAME = "metadata"
private const val METADATA_TAG_NAMESPACE_PREFIX = "dc:"
private const val METADATA_URL = "${METADATA_TAG_NAMESPACE_PREFIX}identifier"
private const val METADATA_TITLE = "${METADATA_TAG_NAMESPACE_PREFIX}title"
private const val METADATA_AUTHOR = "${METADATA_TAG_NAMESPACE_PREFIX}creator"
private const val METADATA_DESCRIPTION = "${METADATA_TAG_NAMESPACE_PREFIX}description"
private const val METADATA_GENRES = "${METADATA_TAG_NAMESPACE_PREFIX}type"
private const val METADATA_META = "meta"
private const val METADATA_META_NAME = "name"
private const val METADATA_META_CONTENT = "content"
private const val METADATA_META_FANDOM_NAME = "fandom"
private const val METADATA_META_CHARACTERS_NAME = "characters"

object EPubContent {

    /**
     * An array of sample (dummy) items.
     */
    private val items_original: MutableList<EPubItem> = ArrayList()
    var ITEMS: List<EPubItem> = ArrayList()
        set(value) {
            field = value
            onListChanged()
        }

    /**
     * A map of EPubItems by file-path.
     */
    val ITEM_MAP: MutableMap<String, EPubItem> = HashMap()
    var onListChanged: () -> Unit = {}

    var filter = ""
        set(value) {
            val valueL = value.toLowerCase()
            if (field == valueL)
                return
            field = valueL
            applyFilter(value = valueL)
        }
    var filterLarge: Boolean = false
        set(value) {
            if (field == value)
                return
            field = value
            applyFilter(largeFilesOnly = value)
        }

    private fun applyFilter(value: String? = null, largeFilesOnly: Boolean? = null) {
        val filters = (value ?: filter).split(' ')
        var filtered: List<EPubItem> = items_original
        if (largeFilesOnly ?: filterLarge)
            filtered = filtered.filter { it.fileSize > LARGE_FILE_MIN_SIZE }
        if (!filters.isEmpty()) {
            filtered = filtered.filter { item -> filters.all { filter -> filter.isEmpty() || item.contains(filter) } }
        }
        ITEMS = filtered
    }

    fun readItems(onComplete: () -> Unit) {
        doAsync {
            /*onComplete {
                onComplete()
            }*/
            val list = LinkedList<EPubItem>()
            Setting.watchedDirectory.forEach {
                val dir = File(it)
                val files = dir.listFiles { _, name -> name.endsWith(".epub") }
                val countDownLatch = CountDownLatch(files.size)
                files.forEach { file ->
                    //doAsync {
                    readEPub(file, ITEM_MAP[file.absolutePath])?.run { list.add(this) }
                    countDownLatch.countDown()
                    //}
                }
                countDownLatch.await()
            }
            list.sortWith(compareByDescending(EPubItem::modified))

            // Populate read items.
            items_original.clear()
            ITEM_MAP.clear()
            list.forEach { addItem(it) }
            applyFilter()
            onComplete()
        }
    }

    private fun addItem(item: EPubItem) {
        items_original.add(item)
        ITEM_MAP[item.filePath] = item
    }

    private fun readEPub(file: File, other: EPubItem?): EPubItem? {
        ZipFile(file).use { epub ->
            val opfEntry = epub.getEntry(OPF_FILE_PATH)
            if (opfEntry === null)
                return null
            if (opfEntry.crc == other?.opfCrc)
                return other
            epub.getInputStream(opfEntry).use { opfStream ->
                val opf = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(opfStream)
                val meta = opf.getElementsByTagName(METADATA_PARENT_NAME)?.item(0) as? Element ?: return null
                meta.normalize()

                /*Log.d(this.javaClass.simpleName, "meta of '${file.name}' has the following elements:")
            for (nodeI in 0 .. meta.childNodes.length) {
                val node = meta.childNodes.item(nodeI) as? Element ?: continue
                val attrsLength = if (node.hasAttributes()) node.attributes.length else 0
                val attrs = HashMap<String, String>(attrsLength)
                for (attrI in 0 until attrsLength) {
                    val attr = node.attributes.item(attrI) as Attr
                    attrs[attr.name] = attr.value
                }
                Log.d(this.javaClass.simpleName, "\tNode name '${node.nodeName}' with attributes '$attrs'")
            }*/

                val url = meta.getElementsByTagName(METADATA_URL).item(0).textContent.trimEnd(' ', '\n')
                val title = meta.getElementsByTagName(METADATA_TITLE).item(0).textContent
                        .trimEnd(' ', '\n')
                val author = meta.getElementsByTagName(METADATA_AUTHOR).takeIf { it.length > 0 }?.item(0)?.textContent?.trimEnd(' ', '\n')
                val description = meta.getElementsByTagName(METADATA_DESCRIPTION).takeIf { it.length > 0 }?.item(0)?.textContent?.trimEnd(' ', '\n')
                val genres = meta.getElementsByTagName(METADATA_GENRES).takeIf { it.length > 0 }?.item(0)
                        ?.textContent?.trimEnd(' ', '\n')?.split(", ") ?: Collections.emptyList<String>()
                var fandom: String? = null
                var characters = Collections.emptyList<String>()
                meta.getElementsByTagName(METADATA_META).let {
                    for (i in 0 until it.length) {
                        val elem = it.item(i) as Element
                        if (!elem.hasAttribute(METADATA_META_NAME) || !elem.hasAttribute(METADATA_META_CONTENT))
                            continue
                        val content = elem.getAttribute(METADATA_META_CONTENT)
                        when (elem.getAttribute(METADATA_META_NAME).toLowerCase()) {
                            METADATA_META_FANDOM_NAME -> fandom = content
                            METADATA_META_CHARACTERS_NAME -> characters = content.split(", ")
                        }
                    }
                }
                return EPubItem(file.absolutePath, file.name, Date(file.lastModified()), file.length(), opfEntry.crc,
                        url, title, author, fandom, description, genres.toTypedArray(), characters.toTypedArray())
            }
        }
    }

    /**
     * A dummy item representing a piece of title.
     */
    data class EPubItem(
            val filePath: String,
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
}
