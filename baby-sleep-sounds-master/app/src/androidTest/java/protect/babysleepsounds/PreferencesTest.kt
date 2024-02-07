import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import protect.babysleepsounds.Preferences

@RunWith(AndroidJUnit4::class)
class PreferencesTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = Preferences[context]!!
        sharedPreferences = context.getSharedPreferences("test_preferences", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    @Test
    fun testSetLowPassFilterFrequency() {
        // Set up
        val frequency = 2000 // Example frequency value

        // Perform
        preferences.setLowPassFilterFrequency(frequency)

        // Verify
        assertEquals(frequency, preferences.lowPassFilterFrequency)
    }
}