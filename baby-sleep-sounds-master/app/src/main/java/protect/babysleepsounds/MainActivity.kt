package protect.babysleepsounds

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.common.collect.ImmutableMap
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.LinkedList
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private var _soundMap: Map<String, Int>? = null
    private var _timeMap: Map<String, Int>? = null
    private var _playing = false
    private var _timer: Timer? = null
    private var _ffmpeg: FFmpeg? = null
    private var _encodingProgress: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        Preferences[this]!!.applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // These sound files by convention are:
        // - take a ~10 second clip
        // - Apply a 2 second fade-in and fade-out
        // - Cut the first 3 seconds, and place it over the last three seconds
        //   which should create a seamless track appropriate for looping
        // - Save as a mp3 file, 128kbps, stereo
        _soundMap = ImmutableMap.builder<String, Int>()
            .put(resources.getString(R.string.campfire), R.raw.campfire)
            .put(resources.getString(R.string.dryer), R.raw.dryer)
            .put(resources.getString(R.string.fan), R.raw.fan)
            .put(resources.getString(R.string.ocean), R.raw.ocean)
            .put(resources.getString(R.string.rain), R.raw.rain)
            .put(resources.getString(R.string.refrigerator), R.raw.refrigerator)
            .put(resources.getString(R.string.shhhh), R.raw.shhhh)
            .put(resources.getString(R.string.shower), R.raw.shower)
            .put(resources.getString(R.string.stream), R.raw.stream)
            .put(resources.getString(R.string.vacuum), R.raw.vacuum)
            .put(resources.getString(R.string.water), R.raw.water)
            .put(resources.getString(R.string.waterfall), R.raw.waterfall)
            .put(resources.getString(R.string.waves), R.raw.waves)
            .put(resources.getString(R.string.whiteNoise), R.raw.white_noise)
            .build()
        _timeMap = ImmutableMap.builder<String, Int>()
            .put(resources.getString(R.string.disabled), 0)
            .put(resources.getString(R.string.time_1minute), 1000 * 60 * 1)
            .put(resources.getString(R.string.time_5minute), 1000 * 60 * 5)
            .put(resources.getString(R.string.time_10minute), 1000 * 60 * 10)
            .put(resources.getString(R.string.time_30minute), 1000 * 60 * 30)
            .put(resources.getString(R.string.time_1hour), 1000 * 60 * 60 * 1)
            .put(resources.getString(R.string.time_2hour), 1000 * 60 * 60 * 2)
            .put(resources.getString(R.string.time_4hour), 1000 * 60 * 60 * 4)
            .put(resources.getString(R.string.time_8hour), 1000 * 60 * 60 * 8)
            .build()
        val soundSpinner = findViewById<Spinner>(R.id.soundSpinner)
        val names: List<String> = _soundMap?.keys?.toList() ?: emptyList()
        val dataAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, names
        )
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        soundSpinner.adapter = dataAdapter
        val sleepTimeoutSpinner = findViewById<Spinner>(R.id.sleepTimerSpinner)
        val times: List<String> = _timeMap?.keys?.toList() ?: emptyList()
        sleepTimeoutSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (_playing) {
                    updatePlayTimeout()
                    Toast.makeText(this@MainActivity, R.string.sleepTimerUpdated, Toast.LENGTH_LONG)
                        .show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // noop
            }
        }
        val timesAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, times
        )
        timesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sleepTimeoutSpinner.adapter = timesAdapter
        volumeControlStream = AudioManager.STREAM_MUSIC
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            if (_playing == false) {
                startPlayback()
            } else {
                stopPlayback()
            }
        }
        _ffmpeg = FFmpeg.getInstance(this)
        if (_ffmpeg is FFmpeg && _ffmpeg!!.isSupported()) {
            button.isEnabled = true
        } else {
            Log.d(TAG, "ffmpeg not supported")
            reportPlaybackUnsupported()
        }
    }

    /**
     * Report to the user that playback is not supported on this device
     */
    private fun reportPlaybackUnsupported() {
        Toast.makeText(this, R.string.playbackNotSupported, Toast.LENGTH_LONG).show()
    }

    private fun startPlayback() {
        val soundSpinner = findViewById<Spinner>(R.id.soundSpinner)
        val selectedSound = soundSpinner.selectedItem as String
        val id = _soundMap!![selectedSound]!!
        volumeControlStream = AudioManager.STREAM_MUSIC
        try {
            val originalFile = File(filesDir, ORIGINAL_MP3_FILE)
            Log.i(TAG, "Writing file out prior to WAV conversion")
            writeToFile(id, originalFile)
            val processed = File(filesDir, PROCESSED_RAW_FILE)
            if (processed.exists()) {
                val result = processed.delete()
                if (result == false) {
                    throw IOException("Unable to delete previous file, cannot prepare new file")
                }
            }
            Log.i(TAG, "Converting file to WAV")
            val arguments = LinkedList<String>()
            arguments.add("-i")
            arguments.add(originalFile.absolutePath)
            if (Preferences[this]!!.isLowPassFilterEnabled) {
                val frequencyValue = Preferences[this]!!.lowPassFilterFrequency
                Log.i(TAG, "Will perform lowpass filter to $frequencyValue Hz")
                arguments.add("-af")
                arguments.add("lowpass=frequency=$frequencyValue")
            }
            arguments.add("-f")
            arguments.add("s16le")
            arguments.add("-acodec")
            arguments.add("pcm_s16le")
            arguments.add(processed.absolutePath)
            _encodingProgress = ProgressDialog(this)
            _encodingProgress!!.setMessage(getString(R.string.preparing))
            _encodingProgress!!.show()
            Log.i(TAG, "Launching ffmpeg")
            val cmd = arguments.toTypedArray()
            _ffmpeg!!.execute(cmd, object : ExecuteBinaryResponseHandler() {
                override fun onStart() {
                    Log.d(TAG, "ffmpeg execute onStart()")
                }

                override fun onSuccess(message: String) {
                    Log.d(TAG, "ffmpeg execute onSuccess(): $message")
                    val startIntent = Intent(this@MainActivity, AudioService::class.java)
                    startIntent.putExtra(AudioService.AUDIO_FILENAME_ARG, processed.absolutePath)
                    startService(startIntent)
                    updateToPlaying()
                }

                override fun onProgress(message: String) {
                    Log.d(TAG, "ffmpeg execute onProgress(): $message")
                }

                override fun onFailure(message: String) {
                    Log.d(TAG, "ffmpeg execute onFailure(): $message")
                    reportPlaybackFailure()
                }

                override fun onFinish() {
                    Log.d(TAG, "ffmpeg execute onFinish()")
                }
            })
        } catch (e: IOException) {
            Log.i(TAG, "Failed to start playback", e)
            reportPlaybackFailure()
        } catch (e: FFmpegCommandAlreadyRunningException) {
            Log.i(TAG, "Failed to start playback", e)
            reportPlaybackFailure()
        }
    }

    /**
     * Write a resource to a file
     * @param resource resource to write
     * @param output destination of the resource
     * @throws IOException if a write failure occurs
     */
    @Throws(IOException::class)
    private fun writeToFile(resource: Int, output: File) {
        val rawStream = resources.openRawResource(resource)
        var outStream: FileOutputStream? = null
        val buff = ByteArray(1024)
        var read: Int
        try {
            outStream = FileOutputStream(output)
            while (rawStream.read(buff).also { read = it } > 0) {
                outStream.write(buff, 0, read)
            }
        } finally {
            try {
                rawStream.close()
            } catch (e: IOException) {
                // If it fails, there is nothing to do
            }
            outStream?.close()
        }
    }

    /**
     * Report to the user that playback has failed, and hide the progress dialog
     */
    private fun reportPlaybackFailure() {
        if (_encodingProgress != null) {
            _encodingProgress!!.dismiss()
            _encodingProgress = null
        }
        Toast.makeText(this, R.string.playbackFailure, Toast.LENGTH_LONG).show()
    }

    /**
     * Update the timeout for playback to stop
     */
    private fun updatePlayTimeout() {
        // Cancel the running timer
        if (_timer != null) {
            _timer!!.cancel()
            _timer!!.purge()
        }
        val sleepTimeoutSpinner = findViewById<Spinner>(R.id.sleepTimerSpinner)
        val selectedTimeout = sleepTimeoutSpinner.selectedItem as String
        val timeoutMs = _timeMap!![selectedTimeout]!!
        if (timeoutMs > 0) {
            _timer = Timer()
            _timer!!.schedule(object : TimerTask() {
                override fun run() {
                    stopPlayback()
                }
            }, timeoutMs.toLong())
        }
    }

    /**
     * Update the UI to reflect it is playing
     */
    private fun updateToPlaying() {
        _playing = true
        runOnUiThread {
            updatePlayTimeout()
            val button = findViewById<Button>(R.id.button)
            button.setText(R.string.stop)
            setControlsEnabled(false)
            if (_encodingProgress != null) {
                _encodingProgress!!.hide()
                _encodingProgress = null
            }
        }
    }

    private fun stopPlayback() {
        val stopIntent = Intent(this@MainActivity, AudioService::class.java)
        startService(stopIntent)
        _playing = false
        if (_timer != null) {
            _timer!!.cancel()
            _timer!!.purge()
            _timer = null
        }
        runOnUiThread {
            val button = findViewById<Button>(R.id.button)
            button.setText(R.string.play)
            setControlsEnabled(true)
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        for (resId in intArrayOf(R.id.soundSpinner)) {
            val view = findViewById<View>(resId)
            view.isEnabled = enabled
        }
    }

    override fun onDestroy() {
        if (_playing) {
            stopPlayback()
        }
        for (toDelete in arrayOf(ORIGINAL_MP3_FILE, PROCESSED_RAW_FILE)) {
            val file = File(filesDir, toDelete)
            val result = file.delete()
            if (result == false) {
                Log.w(TAG, "Failed to delete file on exit: " + file.absolutePath)
            }
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        } else if (id == R.id.action_about) {
            displayAboutDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayAboutDialog() {
        val USED_LIBRARIES: Map<String, String> = ImmutableMap.of(
            "FFmpeg", "https://ffmpeg.org/",
            "FFmpeg-Android", "https://github.com/writingminds/ffmpeg-android"
        )
        val SOUND_RESOURCES: Map<String, String> = ImmutableMap.of(
            "Canton Becker",
            "http://whitenoise.cantonbecker.com/",
            "The MC2 Method",
            "http://mc2method.org/white-noise/",
            "Campfire-1.mp3 Copyright SoundJay.com Used with Permission",
            "https://www.soundjay.com/nature/campfire-1.mp3"
        )
        val IMAGE_RESOURCES: Map<String, String> = ImmutableMap.of(
            "'Music' by Aleks from the Noun Project",
            "https://thenounproject.com/term/music/886761/"
        )
        val libs = StringBuilder().append("<ul>")
        for ((key, value) in USED_LIBRARIES) {
            libs.append("<li><a href=\"").append(value).append("\">").append(key)
                .append("</a></li>")
        }
        libs.append("</ul>")
        val soundResources = StringBuilder().append("<ul>")
        for ((key, value) in SOUND_RESOURCES) {
            soundResources.append("<li><a href=\"").append(value).append("\">").append(key)
                .append("</a></li>")
        }
        soundResources.append("</ul>")
        val imageResources = StringBuilder().append("<ul>")
        for ((key, value) in IMAGE_RESOURCES) {
            imageResources.append("<li><a href=\"").append(value).append("\">").append(key)
                .append("</a></li>")
        }
        imageResources.append("</ul>")
        val appName = getString(R.string.app_name)
        val year = Calendar.getInstance()[Calendar.YEAR]
        var version = "?"
        try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            version = pi.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package name not found", e)
        }
        val wv = WebView(this)
        val html = "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" +
                "<img src=\"file:///android_res/mipmap/ic_launcher.png\" alt=\"" + appName + "\"/>" +
                "<h1>" + String.format(
            getString(R.string.about_title_fmt),
            "<a href=\"" + getString(R.string.app_webpage_url)
        ) + "\">" +
                appName +
                "</a>" +
                "</h1><p>" +
                appName +
                " " + String.format(getString(R.string.debug_version_fmt), version) +
                "</p><p>" + String.format(
            getString(R.string.app_revision_fmt),
            "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                    getString(R.string.app_revision_url) +
                    "</a>"
        ) +
                "</p><hr/><p>" + String.format(getString(R.string.app_copyright_fmt), year) +
                "</p><hr/><p>" +
                getString(R.string.app_license) +
                "</p><hr/><p>" + String.format(
            getString(R.string.sound_resources),
            appName,
            soundResources.toString()
        ) +
                "</p><hr/><p>" + String.format(
            getString(R.string.image_resources),
            appName,
            imageResources.toString()
        ) +
                "</p><hr/><p>" + String.format(
            getString(R.string.app_libraries),
            appName,
            libs.toString()
        )
        wv.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null)
        AlertDialog.Builder(this)
            .setView(wv)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, which -> dialog.dismiss() }
            .show()
    }

    companion object {
        private const val TAG = "BabySleepSounds"
        private const val ORIGINAL_MP3_FILE = "original.mp3"
        private const val PROCESSED_RAW_FILE = "processed.raw"
    }
}