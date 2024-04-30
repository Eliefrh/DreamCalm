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

class SettingsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction().replace(R.id.settings_wrapper, Fragment())
            .commit()
        val startIntentMedia = Intent(this@SettingsActivity, MediaPlaybackService::class.java)
        startService(startIntentMedia)

    }

    class Fragment : PreferenceFragmentCompat() {
        private var filterCutoff: SeekBarPreference? = null
        private var theme: ListPreference? = null
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
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

        private fun updateThemeSummary() {
            theme!!.summary = theme!!.entry
        }

        private fun updateCutoffSummary(value: Int) {
            filterCutoff!!.summary = String.format(getString(R.string.filterCutoffValue), value)
        }

        private fun toggleCutoff(show: Boolean) {
            filterCutoff!!.isVisible = show
            val applyButton = activity?.findViewById<Button>(R.id.boutonAppuyer)
            applyButton?.isVisible = show

        }
    }

    fun appuyerSurBoutonAppliquer(view: View) {
        val stopMusicIntent = Intent("STOP_MUSIC_ACTION")
        sendBroadcast(stopMusicIntent)
    }

}