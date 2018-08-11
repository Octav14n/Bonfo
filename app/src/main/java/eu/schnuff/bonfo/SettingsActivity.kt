package eu.schnuff.bonfo

import android.app.Activity
import android.os.Bundle

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        fragmentManager.beginTransaction().replace(android.R.id.content,
                PreferenceGeneral()).commit()
    }

}
