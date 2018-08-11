package eu.schnuff.bonfo

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import eu.schnuff.bonfo.dummy.Setting
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import net.rdrei.android.dirchooser.DirectoryChooserFragment
import java.util.*

/**
 * TODO: document your custom view class.
 */
class PreferenceGeneral : PreferenceFragment(), DirectoryChooserFragment.OnFragmentInteractionListener {
    private var mDialog: DirectoryChooserFragment
    private var dirAdapter: ArrayAdapter<String>? = null

    init {
        val config = DirectoryChooserConfig.builder()
                .newDirectoryName("DialogSample")
                .allowNewDirectoryNameModification(true)
                .allowReadOnlyDirectory(true)
                .build()
        mDialog = DirectoryChooserFragment.newInstance(config).apply { this.setTargetFragment(this@PreferenceGeneral, 0) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_general)

        Setting.load(context)
        dirAdapter = ArrayAdapter(context, R.layout.simple_text_view, ArrayList(Setting.watchedDirectory))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dirLister = preferenceScreen.findPreference("selected_directories") as PreferenceDirLister
        dirLister.onAddListener = { _ -> mDialog.show(fragmentManager, null) }
        dirLister.dirAdapter = dirAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            startActivity(Intent(activity, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSelectDirectory(path: String) {
        mDialog.dismiss()
        Setting.addPath(context, path)
        dirAdapter!!.add(path)
    }

    override fun onCancelChooser() {
        mDialog.dismiss()
    }
}
