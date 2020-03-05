package eu.schnuff.bonfo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.schnuff.bonfo.dummy.EPubContent
import eu.schnuff.bonfo.dummy.Setting
import eu.schnuff.bonfo.helpers.RFastScroller
import eu.schnuff.bonfo.helpers.permissionExternalStorageHelper
import kotlinx.android.synthetic.main.activity_book_list.*
import kotlinx.android.synthetic.main.book_list.*
import org.jetbrains.anko.startActivity


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [BookDetailActivity] representing
 * item description. On tablets, the activity presents the list of items and
 * item description side-by-side using two vertical panes.
 */
class BookListActivity : AppCompatActivity() {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false
    private var firstItemIdx = 0
    private var firstItemOffset = 0

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

        val pref = this.getSharedPreferences(SETTING_UI_NAME, Context.MODE_PRIVATE)
        EPubContent.filter = pref.getString(SAVED_FILTER, "")!!
        firstItemIdx = pref.getInt(SAVED_SCROLL, -1)
        firstItemOffset = pref.getInt(SAVED_SCROLL_OFFSET, 0)

        Log.i("start_stop", "Filter is %s".format(EPubContent.filter))
        Log.i("start_stop", "Scroll is idx:%d, offset:%d".format(firstItemIdx, firstItemOffset))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_book_list_menu, menu)
        val searchAction = menu!!.findItem(R.id.app_bar_search)
        val searchView = searchAction.actionView as SearchView
        if (EPubContent.filter.isNotEmpty()) {
            searchView.setQuery(EPubContent.filter, false)
            searchView.isIconified = false
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

    override fun onPause() {
        super.onPause()
        val pref = this.getSharedPreferences(SETTING_UI_NAME, Context.MODE_PRIVATE)
        pref.edit {
            this.putString(SAVED_FILTER, EPubContent.filter)
            val firstItemIdx = (book_list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val firstItem = book_list.getChildAt(0)
            val scrollPosition = if (firstItem == null) 0 else (firstItem.top - book_list.paddingTop)
            this.putInt(SAVED_SCROLL, firstItemIdx)
            this.putInt(SAVED_SCROLL_OFFSET, scrollPosition)
            Log.i("start_stop", "Filter is %s".format(EPubContent.filter))
            Log.i("start_stop", "Scroll is idx:%d, offset:%d".format(firstItemIdx, scrollPosition))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId) {
        R.id.settings -> {
            startActivity<SettingsActivity>()
            true
        }
        R.id.filterLargeFile -> {
            item.isChecked = !item.isChecked
            Setting.setFilterLargeFiles(this, item.isChecked)
            EPubContent.filterLarge = item.isChecked
            true
        }
        R.id.menuRefresh -> {readEPubsForList(); true}
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupEPubList() {
        EPubContent.filterLarge = Setting.filterLargeFiles
        EPubContent.onListChanged = {
            runOnUiThread {
                book_list.adapter!!.notifyDataSetChanged()
                if (firstItemIdx != -1) {
                    (book_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            firstItemIdx,
                            firstItemOffset
                    )
                    firstItemIdx = -1
                    firstItemOffset = 0
                }
                book_list_refresh!!.isRefreshing = false
            }
        }

        if (!EPubContent.isLoaded) {
            book_list_refresh!!.isRefreshing = true
            EPubContent.loadItems(applicationContext) {
                book_list_refresh!!.isRefreshing = false
            }
        }
    }

    private fun readEPubsForList() {
        permissionExternalStorageHelper(this) {
            Log.d("main", "got permission, now starting service...")
            book_list_refresh!!.isRefreshing = true
            val intent = Intent(this, EPubRefreshService::class.java)
                    .setAction(EPubRefreshService.ACTION_START)
            startService(intent)
        }
    }


    private fun setupRecyclerView(recyclerView: RecyclerView) {
        book_list_refresh!!.setOnRefreshListener { readEPubsForList() }
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, twoPane)
    }

    companion object {
        const val SAVED_FILTER = "filter"
        const val SAVED_SCROLL = "scroll"
        const val SAVED_SCROLL_OFFSET = "scroll_offset"
        const val SETTING_UI_NAME = "ui_states"
    }
}
