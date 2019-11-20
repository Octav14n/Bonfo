package eu.schnuff.bonfo.dummy

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.jetbrains.anko.doAsync
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.BufferedInputStream
import java.io.File
import java.util.*
import java.util.zip.ZipFile
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory
import kotlin.collections.ArrayList

/**
 * Helper class for providing sample title for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
private const val LARGE_FILE_MIN_SIZE: Long = (1024 shl 1) * 100
private const val CONTAINER_FILE_PATH = "META-INF/container.xml"
private val XPATH = XPathFactory.newInstance().newXPath().apply {
    namespaceContext = object : NamespaceContext {
        override fun getNamespaceURI(p0: String?): String = when (p0) {
            "x" -> "http://www.idpf.org/2007/opf"
            "dc" -> "http://purl.org/dc/elements/1.1/"
            "opf" -> "http://www.idpf.org/2007/opf"
            "xsi" -> "http://www.w3.org/2001/XMLSchema-instance"
            "dcterms" -> "http://purl.org/dc/terms/"
            "cont" -> "urn:oasis:names:tc:opendocument:xmlns:container"
            else -> XMLConstants.NULL_NS_URI
        }

        override fun getPrefix(p0: String?): String {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getPrefixes(p0: String?): MutableIterator<String> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}
private val XPATH_META = XPATH.compile("/x:package/x:metadata")
private val XPATH_ID = XPATH.compile("dc:identifier/text()")
private val XPATH_TITLE = XPATH.compile("dc:title/text()")
private val XPATH_AUTHOR = XPATH.compile("dc:creator/text()")
private val XPATH_FANDOM = XPATH.compile("x:meta[@name='fandom']/@content|dc:subject[not(starts-with(., 'Last Update') or text() = 'FanFiction')]/text()")
private val XPATH_DESCRIPTION = XPATH.compile("dc:description/text()")
private val XPATH_GENRES = XPATH.compile("dc:type/text()")
private val XPATH_CHARACTERS = XPATH.compile("x:meta[@name='characters']/@content")
private val XPATH_OPF_FILE_PATH = XPATH.compile("//cont:rootfile/@full-path[1]")
private val DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }

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
            val itemFilePath = items_original.associateByTo(mutableMapOf<String, EPubItem>()) { it.filePath }

            val dao = PersistenceHelper.epub_items(context).ePubItemDao()
            val list = mutableListOf<EPubItem>()
            runBlocking {
                Setting.watchedDirectory.forEach {
                    val dir = File(it)
                    dir.walkTopDown().filter { it.isFile && it.extension =="epub" }.forEach { file ->
                        try {
                            readEPub(file, itemFilePath.remove(file.absolutePath), dao)?.run {
                                list.add(this)
                            }
                        } catch (e: Exception) {
                            Log.d("reading", "error reading", e)
                        }
                    }
                }
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
            var opfPath = epub.entries().asSequence().first { !it.isDirectory && it.name.endsWith(".opf") }?.name
            if (opfPath == null) {
                val containerInfoEntry = epub.getEntry(CONTAINER_FILE_PATH)!!
                opfPath = BufferedInputStream(epub.getInputStream(containerInfoEntry)).use {
                    val container = DOCUMENT_FACTORY.newDocumentBuilder().parse(it)
                    it.close()
                    XPATH_OPF_FILE_PATH.evaluate(container, XPathConstants.STRING).toString()
                }
            }
            val opfEntry = epub.getEntry(opfPath)
            if (opfEntry === null)
                return null
            if (opfEntry.crc == other?.opfCrc) {
                val modified = Date(file.lastModified())
                if (modified != other.modified) {
                    val item = other.copy(modified = modified)
                    dao.update(item)
                    return item
                }
                return other
            }
            Log.d("reading", "Now reading " + file.nameWithoutExtension)
            BufferedInputStream(epub.getInputStream(opfEntry)).use { opfStream ->
                val opf = DOCUMENT_FACTORY.newDocumentBuilder().parse(opfStream)
                opfStream.close()
                opf.normalizeDocument()

                val meta  = XPATH_META.evaluate(opf, XPathConstants.NODE) as Node

                val id = path(meta, XPATH_ID) ?: "file://" + file.canonicalPath
                val title = path(meta, XPATH_TITLE) ?: "<No Title>"
                val author = path(meta, XPATH_AUTHOR)
                val fandom = path(meta, XPATH_FANDOM)
                val description = path(meta, XPATH_DESCRIPTION, "\n\n")
                val genres = path(meta, XPATH_GENRES)?.split(", ") ?: Collections.emptyList<String>()
                val characters = path(meta, XPATH_CHARACTERS)?.split(", ") ?: Collections.emptyList<String>()

                val item = EPubItem(file.absolutePath, file.name, Date(file.lastModified()), file.length(), opfEntry.crc,
                        id, title, author, fandom, description, genres.toTypedArray(), characters.toTypedArray())
                if (other != null) {
                    dao.update(item)
                } else {
                    dao.insert(item)
                }
                return item
            }
        }
    }

    private fun path(node: Node, xPath: XPathExpression, seperator: String = ", "): String? {
        val texts = xPath.evaluate(node, XPathConstants.NODESET) as NodeList
        return if (texts.length == 0) {
            null
        } else {
            Array(texts.length) { i -> texts.item(i).nodeValue.trim() }.joinToString(seperator)
        }
    }
}
