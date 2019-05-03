package eu.schnuff.bonfo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.schnuff.bonfo.dummy.EPubContent
import eu.schnuff.bonfo.dummy.EPubItem
import eu.schnuff.bonfo.dummy.Setting
import kotlinx.android.synthetic.main.book_list_content.view.*
import org.jetbrains.anko.newTask
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.io.File

class SimpleItemRecyclerViewAdapter(private val parentActivity: BookListActivity,
                                    private val twoPane: Boolean) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

    private val onLongClickListener: View.OnLongClickListener
    private val onClickListener: View.OnClickListener
    private var lastClickedView: View? = null

    companion object {
        const val HIGHLIGHT = "<font color='%s'>$1</font>"
        val HIGHLIGHT_COLOR = arrayOf("#FFCC00", "#CCFF00", "#FF00CC", "#CC00FF")
        const val HIGHLIGHT_MIN = 2
    }

    init {
        onLongClickListener = View.OnLongClickListener { v ->
            val item = v.tag as EPubItem
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
            val item = it.tag as EPubItem
            Setting.setLastEpubOpenedPath(parentActivity.applicationContext, item.filePath)
            it.isSelected = true
            lastClickedView?.isSelected = false
            lastClickedView = it
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
            if (item.filePath == Setting.lastEpubOpenedPath) {
                isSelected = true
                lastClickedView = this
            } else if (isSelected) {
                isSelected = false
            }
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