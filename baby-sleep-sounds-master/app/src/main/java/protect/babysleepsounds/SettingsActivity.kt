package protect.babysleepsounds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
/**
 * This is the SettingsActivity class that extends AppCompatActivity.
 * It is responsible for handling the settings of the application.
 */
class SettingsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        // Replace the fragment with the preference fragment
        supportFragmentManager.beginTransaction().replace(R.id.settings_wrapper, Fragment())
            .commit()
        // Start the media playback service
        val startIntentMedia = Intent(this@SettingsActivity, MediaPlaybackService::class.java)
        startService(startIntentMedia)

    }

    class Fragment : PreferenceFragmentCompat() {
        private var filterCutoff: SeekBarPreference? = null
        private var theme: ListPreference? = null
        /**
         * This is the onCreatePreferences function that is called during onCreate(Bundle) to supply the preferences for this fragment.
         * @param savedInstanceState This is the first parameter to onCreatePreferences method which is a Bundle object containing the fragment's previously saved state.
         * @param rootKey If non-null, this preference fragment should be rooted at the PreferenceScreen with this key.
         */
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
            // Initialize and set listeners for preferences
            filterCutoff = findPreference("filter_cutoff")
            filterCutoff!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    updateCutoffSummary(newValue as Int)

                    Preferences[activity]?.setLowPassFilterFrequency(newValue)
                    true
                }

            val filterEnabled = findPreference<SwitchPreferenceCompat>("filter_enabled")
            filterEnabled!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    toggleCutoff(newValue as Boolean)

                    true
                }
            toggleCutoff(true)

            theme = findPreference("theme")
            theme!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    Preferences.Companion.get(activity)!!.applyTheme(newValue as String)
                    true
                }
            toggleCutoff(filterEnabled.isChecked)
            updateCutoffSummary(filterCutoff!!.value)
            updateThemeSummary()
        }
        /**
         * This is the updateThemeSummary function that updates the summary of the theme preference.
         */
        private fun updateThemeSummary() {
            theme!!.summary = theme!!.entry
        }

        /**
         * This is the updateCutoffSummary function that updates the summary of the filter cutoff preference.
         * @param value This is the first parameter to updateCutoffSummary method which is the new value of the filter cutoff preference.
         */
        private fun updateCutoffSummary(value: Int) {
            filterCutoff!!.summary = String.format(getString(R.string.filterCutoffValue), value)
        }
        /**
         * This is the toggleCutoff function that toggles the visibility of the filter cutoff preference.
         * @param show This is the first parameter to toggleCutoff method which determines whether the filter cutoff preference should be visible or not.
         */
        private fun toggleCutoff(show: Boolean) {
            filterCutoff!!.isVisible = show
            val applyButton = activity?.findViewById<Button>(R.id.boutonAppuyer)
            applyButton?.isVisible = show

        }
    }
    /**
     * This is the appuyerSurBoutonAppliquer function that is called when the apply button is clicked.
     * @param view This is the first parameter to appuyerSurBoutonAppliquer method which is the view that was clicked.
     */
    fun appuyerSurBoutonAppliquer(view: View) {
        val stopMusicIntent = Intent("STOP_MUSIC_ACTION")
        sendBroadcast(stopMusicIntent)
    }

}