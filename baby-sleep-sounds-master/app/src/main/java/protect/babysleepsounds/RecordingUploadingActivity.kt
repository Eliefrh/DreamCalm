package protect.babysleepsounds

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
        playText.setText(R.string.playSound)
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
                playText.text = getString(R.string.playSound)
            } else {
                playText.text =  getString(R.string.stop)
                selectedAudioUri?.let { uri ->
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(this@RecordingUploadingActivity, uri)
                        prepare()
                        start()
                        setOnCompletionListener {
                            release()
                            mediaPlayer = null
                            playText.text = getString(R.string.playSound)
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
                            playText.text =  getString(R.string.playSound)
                        }
                    }
                }
            }
        }
        buttonSavingFile.setOnClickListener {
            showSaveDialog()
            //finish()
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer!!.stop()
                mediaPlayer!!.release()
                mediaPlayer = null
                playText.text =  getString(R.string.playSound)
            }
        }
        buttonSelectFile.setOnClickListener {
            selectAudioFile()
        }
    }
    private fun showSaveDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_name, null)
        val editTextName = dialogView.findViewById<EditText>(R.id.editTextName)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.saveRec))
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setPositiveButton(getString(R.string.saveFile), null)
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel), null)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.setOnShowListener {
            val saveButtonColor = if (Preferences[this]?.theme == Preferences.THEME_LIGHT) {
                ContextCompat.getColor(this, R.color.menuItemTextColorLight) //  color for light theme
            } else {
                ContextCompat.getColor(this, R.color.menuItemTextColorDark)
                // Blue color for dark theme
            }
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(saveButtonColor)      // Similarly, set the text color for the negative button
            val cancelButtonColor = if (Preferences[this]?.theme == Preferences.THEME_LIGHT) {
                ContextCompat.getColor(this, R.color.menuItemTextColorLight)  //  color for light theme
            } else {
                ContextCompat.getColor(this, R.color.menuItemTextColorDark)
            }
            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(cancelButtonColor)
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                val name = editTextName.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveRecording(name)
                } else {
                    Toast.makeText(this, getString(R.string.please), Toast.LENGTH_SHORT).show()
                }
                alertDialog.dismiss()
            }
        }
        alertDialog.show()
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

    private fun saveRecording(name: String) {
        // Your existing saveRecording logic
        val destinationDir =
            File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "SelectedSounds")
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        selectedAudioUri?.let { uri ->
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SEL_${name}_${timeStamp}.3gp"
            val destinationFile = File(destinationDir, fileName)

            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "Recording saved as $fileName", Toast.LENGTH_SHORT).show()
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
            playText.text = getString(R.string.playSound)
        }
    }
}
