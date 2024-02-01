package protect.babysleepsounds

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate

class Preferences private constructor(context: Context?) {
    private val preferences: SharedPreferences

    init {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    val isLowPassFilterEnabled: Boolean
        get() = preferences.getBoolean(LOW_PASS_FILTER_ENABLED, false)
    val lowPassFilterFrequency: Int
        get() = preferences.getInt(LOW_PASS_FILTER_FREQUENCY, 1000)
    val theme: String?
        get() = preferences.getString(THEME, THEME_LIGHT)

    @JvmOverloads
    fun applyTheme(theme: String? = this.theme) {
        val dayNightMode: Int
        dayNightMode = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(dayNightMode)
    }

    companion object {
        private const val LOW_PASS_FILTER_ENABLED = "filter_enabled"
        private const val LOW_PASS_FILTER_FREQUENCY = "filter_value"
        private const val THEME = "theme"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        private var instance: Preferences? = null
        operator fun get(context: Context?): Preferences? {
            if (instance == null) {
                instance = Preferences(context)
            }
            return instance
        }
    }
}