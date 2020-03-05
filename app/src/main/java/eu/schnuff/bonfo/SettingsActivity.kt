package eu.schnuff.bonfo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, PreferenceGeneral())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}