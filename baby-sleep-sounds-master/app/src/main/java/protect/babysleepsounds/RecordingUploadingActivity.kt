package protect.babysleepsounds

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
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
import androidx.core.net.toUri
import org.w3c.dom.Text
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
    private lateinit var buttonRecording: ImageButton
    private lateinit var buttonPlaySound: Button
    private lateinit var buttonSavingFile: Button
    private lateinit var buttonSelectFile: ImageButton
    private var selectedAudioUri: Uri? = null
    private lateinit var playText: TextView
    private var mediaPlayer: MediaPlayer? = null

    val donnesVM: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_uploading)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Check for required permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                123
            )
        }
        isRecordingText = findViewById(R.id.isRecordingText)
        chronometer = findViewById(R.id.chronometer)
        buttonRecording = findViewById(R.id.recordingButton)
        buttonPlaySound = findViewById(R.id.playSound)
        buttonSavingFile = findViewById(R.id.saveFile)
        buttonSelectFile = findViewById(R.id.uploadingButton)
        buttonPlaySound.isEnabled = false
        buttonSavingFile.isEnabled = false
        playText = findViewById(R.id.playSound)
        playText.setText("Play")
        // Check if a recording is in progress
        val recordingFilePath = donnesVM.recordingFilePath
        if (!recordingFilePath.isNullOrEmpty()) {
            // Restore the recording file path and update UI accordingly
            outputFile = File(recordingFilePath)
            isRecordingText.visibility = TextView.VISIBLE
            isRecordingText.text = getString(R.string.recorded)
            chronometer.visibility = TextView.VISIBLE
            chronometer.base = SystemClock.elapsedRealtime() + donnesVM.recordingElapsedTime
            buttonPlaySound.isEnabled = true
            buttonSavingFile.isEnabled = true
        }
        buttonRecording.setOnClickListener {
            if (!isRecording) {
                buttonRecording.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorPrimary
                    )
                )
                startRecording()
            } else {
                buttonRecording.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorPrimaryPale
                    )
                )
                stopRecording()
            }
        }
        buttonPlaySound.setOnClickListener {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer!!.stop()
                mediaPlayer!!.release()
                mediaPlayer = null
                playText.text = "Play"
            } else {
                playText.text = "Stop"
                selectedAudioUri?.let { uri ->
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(this@RecordingUploadingActivity, uri)
                        prepare()
                        start()
                        setOnCompletionListener {
                            release()
                            mediaPlayer = null
                            playText.text = "Play"
                        }
                    }
                } ?: outputFile?.let { file ->
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            release()
                            mediaPlayer = null
                            playText.text = "Play"
                        }
                    }
                }
            }
        }
        buttonSavingFile.setOnClickListener {
            saveRecording()
            finish()
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer!!.stop()
                mediaPlayer!!.release()
                mediaPlayer = null
                playText.text = "Play"
            }
        }
        buttonSelectFile.setOnClickListener {
            selectAudioFile()
        }
    }

    private fun startRecording() {
        isRecordingText.visibility = TextView.VISIBLE
        isRecordingText.text = getString(R.string.isRecordingText)
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
                Toast.makeText(
                    this@RecordingUploadingActivity,
                    "Failed to start recording",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        isRecording = true
    }

    private fun stopRecording() {
        isRecordingText.text = getString(R.string.recorded)

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
        selectedAudioUri = outputFile?.toUri()
    }

    private fun saveRecording() {
        val destinationDir =
            File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "SelectedSounds")
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        selectedAudioUri?.let { uri ->
            val inputStream = contentResolver.openInputStream(uri)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val destinationFile = File(destinationDir, "SEL_$timeStamp.3gp")

            inputStream?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "Selected audio saved", Toast.LENGTH_SHORT).show()

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

    private fun selectAudioFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
        }
        startActivityForResult(intent, 123)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            selectedAudioUri = data?.data
            buttonPlaySound.isEnabled = true
            buttonSavingFile.isEnabled = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
            playText.text = "Play"
        }
    }
}
