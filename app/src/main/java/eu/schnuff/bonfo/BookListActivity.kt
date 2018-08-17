package eu.schnuff.bonfo

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.os.StrictMode
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.*
import android.widget.SearchView
import android.widget.TextView
import eu.schnuff.bonfo.dummy.EPubContent
import eu.schnuff.bonfo.dummy.Setting
import eu.schnuff.bonfo.helpers.RFastScroller
import kotlinx.android.synthetic.main.activity_book_list.*
import kotlinx.android.synthetic.main.book_list.*
import kotlinx.android.synthetic.main.book_list_content.view.*
import org.jetbrains.anko.newTask
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.io.File


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [BookDetailActivity] representing
 * item description. On tablets, the activity presents the list of items and
 * item description side-by-side using two vertical panes.
 */
class BookListActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Setting.load(applicationContext)
        StrictMode::class.java.getMethod("disableDeathOnFileUriExposure").invoke(null)
        setContentView(R.layout.activity_book_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        if (book_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }
        setupEPubList()
        setupRecyclerView(book_list)
        RFastScroller(book_list, Color.WHITE, Color.GRAY)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_book_list_menu, menu)
        val searchView = menu!!.findItem(R.id.app_bar_search).actionView as SearchView
        if (EPubContent.filter.isNotEmpty()) {
            searchView.setQuery(EPubContent.filter, false)
            searchView.clearFocus()
        }
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { return false }

            override fun onQueryTextChange(newText: String?): Boolean {
                EPubContent.filter = newText ?: ""
                return true
            }

        })
        (menu.findItem(R.id.filterLargeFile) as MenuItem).isChecked = Setting.filterLargeFiles
        return true
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?, savedPersistentState: PersistableBundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedPersistentState?.run {
            EPubContent.filter = getString(SAVED_FILTER)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        outPersistentState?.putString(SAVED_FILTER, EPubContent.filter)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settings -> startActivity<SettingsActivity>()
            R.id.filterLargeFile -> {
                item.isChecked = !item.isChecked
                Setting.setFilterLargeFiles(this, item.isChecked)
                EPubContent.filterLarge = item.isChecked
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            PERMISSION_SDCARD -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { readEPubsForList() }
        }
    }

    private fun setupEPubList() {
        EPubContent.filterLarge = Setting.filterLargeFiles
        EPubContent.onListChanged = {
            runOnUiThread {
                book_list.adapter.notifyDataSetChanged()
            }
        }
        if (EPubContent.ITEMS.isEmpty()) {
            readEPubsForList()
        }
    }

    private fun readEPubsForList() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_SDCARD)
        } else {
            book_list_refresh!!.isRefreshing = true
            EPubContent.readItems {
                book_list_refresh!!.isRefreshing = false
            }
        }
    }


    private fun setupRecyclerView(recyclerView: RecyclerView) {
        book_list_refresh!!.setOnRefreshListener { readEPubsForList() }
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, twoPane)
    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: BookListActivity,
                                        private val twoPane: Boolean) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onLongClickListener: View.OnLongClickListener
        private val onClickListener: View.OnClickListener

        init {
            onLongClickListener = View.OnLongClickListener { v ->
                val item = v.tag as EPubContent.EPubItem
                if (twoPane) {
                    val fragment = BookDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(BookDetailFragment.ARG_ITEM_ID, item.filePath)
                        }
                    }
                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.book_detail_container, fragment)
                            .commit()
                } else {
                    parentActivity.startActivity<BookDetailActivity>(BookDetailFragment.ARG_ITEM_ID to item.filePath)
                }
                true
            }
            onClickListener = View.OnClickListener {
                val item = it.tag as EPubContent.EPubItem
                val intent = Intent(Intent.ACTION_VIEW)
                        .newTask()
                        .setDataAndType(Uri.fromFile(File(item.filePath)), "application/epub+zip")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    parentActivity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    parentActivity.toast("Activity not found.")
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.book_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = EPubContent.ITEMS[position]
            val highlighting = EPubContent.filter.split(' ').filter { it.length >= HIGHLIGHT_MIN }
            with(holder) {

                header.setHighlightedText(item.name, highlighting)
                author.setHighlightedText(item.author, highlighting, R.string.author)
                body.setHighlightedText(item.description, highlighting)
                genres.setHighlightedText(item.genres, highlighting)
                characters.setHighlightedText(item.characters, highlighting)
                size.text = item.size
            }

            with(holder.itemView) {
                tag = item
                setOnLongClickListener(onLongClickListener)
                setOnClickListener(onClickListener)
            }
        }

        private fun TextView.setHighlightedText(text: String?, highlighting: Collection<String>, formattedBy: Int? = null) {
            if (text.isNullOrEmpty()) {
                this.visibility = View.GONE
            } else {
                this.visibility = View.VISIBLE
                val value = highlight(text, highlighting)
                val formatted = if (formattedBy == null) value else parentActivity.getString(formattedBy, value)
                this.text = Html.fromHtml(formatted, Html.FROM_HTML_MODE_COMPACT)
            }
        }
        private fun TextView.setHighlightedText(texts: Array<String>, highlighting: Collection<String>, formattedBy: Int? = null) {
            if (texts.isEmpty()) {
                this.visibility = View.GONE
            } else {
                this.visibility = View.VISIBLE
                val value = highlight(texts, highlighting)
                val formatted = if (formattedBy == null) value else parentActivity.getString(formattedBy, value)
                this.text = Html.fromHtml(formatted, Html.FROM_HTML_MODE_COMPACT)
            }
        }

        private fun highlight(item: String?, highlighting: Collection<String>) : String {
            return when {
                item === null -> ""
                highlighting.isNotEmpty() -> {
                    highlighting.foldIndexed(item) { myI, acc, highlight ->
                        val i = myI % HIGHLIGHT_COLOR.size
                        acc.replace("($highlight)".toRegex(RegexOption.IGNORE_CASE), HIGHLIGHT.format(HIGHLIGHT_COLOR[i]))
                    }
                }
                else -> item
            }
        }
        private fun highlight(items: Array<String>, highlighting: Collection<String>) : String =
                items.joinToString { s -> highlight(s, highlighting) }

        override fun getItemCount() = EPubContent.ITEMS.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val header = view.header!!
            val author = view.author!!
            val body = view.body!!
            val genres = view.genres!!
            val characters = view.characters!!
            val size = view.size!!
        }
    }

    companion object {
        const val PERMISSION_SDCARD = 1
        const val SAVED_FILTER = "filter"
        const val HIGHLIGHT = "<font color='%s'>$1</font>"
        val HIGHLIGHT_COLOR = arrayOf("#FFCC00", "#CCFF00", "#FF00CC", "#CC00FF")
        const val HIGHLIGHT_MIN = 2
    }
}
