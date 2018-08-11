package eu.schnuff.bonfo

import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlinx.android.synthetic.main.preference_dir_lister.view.*

class PreferenceDirLister(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {
    var onAddListener: (v: View) -> Unit = {}
    private var listView: ListView? = null
    var dirAdapter: ArrayAdapter<String>? = null
    set(value) {
        field = value
        if (null != this.listView) {
            this.listView!!.adapter = value
        }
    }

    override fun onCreateView(parent: ViewGroup): View {
        super.onCreateView(parent)
        val li = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = li.inflate(R.layout.preference_dir_lister, parent, false)
        view.buttonAdd.setOnClickListener {
            onAddListener(it)
        }
        if (null != this.dirAdapter) {
            view.listContent.adapter = this.dirAdapter
        }
        return view
    }
}
