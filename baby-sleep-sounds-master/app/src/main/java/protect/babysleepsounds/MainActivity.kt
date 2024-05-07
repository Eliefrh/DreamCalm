package protect.babysleepsounds

import android.Manifest
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.common.collect.ImmutableMap
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedList
import java.util.Locale


data class SoundItem(val imageResId: Int)
data class AddedSoundItem(
    val imageResId: Int,
    val path: String,
    val name: String,
    val creationDate: String
)

class MainActivity : AppCompatActivity() {
    val donnesVM: MainActivityViewModel by viewModels()
    private var _soundMap: Map<Int, Int>? = null

    //    private var _addedSoundMap: Map<Int, String>? = null
    private var _timeMap: Map<String, Int>? = null
    private var _ffmpeg: FFmpeg? = null
    private var _encodingProgress: ProgressDialog? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    lateinit var soundItems: List<SoundItem>
    lateinit var buttonPlay: Button
    lateinit var gridviewSound: GridView
    lateinit var addedGridView: GridView
    private var addedSoundItem: MutableList<AddedSoundItem> = mutableListOf()
    private var isUserSelection = false

    //    private lateinit var mediaSession: MediaSession
    var countdownDuration = 60 * 1000L
    var timeLeftInMillis: Long = 0
    lateinit var sleepTimeoutSpinner: Spinner

    private var mediaPlayer: MediaPlayer? = null


    val sourcePath = "/baby-sleep-sounds-master/app/src/main/res/raw"
    val destinationPath = "/storage/emulated/0/Android/data/protect.babysleepsounds/files"

    // Déclaration de constantes pour les permissions
    private val REQUEST_PERMISSION_CODE = 123
    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)


    private val stopMusicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (donnesVM.isPlaying) {
                donnesVM.frequenceChanged = true
                stopPlayback()
                if (donnesVM.choosedGrid == 1) {
                    startPlayback()

                }
                if (donnesVM.choosedGrid == 2) {
                    startAddedPlayback(donnesVM.selectedImageposition as Int)
                }
            }
        }
    }

    private val stopStartMusicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (donnesVM.selectedImageposition != null) {
                if (donnesVM.isPlaying) {
                    stopPlayback()
                } else {
                    if (donnesVM.choosedGrid == 1) {
                        startPlayback()

                    }
                    if (donnesVM.choosedGrid == 2) {
                        startAddedPlayback(donnesVM.selectedImageposition as Int)
                    }
                }
            }

        }

    }

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val selectedView = sleepTimeoutSpinner.selectedView as? TextView
            if (intent != null) {
                selectedView?.text = intent.getStringExtra("timeLeftFormatted")
            }
        }
    }
    private val timerFinishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val selectedView = sleepTimeoutSpinner.selectedView as? TextView
            selectedView?.text = sleepTimeoutSpinner.selectedItem.toString()
            stopPlayback()
        }
    }

    override fun onStart() {
        val intentFilter = IntentFilter("STOP_MUSIC_ACTION")
        registerReceiver(stopMusicReceiver, intentFilter)

        val intentFilter3 = IntentFilter("STOP_START_MUSIC")
        registerReceiver(stopStartMusicReceiver, intentFilter3)

        registerReceiver(timerUpdateReceiver, IntentFilter("TIMER_UPDATE"))
        registerReceiver(timerFinishReceiver, IntentFilter("TIMER_FINISH"))
        val startIntentMedia = Intent(this@MainActivity, MediaPlaybackService::class.java)
        startService(startIntentMedia)
        scanSoundFolder()

        //Highlight le sons du feu par defaut
        if (donnesVM.selectedImageposition == null && donnesVM.choosedGrid == 0) {
            (gridviewSound.adapter as SoundAdapter).setSelectedItem(0)
        }
        //Highlight le son selectionné si la selection est sauvegardée
        else
            if (donnesVM.selectedImageposition != null) {
                if (donnesVM.choosedGrid == 1) {
                    (gridviewSound.adapter as SoundAdapter).setSelectedItem(donnesVM.selectedImageposition!!)
                } else if (donnesVM.choosedGrid == 2) {
                    (addedGridView.adapter as AddedSoundAdapter).setSelectedItem(donnesVM.selectedImageposition!!)
                }
            }


        if (donnesVM.isPlaying) {
            val button = findViewById<Button>(R.id.button)
            button.setText(R.string.stop)
            setControlsEnabled(true)
        }

        if (donnesVM.isSwitch1On) {
            initialiserGif(true, findViewById(R.id.playingSound))
        } else {
            initialiserGif(false, findViewById(R.id.playingSound))
        }
        super.onStart()
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        Preferences[this]!!.applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        checkPermissions()
        sleepTimeoutSpinner = findViewById(R.id.sleepTimerSpinner)
        buttonPlay = findViewById(R.id.button)
        buttonPlay.isEnabled = true
        if (donnesVM.selectedImageposition == null) {
            donnesVM.selectedImageposition = 0
            donnesVM.choosedGrid = 1
            donnesVM.isPlaying = false
        }
        val filesDir = filesDir
        var playingMusicImg = findViewById<ImageView>(R.id.playingSound)


        //Switch pour turn on/off les Gifs
        val switch1 = findViewById<Switch>(R.id.switch1)
        switch1.isChecked = donnesVM.isSwitch1On

        switch1.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked == true) {
                switch1.text = "Gif"
                donnesVM.isSwitch1On = isChecked
            } else {
                switch1.text = "Icon"
                donnesVM.isSwitch1On = isChecked
            }

            initialiserGif(donnesVM.isSwitch1On, playingMusicImg)

        }

//        initialiserGif(donnesVM.isSwitch1On, playingMusicImg)


        // Initialize BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, R.string.bluetoothnot, Toast.LENGTH_SHORT)
                .show()
        }

        initializeApp()
        scanSoundFolder()

        gridviewSound = findViewById(R.id.gridView)
        soundItems = _soundMap?.keys?.map { SoundItem(it) } ?: emptyList()
        val adapter = SoundAdapter(this, soundItems)
        gridviewSound.adapter = adapter

        val addedText: TextView = findViewById(R.id.recentlyadded)


        addedGridView = findViewById<GridView>(R.id.gridView_ajoute)
        val parentView = findViewById<View>(R.id.visibil)
        val addedAdapter = AddedSoundAdapter(this, addedSoundItem)
        addedGridView.adapter = addedAdapter
        addedGridView.visibility = View.GONE
        val addedFlesh: ImageView = findViewById(R.id.fleshImg)

        addedFlesh.setOnClickListener {
            if (addedGridView.visibility == View.VISIBLE) {
                addedGridView.visibility = View.GONE
                if (parentView != null) {

                    val params = gridviewSound.layoutParams
                    params.height =
                        (parentView.height * 0.75).toInt() // 80% of the parent view's height
                    gridviewSound.layoutParams = params
                }
            } else {
                addedGridView.visibility = View.VISIBLE
                if (parentView != null) {
                    val params = gridviewSound.layoutParams
                    params.height =
                        (parentView.height * 0.3).toInt() // 80% of the parent view's height
                    gridviewSound.layoutParams = params
                }
            }
        }

        addedText.setOnClickListener {
            if (addedGridView.visibility == View.VISIBLE) {
                addedGridView.visibility = View.GONE
                if (parentView != null) {

                    val params = gridviewSound.layoutParams
                    params.height =
                        (parentView.height * 0.75).toInt() // 80% of the parent view's height
                    gridviewSound.layoutParams = params
                }
            } else {
                addedGridView.visibility = View.VISIBLE
                if (parentView != null) {
                    val params = gridviewSound.layoutParams
                    params.height =
                        (parentView.height * 0.3).toInt() // 80% of the parent view's height
                    gridviewSound.layoutParams = params
                }
            }
        }


        playingMusicImg.setImageResource(R.mipmap.campfire_foreground)

//        if (donnesVM.choosedGrid == 1) {
//            var choosenGif: Int? = null
//            if (isSwitch1On) {
//                when (donnesVM.selectedImageposition) {
//                    0 -> choosenGif = R.drawable.campfire
//                    1 -> choosenGif = R.drawable.dryer
//                    2 -> choosenGif = R.drawable.fan
//                    3 -> choosenGif = R.drawable.ocean
//                    4 -> choosenGif = R.drawable.rain
//                    5 -> choosenGif = R.drawable.refrigerator
//                    6 -> choosenGif = R.drawable.shhhh
//                    7 -> choosenGif = R.drawable.shower
//                    8 -> choosenGif = R.drawable.stream
//                    9 -> choosenGif = R.drawable.vacuum
//                    10 -> choosenGif = R.drawable.water
//                    11 -> choosenGif = R.drawable.waterfall
//                    12 -> choosenGif = R.drawable.waves
//                    13 -> choosenGif = R.drawable.white_noise
//                }
//
//                Glide.with(this)
//                    .load(choosenGif)
//                    .into(playingMusicImg)
//            }else{
//                playingMusicImg.setImageResource(soundItems[donnesVM.selectedImageposition!!].imageResId)
//            }
//        }


        gridviewSound.setOnItemClickListener { parent, view, position, id ->
            if (donnesVM.isGridViewClickable) {

                //Remove the highlight from the other item
                (addedGridView.adapter as AddedSoundAdapter).setSelectedItem(-1)


                buttonPlay.isEnabled = true
                // Store the selected position in a variable
                donnesVM.selectedImageposition = position
                playingMusicImg.setImageResource(soundItems[position].imageResId)
                donnesVM.choosedGrid = 1

                //highlight the selected item
                (gridviewSound.adapter as SoundAdapter).setSelectedItem(donnesVM.selectedImageposition!!)

                initialiserGif(donnesVM.isSwitch1On, playingMusicImg)
//                var choosenGif: Int? = null
//
//                if (isSwitch1On) {
//
//                    when (donnesVM.selectedImageposition) {
//                        0 -> choosenGif = R.drawable.campfire
//                        1 -> choosenGif = R.drawable.dryer
//                        2 -> choosenGif = R.drawable.fan
//                        3 -> choosenGif = R.drawable.ocean
//                        4 -> choosenGif = R.drawable.rain
//                        5 -> choosenGif = R.drawable.refrigerator
//                        6 -> choosenGif = R.drawable.shhhh
//                        7 -> choosenGif = R.drawable.shower
//                        8 -> choosenGif = R.drawable.stream
//                        9 -> choosenGif = R.drawable.vacuum
//                        10 -> choosenGif = R.drawable.water
//                        11 -> choosenGif = R.drawable.waterfall
//                        12 -> choosenGif = R.drawable.waves
//                        13 -> choosenGif = R.drawable.white_noise
//                    }
//                    Glide.with(this)
//                        .load(choosenGif)
//                        .into(playingMusicImg)
//
//                }else{
//                    playingMusicImg.setImageResource(soundItems[donnesVM.selectedImageposition!!].imageResId)
//                }
            }
        }

        addedGridView.setOnItemClickListener { parent, view, position, id ->
            if (donnesVM.isGridViewClickable) {

                //Remove the highlight from the other item
                (gridviewSound.adapter as SoundAdapter).setSelectedItem(-1)

                buttonPlay.isEnabled = true
                // Store the selected position in a variable
                donnesVM.selectedImageposition = position
                playingMusicImg.setImageResource(addedSoundItem[position].imageResId)
                donnesVM.choosedGrid = 2
                (addedGridView.adapter as AddedSoundAdapter).setSelectedItem(donnesVM.selectedImageposition!!)

                initialiserGif(donnesVM.isSwitch1On, playingMusicImg)

//                if (donnesVM.isSwitch1On == true) {
//                    //AJOUT DU GIF
//                    Glide.with(this)
//                        .load(R.drawable.music_notes)
//                        .into(playingMusicImg)
//                }
//                else {
//                    playingMusicImg.setImageResource(R.mipmap.music_notes)
//
//                }
            }
        }
        if (donnesVM.selectedImageposition != null) {
            buttonPlay.isEnabled = true
            if (donnesVM.choosedGrid == 1) {
                playingMusicImg.setImageResource(soundItems[donnesVM.selectedImageposition!!].imageResId)
                (gridviewSound.adapter as SoundAdapter).setSelectedItem(donnesVM.selectedImageposition!!)

            } else if (donnesVM.choosedGrid == 2) {
                playingMusicImg.setImageResource(addedSoundItem[donnesVM.selectedImageposition!!].imageResId)
                (addedGridView.adapter as AddedSoundAdapter).setSelectedItem(donnesVM.selectedImageposition!!)

            }
        }
        //Press and hold to delete the selected sound
        addedGridView.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { parent, view, position, id ->
                val path: String = addedSoundItem[position].path

                val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
                alertDialogBuilder.setTitle(getString(R.string.confirm_action))
                alertDialogBuilder.setMessage(getString(R.string.choose_action))

                alertDialogBuilder.setPositiveButton(
                    getString(R.string.delete),
                    DialogInterface.OnClickListener { dialog, which ->
                        // Delete the sound
                        val deleteDialog = AlertDialog.Builder(this@MainActivity)
                        deleteDialog.setTitle(getString(R.string.confirm_deletion))
                        deleteDialog.setMessage(getString(R.string.deleteDescription))
                        deleteDialog.setPositiveButton(getString(R.string.yes), null)
                        deleteDialog.setNegativeButton(getString(R.string.no), null)

                        val alertDialog = deleteDialog.create()
                        alertDialog.setOnShowListener {
                            val yesButtonColor =
                                if (Preferences[this]?.theme == Preferences.THEME_LIGHT) {
                                    ContextCompat.getColor(
                                        this,
                                        R.color.menuItemTextColorLight
                                    ) //  color for light theme
                                } else {
                                    ContextCompat.getColor(this, R.color.menuItemTextColorDark)
                                    // Blue color for dark theme
                                }
                            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                                ?.setTextColor(yesButtonColor)                    // Similarly, set the text color for the negative button
                            val noButtonColor =
                                if (Preferences[this]?.theme == Preferences.THEME_LIGHT) {
                                    ContextCompat.getColor(
                                        this,
                                        R.color.menuItemTextColorLight
                                    )  //  color for light theme
                                } else {
                                    ContextCompat.getColor(this, R.color.menuItemTextColorDark)
                                }
                            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                                ?.setTextColor(noButtonColor)
                            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                                ?.setOnClickListener {
                                    // Effacer du path original
                                    val file = File(path)
                                    if (file.exists()) {
                                        file.delete()
                                    }

                                    //effacer du gridView
                                    addedSoundItem.removeAt(position)
                                    scanSoundFolder()

                                    alertDialog.dismiss()
                                    donnesVM.selectedImageposition = null
                                    donnesVM.itemSelected = false
                                    playingMusicImg?.setImageResource(0)
//                        playingMusicImg = null
                                    buttonPlay.isEnabled = false
                                    stopPlayback()

                                }
                        }
                        alertDialog.show()
                    })
                alertDialogBuilder.setNegativeButton(
                    getString(R.string.rename),
                    DialogInterface.OnClickListener { dialog, which ->
                        // Rename the sound
                        val renameDialog = AlertDialog.Builder(this@MainActivity)
                        renameDialog.setTitle(getString(R.string.enter_new_name))
                        val input = EditText(this@MainActivity)
                        renameDialog.setView(input)
                        renameDialog.setPositiveButton(
                            getString(R.string.ok),
                            DialogInterface.OnClickListener { dialog, which ->
                                val newName = input.text.toString()
                                val file = File(path)
                                val timeStamp =
                                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                                        Date()
                                    )
                                val newFile =
                                    File(file.parentFile, "SEL_${newName}_${timeStamp}.3gp")
                                file.renameTo(newFile)
                                addedSoundItem[position] = AddedSoundItem(
                                    addedSoundItem[position].imageResId,
                                    newFile.absolutePath,
                                    newName,
                                    addedSoundItem[position].creationDate
                                )
                                addedAdapter.notifyDataSetChanged()
                                Toast.makeText(
                                    this,
                                    getString(R.string.sound_renamed),
                                    Toast.LENGTH_SHORT
                                ).show()
                                (addedGridView.adapter as AddedSoundAdapter).setSelectedItem(
                                    position
                                )
                                (gridviewSound.adapter as SoundAdapter).setSelectedItem(-1)
                                donnesVM.selectedImageposition = position
                                donnesVM.choosedGrid = 2
                                addedGridView.setSelection(position)
                                Log.d("Elie", "position: $position")
                                playingMusicImg.setImageResource(R.mipmap.music_notes_foreground)
                            })
                        renameDialog.setNegativeButton(getString(R.string.cancel), null)
                        renameDialog.show()
                    })
                alertDialogBuilder.setNeutralButton(getString(R.string.cancel), null)

                val alertDialogg = alertDialogBuilder.create()
                alertDialogg.show()
                true // Return true to consume the long click event
            }


        val sleepTimeoutSpinner = findViewById<Spinner>(R.id.sleepTimerSpinner)
        val times: List<String> = _timeMap?.keys?.toList() ?: emptyList()
        sleepTimeoutSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (donnesVM.isPlaying && donnesVM.itemSelected && isUserSelection) {
                    updatePlayTimeout()
                    Toast.makeText(this@MainActivity, R.string.sleepTimerUpdated, Toast.LENGTH_LONG)
                        .show()
                    isUserSelection = false
                } else if (!donnesVM.itemSelected) {
                    donnesVM.itemSelected = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // noop
            }

        }
        // Set onTouchListener to track user interaction
        sleepTimeoutSpinner.setOnTouchListener { _, _ ->
            isUserSelection = true  // Set the flag when the user interacts with the spinner
            false  // Return false to indicate that touch event is not consumed
        }
        val timesAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, times
        )
        timesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sleepTimeoutSpinner.adapter = timesAdapter
        volumeControlStream = AudioManager.STREAM_MUSIC


        buttonPlay.setOnClickListener {
//            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
//        if()
            if (donnesVM.selectedImageposition != null) {
                if (!donnesVM.isPlaying) {
                    if (donnesVM.choosedGrid == 1) {
                        startPlayback()

                    }
                    if (donnesVM.choosedGrid == 2) {
                        startAddedPlayback(donnesVM.selectedImageposition as Int)
                    }
                } else {
                    stopPlayback()
                }
            }

        }
        _ffmpeg = FFmpeg.getInstance(this)
        File(filesDir, "ffmpeg").setExecutable(true)
        File(getExternalFilesDir(null), "files/ffmpeg").setExecutable(true)

        if (_ffmpeg is FFmpeg && _ffmpeg!!.isSupported()) {
            //button.isEnabled = true
        } else {
            Log.d(TAG, "ffmpeg not supported")
            reportPlaybackUnsupported()
        }


        // Add the code to set execute permissions for the directory and the FFmpeg binary file
        val ffmpegDirectory = File("/data/user/0/protect.babysleepsounds/files/")
        ffmpegDirectory.setExecutable(true)

        val ffmpegFile = File("/data/user/0/protect.babysleepsounds/files/ffmpeg")
        ffmpegFile.setExecutable(true)

        if (_ffmpeg is FFmpeg && _ffmpeg!!.isSupported()) {
            //button.isEnabled = true
        } else {
            Log.d(TAG, "ffmpeg not supported")
            reportPlaybackUnsupported()
        }


    }


    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSION_CODE
            )
        } else {
            // Permissions already granted, continue with app initialization
            initializeApp()
        }
    }

    // Fonction pour gérer la réponse de l'utilisateur à la demande d'autorisations
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                // If all permissions are granted, initialize the app
                initializeApp()
            } else {
                // If any permission is denied, inform the user and finish the activity
                Toast.makeText(
                    this, "Permission denied. App cannot function properly.", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    private fun initializeApp() {
        // These sound files by convention are:
        // - take a ~10 second clip
        // - Apply a 2 second fade-in and fade-out
        // - Cut the first 3 seconds, and place it over the last three seconds
        //   which should create a seamless track appropriate for looping
        // - Save as a mp3 file, 128kbps, stereo
        _soundMap =
            ImmutableMap.builder<Int, Int>().put(R.mipmap.campfire_foreground, R.raw.campfire)
                .put(R.mipmap.dryer_foreground, R.raw.dryer)
                .put(R.mipmap.fan_foreground, R.raw.fan)
                .put(R.mipmap.ocean_foreground, R.raw.ocean)
                .put(R.mipmap.rain_foreground, R.raw.rain)
                .put(R.mipmap.refrigerator_foreground, R.raw.refrigerator)
                .put(R.mipmap.shhh_foreground, R.raw.shhhh)
                .put(R.mipmap.shower_foreground, R.raw.shower)
                .put(R.mipmap.stream_foreground, R.raw.stream)
                .put(R.mipmap.vacuum_foreground, R.raw.vacuum)
                .put(R.mipmap.water_foreground, R.raw.water)
                .put(R.mipmap.waterfall_foreground, R.raw.waterfall)
                .put(R.mipmap.waves_foreground, R.raw.waves)
                .put(R.mipmap.white_noise_foreground, R.raw.white_noise).build()
        _timeMap =
            ImmutableMap.builder<String, Int>().put(resources.getString(R.string.disabled), 0)
                .put(resources.getString(R.string.time_1minute), 1000 * 60 * 1)
                .put(resources.getString(R.string.time_5minute), 1000 * 60 * 5)
                .put(resources.getString(R.string.time_10minute), 1000 * 60 * 10)
                .put(resources.getString(R.string.time_30minute), 1000 * 60 * 30)
                .put(resources.getString(R.string.time_1hour), 1000 * 60 * 60 * 1)
                .put(resources.getString(R.string.time_2hour), 1000 * 60 * 60 * 2)
                .put(resources.getString(R.string.time_4hour), 1000 * 60 * 60 * 4)
                .put(resources.getString(R.string.time_8hour), 1000 * 60 * 60 * 8).build()

    }

    /**
     * Report to the user that playback is not supported on this device
     */
    private fun reportPlaybackUnsupported() {
        Toast.makeText(this, R.string.playbackNotSupported, Toast.LENGTH_LONG).show()
    }

    private fun startPlayback() {
        val selectedPosition = donnesVM?.selectedImageposition
        val selectedSoundItem = soundItems[selectedPosition!!]


        val selectedSound = selectedSoundItem.imageResId
        val id = _soundMap!![selectedSound]!!
        // Check if it's a valid selection
        if (selectedPosition != null && selectedPosition != -1 && selectedPosition < soundItems.size) {

            val originalFile = File(getExternalFilesDir(null), "$selectedSound.mp3")
            writeToFile(id, originalFile)
            val processed = File(filesDir, "$selectedSound.raw")
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                // Use existing code for Android 10 and below

                if (processed.exists()) {
                    val result = processed.delete()
                    if (!result) {
                        throw IOException("Unable to delete previous file, cannot prepare new file")
                    }
                }
                val arguments = LinkedList<String>()
                arguments.add("-i")
                arguments.add(originalFile.absolutePath)
                if (Preferences[this]!!.isLowPassFilterEnabled) {
                    val frequencyValue = Preferences[this]!!.lowPassFilterFrequency
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
                val cmd = arguments.toTypedArray()
                _ffmpeg!!.execute(cmd, object : ExecuteBinaryResponseHandler() {
                    override fun onStart() {
                        Log.d(TAG, "ffmpeg execute onStart()")
                    }

                    override fun onSuccess(message: String) {
                        Log.d(TAG, "ffmpeg execute onSuccess(): $message")
                        val startIntent = Intent(this@MainActivity, AudioService::class.java)
                        startIntent.putExtra(
                            AudioService.AUDIO_FILENAME_ARG, processed.absolutePath
                        )
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

            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                val startIntent = Intent(this@MainActivity, AudioService::class.java)
                startIntent.putExtra(
                    AudioService.AUDIO_FILENAME_ARG,
                    "/storage/emulated/0/Android/data/protect.babysleepsounds/files/" + selectedSound + ".mp3"
                )
                startService(startIntent)
                updateToPlaying()

            }

//
        } else {
            // Handle the case when no item is selected
            Toast.makeText(this, "Please select a sound first", Toast.LENGTH_SHORT).show()
        }

    }

    private fun startAddedPlayback(position: Int) {
        val startIntent = Intent(this@MainActivity, AudioService::class.java)
        startIntent.putExtra(
            AudioService.AUDIO_FILENAME_ARG,
            addedSoundItem[position].path
        )
        startService(startIntent)
        updateToPlaying()
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
        // Start the countdown timer

        val sleepTimeoutSpinner = findViewById<Spinner>(R.id.sleepTimerSpinner)
        val selectedTimeout = sleepTimeoutSpinner.selectedItem as String
        val timeoutMs = _timeMap!![selectedTimeout]!!
        if (timeoutMs > 0 && !donnesVM.frequenceChanged) {
            val serviceTimerChanged = Intent(this@MainActivity, TimerService::class.java)
            serviceTimerChanged.putExtra("timerchanged", true)
            startService(serviceTimerChanged)

            val serviceIntent = Intent(this@MainActivity, TimerService::class.java)
            serviceIntent.putExtra("countdownDuration", timeoutMs)
            startService(serviceIntent)
        } else if (donnesVM.frequenceChanged) {
            donnesVM.frequenceChanged = false
        } else {
            val serviceTimerChanged = Intent(this@MainActivity, TimerService::class.java)
            serviceTimerChanged.putExtra("timerchanged", true)
            serviceTimerChanged.putExtra("timerDisabled", true)
            startService(serviceTimerChanged)
        }
    }

    /**
     * Update the UI to reflect it is playing
     */
    private fun updateToPlaying() {
        donnesVM.isPlaying = true
        runOnUiThread {
            if (!donnesVM.frequenceChanged) {
                updatePlayTimeout()
            } else {
                donnesVM.frequenceChanged = false
            }
            val button = findViewById<Button>(R.id.button)
            button.setText(R.string.stop)
            donnesVM.isGridViewClickable = false

            if (_encodingProgress != null) {
                _encodingProgress!!.hide()
                _encodingProgress = null
            }
        }
    }

    private fun stopPlayback() {
        if (!donnesVM.frequenceChanged) {
            val serviceTimerStop = Intent(this@MainActivity, TimerService::class.java)
            startService(serviceTimerStop)
        }
        val stopIntent = Intent(this@MainActivity, AudioService::class.java)
        startService(stopIntent)
        donnesVM.isPlaying = false

        runOnUiThread {
            val button = findViewById<Button>(R.id.button)
            button.setText(R.string.play)
            setControlsEnabled(true)
            donnesVM.isGridViewClickable = true
        }
        // Stop the MediaPlayer
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

    }


    private fun setControlsEnabled(enabled: Boolean) {
        for (resId in intArrayOf(R.id.gridView)) {
            val view = findViewById<View>(resId)
            view.isEnabled = enabled
        }
    }

    override fun onDestroy() {
        donnesVM.itemSelected = false
        if (!isChangingConfigurations) {
            if (donnesVM.isPlaying) {
                stopPlayback()
            }
            for (toDelete in arrayOf(ORIGINAL_MP3_FILE, PROCESSED_RAW_FILE)) {
                val file = File(filesDir, toDelete)
                if (file.exists()) { // Check if the file exists before deleting
                    val result = file.delete()
                    if (!result) {
                        Log.w(TAG, "Failed to delete file on exit: " + file.absolutePath)
                    }
                }
            }

        }
        unregisterReceiver(stopMusicReceiver)
        unregisterReceiver(stopStartMusicReceiver)
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val theme = Preferences[this]?.theme

        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val itemTitle = menuItem.title.toString()
            val textColor = if (theme == Preferences.THEME_LIGHT) {
                R.color.menuItemTextColorLight // Define your light theme text color here
            } else {
                R.color.menuItemTextColorDark // Define your dark theme text color here
            }
            menuItem.title = SpannableString(itemTitle).apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this@MainActivity, textColor)),
                    0,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        }
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
        } else if (id == R.id.action_add_own) {
            if (donnesVM.isPlaying) {
                stopPlayback()
            }
            startActivity(Intent(this, RecordingUploadingActivity::class.java))
            return true
        } else if (id == R.id.action_supprimer) {
            showHowToDeleteSoundDialog()
            true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showHowToDeleteSoundDialog() {
        val dialogTitle = getString(R.string.howToDelete)
        val dialogContent = getString(R.string.how_to_delete_sound_content)

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setMessage(dialogContent)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun scanSoundFolder() {
        val soundDirectory =
            File("/storage/emulated/0/Android/data/protect.babysleepsounds/files/Music/SelectedSounds")
        val soundFiles = soundDirectory.listFiles { dir, name -> name.endsWith(".3gp") }
        val addedGridView = findViewById<GridView>(R.id.gridView_ajoute)

        // Initialize addedSoundItem as an empty list
        addedSoundItem = mutableListOf()

        if (soundFiles != null) {
            for (soundFile in soundFiles) {
                // Extract the name of the sound from its path
                val soundName = soundFile.nameWithoutExtension

                // Get the creation date of the file
                // Get the creation date of the file
                val creationDate = SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(soundFile.lastModified())
                // Use the actual path of the sound file for AddedSoundItem
                val addedSound = AddedSoundItem(
                    R.mipmap.music_notes_foreground,
                    soundFile.absolutePath,
                    soundName,
                    creationDate
                )
                addedSoundItem.add(addedSound)
            }
        }

        val addedAdapter = AddedSoundAdapter(this, addedSoundItem)
        addedGridView.adapter = addedAdapter

        // Notify the adapter that the data set has changed
        addedAdapter.notifyDataSetChanged()
    }


    private fun initialiserGif(isSwitch1On: Boolean, playingMusicImg: ImageView) {
        if (donnesVM.choosedGrid == 1) {
            var choosenGif: Int? = null
            if (isSwitch1On == true) {
                when (donnesVM.selectedImageposition) {
                    0 -> choosenGif = R.drawable.campfire
                    1 -> choosenGif = R.drawable.dryer
                    2 -> choosenGif = R.drawable.fan
                    3 -> choosenGif = R.drawable.ocean
                    4 -> choosenGif = R.drawable.rain
                    5 -> choosenGif = R.drawable.refrigerator
                    6 -> choosenGif = R.drawable.shhhh
                    7 -> choosenGif = R.drawable.shower
                    8 -> choosenGif = R.drawable.stream
                    9 -> choosenGif = R.drawable.vacuum
                    10 -> choosenGif = R.drawable.water
                    11 -> choosenGif = R.drawable.waterfall
                    12 -> choosenGif = R.drawable.waves
                    13 -> choosenGif = R.drawable.white_noise
                }

                Glide.with(this)
                    .load(choosenGif)
                    .into(playingMusicImg)
            } else {
                donnesVM.isSwitch1On = false
                playingMusicImg.setImageResource(soundItems[donnesVM.selectedImageposition!!].imageResId)
            }
        } else {
            if (isSwitch1On == true) {
                Glide.with(this)
                    .load(R.drawable.music_notes)
                    .into(playingMusicImg)
            } else {
                playingMusicImg.setImageResource(R.mipmap.music_notes_foreground)
            }
        }
    }

    private fun displayAboutDialog() {
        val USED_LIBRARIES: Map<String, String> = ImmutableMap.of(
            "FFmpeg",
            "https://ffmpeg.org/",
            "FFmpeg-Android",
            "https://github.com/writingminds/ffmpeg-android"
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
        val html =
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" + "<img src=\"file:///android_res/mipmap/music_notes\" alt=\"" + appName + "\"/>" + "<h1>" + String.format(
                getString(R.string.about_title_fmt),
                "<a href=\"" + getString(R.string.app_webpage_url)
            ) + "\">" + appName + "</a>" + "</h1><p>" + appName + " " + String.format(
                getString(R.string.debug_version_fmt),
                version
            ) + "</p><p>" + String.format(
                getString(R.string.app_revision_fmt),
                "<a href=\"" + getString(R.string.app_revision_url) + "\">" + getString(R.string.app_revision_url) + "</a>"
            ) + "</p><hr/><p>" + String.format(
                getString(R.string.app_copyright_fmt),
                year
            ) + "</p><hr/><p>" + String.format(
        getString(R.string.about_description),
        year
        ) +  "</p><hr/><p>" + getString(R.string.app_license) + "</p><hr/><p>" + String.format(
                getString(R.string.sound_resources), appName, soundResources.toString()
            ) + "</p><hr/><p>" + String.format(
                getString(R.string.image_resources), appName, imageResources.toString()
            ) + "</p><hr/><p>" + String.format(
                getString(R.string.app_libraries), appName, libs.toString()
            )
        wv.loadDataWithBaseURL(
            "file:///android_res/drawable/", html, "text/html", "utf-8", null
        )
        AlertDialog.Builder(this).setView(wv).setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
    }


    companion object {
        private const val TAG = "BabySleepSounds"
        private const val ORIGINAL_MP3_FILE = "original.mp3"
        private const val PROCESSED_RAW_FILE = "processed.raw"

    }
}