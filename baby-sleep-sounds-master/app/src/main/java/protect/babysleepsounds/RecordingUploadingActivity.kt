package protect.babysleepsounds
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingUploadingActivity : AppCompatActivity() {

    private var isRecording = false
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private lateinit var isRecordingText: TextView
    private lateinit var timerText: TextView
    private lateinit var chronometer: Chronometer
    private lateinit var buttonRecording : ImageButton
    private lateinit var buttonPlaySound: Button
    private lateinit var buttonSavingFile : Button

    val donnesVM: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_uploading)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Check for required permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), 123)
        }
        isRecordingText = findViewById(R.id.isRecordingText)
        chronometer = findViewById(R.id.chronometer)
        buttonRecording= findViewById(R.id.recordingButton)
        buttonPlaySound = findViewById(R.id.playSound)
        buttonSavingFile = findViewById(R.id.saveFile)
        buttonPlaySound.isEnabled = false
        buttonSavingFile.isEnabled = false
        // Check if a recording is in progress
        val recordingFilePath = donnesVM.recordingFilePath
        if (!recordingFilePath.isNullOrEmpty()) {
            // Restore the recording file path and update UI accordingly
            outputFile = File(recordingFilePath)
            isRecordingText.visibility = TextView.VISIBLE
            isRecordingText.text ="Recorded a sound"
            chronometer.visibility = TextView.VISIBLE
            chronometer.base = SystemClock.elapsedRealtime() + donnesVM.recordingElapsedTime
            buttonPlaySound.isEnabled = true
            buttonSavingFile.isEnabled = true
        }
        buttonRecording.setOnClickListener {
            if (!isRecording) {
                buttonRecording.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                startRecording()
            } else {
                buttonRecording.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryPale))
                stopRecording()
            }
        }
        buttonPlaySound.setOnClickListener {

            outputFile?.let { file ->
                // Play the recorded audio
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        // Release the MediaPlayer resources when playback completes
                        release()
                    }
                }
            }
        }
        buttonSavingFile.setOnClickListener {
            saveRecording()
        }
    }

    private fun startRecording() {

        isRecordingText.visibility = TextView.VISIBLE
        isRecordingText.text ="Recording ..."
        chronometer.visibility = TextView.VISIBLE

        // Start the chronometer
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            outputFile = getOutputMediaFile()
            setOutputFile(outputFile!!.absolutePath)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Toast.makeText(this@RecordingUploadingActivity, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        }

        isRecording = true
    }
    private fun stopRecording() {
        // Stop the chronometer
        chronometer.stop()
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        buttonPlaySound.isEnabled = true
        buttonSavingFile.isEnabled = true
        donnesVM.recordingFilePath = outputFile?.absolutePath
        donnesVM.recordingElapsedTime = chronometer.base - SystemClock.elapsedRealtime()
    }
    private fun saveRecording() {
        if (outputFile != null) {
            // Implement saving logic here (e.g., copying the file to another directory)
            // For example:
            // val destinationFile = File(getExternalFilesDir(null), "my_recorded_audio.mp3")
            // outputFile?.copyTo(destinationFile, overwrite = true)
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No recording to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOutputMediaFile(): File {
        val mediaStorageDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MyRecordings")
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(mediaStorageDir.path + File.separator + "REC_$timeStamp.3gp")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if(isRecording) {
            stopRecording()
        }
    }
}