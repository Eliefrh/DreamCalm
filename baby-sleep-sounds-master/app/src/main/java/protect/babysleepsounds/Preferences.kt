package protect.babysleepsounds

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate

/**
 * This is the Preferences class that handles the application's preferences.
 * It provides methods to get and set the low pass filter frequency, check if the low pass filter is enabled, and get the theme.
 * It also provides a method to apply the theme to the app.
 *
 * @param context This is the context in which the Preferences class is used.
 */
class Preferences private constructor(context: Context?) {
    private val preferences: SharedPreferences

    init {
        // Initialize SharedPreferences
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }
    /**
     * This property checks if the low pass filter is enabled.
     * @return Boolean value indicating whether the low pass filter is enabled.
     */
    val isLowPassFilterEnabled: Boolean
        get() = preferences.getBoolean(LOW_PASS_FILTER_ENABLED, false)
    /**
     * This property gets the low pass filter frequency.
     * @return Integer value of the low pass filter frequency.
     */
    val lowPassFilterFrequency: Int
        get() = preferences.getInt(LOW_PASS_FILTER_FREQUENCY, 1000)
    /**
     * This property gets the theme.
     * @return String value of the theme.
     */
    val theme: String?
        get() = preferences.getString(THEME, THEME_LIGHT)

    /**
     * This function applies the theme to the app.
     * @param theme This is the theme to be applied. If not provided, the current theme is used.
     */
    @JvmOverloads
    fun applyTheme(theme: String? = this.theme) {
        // Determine the day/night mode based on the theme
        val dayNightMode: Int
        dayNightMode = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        // Set the day/night mode
        AppCompatDelegate.setDefaultNightMode(dayNightMode)
    }
    /**
     * This function sets the low pass filter frequency.
     * @param value This is the new value of the low pass filter frequency.
     */
    fun setLowPassFilterFrequency(value: Int) {
        preferences.edit().putInt(LOW_PASS_FILTER_FREQUENCY, value).apply()
    }

    /**
     * This function gets the instance of the Preferences class.
     * If the instance is null, a new instance is created.
     * @param context This is the context in which the Preferences class is used.
     * @return Instance of the Preferences class.
     */
    companion object {
        private const val LOW_PASS_FILTER_ENABLED = "filter_enabled"
        const val LOW_PASS_FILTER_FREQUENCY = "filter_value"
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