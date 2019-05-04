package eu.schnuff.bonfo.dummy

import android.content.Context
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
            if (isLoaded) onListChanged()
        }
    var onListChanged: () -> Unit = {}
    val isLoaded get() = items_original.isNotEmpty()

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
        val filters = (value ?: filter).split(' ').filter { f -> f.isNotBlank() }
        var filtered: List<EPubItem> = items_original
        if (largeFilesOnly ?: filterLarge)
            filtered = filtered.filter { it.fileSize > LARGE_FILE_MIN_SIZE }
        if (filters.isNotEmpty()) {
            filtered = filtered.filter { item -> filters.all { filter -> item.contains(filter) } }
        }
        ITEMS = filtered
    }

    operator fun get(filePath: String): EPubItem? {
        return items_original.find { it.filePath == filePath }
    }

    fun readItems(context: Context, onComplete: () -> Unit) {
        doAsync {
            /*onComplete {
                onComplete()
            }*/
            val itemFilePath: MutableMap<String, EPubItem> = HashMap()
            items_original.forEach {
                itemFilePath[it.filePath] = it
            }

            val dao = PersistenceHelper.epub_items(context).ePubItemDao()
            val list = LinkedList<EPubItem>()
            Setting.watchedDirectory.forEach {
                val dir = File(it)
                val files = dir.listFiles { _, name -> name.endsWith(".epub") }
                val countDownLatch = CountDownLatch(files.size)
                files.forEach { file ->
                    readEPub(file, itemFilePath.remove(file.absolutePath), dao)?.run { list.add(this) }
                    countDownLatch.countDown()
                }
                countDownLatch.await()
            }
            list.sortWith(compareByDescending(EPubItem::modified))

            // Remove deleted items.
            itemFilePath.values.forEach {
                dao.delete(it)
            }

            // Populate read items.
            setItems(list)
            onComplete()
        }
    }

    private fun setItems(items: List<EPubItem>) {
        items_original.clear()
        items.forEach { addItem(it) }
        applyFilter()
    }

    fun loadItems(context: Context, onComplete: () -> Unit) {
        doAsync {
            val db = PersistenceHelper.epub_items(context)
            val items = db.ePubItemDao().getAll()
            setItems(items)
            onComplete()
        }
    }

    private fun addItem(item: EPubItem) {
        items_original.add(item)
    }

    private fun readEPub(file: File, other: EPubItem?, dao: EPubItemDAO): EPubItem? {
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
                val item = EPubItem(file.absolutePath, file.name, Date(file.lastModified()), file.length(), opfEntry.crc,
                        url, title, author, fandom, description, genres.toTypedArray(), characters.toTypedArray())
                if (other != null) {
                    dao.update(item)
                } else {
                    dao.insert(item)
                }
                return item
            }
        }
    }
}
