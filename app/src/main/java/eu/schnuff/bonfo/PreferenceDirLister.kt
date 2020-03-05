package eu.schnuff.bonfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import kotlinx.android.synthetic.main.preference_dir_lister.view.*

class PreferenceDirLister(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {
    var onAddListener: (v: View) -> Unit = {}
    private var listView: ListView? = null
    var dirAdapter: ArrayAdapter<String>? = null
    set(value) {
        field = value
        this.listView?.adapter = value
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.buttonAdd ?: TODO("PreferenceDirLister has no ui.")
        holder.itemView.buttonAdd.setOnClickListener {
            onAddListener(it)
        }
        listView = holder.itemView.listContent
        listView!!.adapter = dirAdapter
        holder.itemView.visibility = View.VISIBLE
    }
}
