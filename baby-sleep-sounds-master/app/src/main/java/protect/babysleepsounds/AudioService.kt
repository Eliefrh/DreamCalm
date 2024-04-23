package protect.babysleepsounds

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import java.io.File

class AudioService : Service() {
    private var _mediaPlayer: LoopingAudioPlayer? = null
    public lateinit var exoPlayer: SimpleExoPlayer
    var audioFilename: String? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent): IBinder? {
        // Used only in case of bound services.
        return null
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
    }

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // The service has lost focus, stop playback
                if (_mediaPlayer != null) {
                    _mediaPlayer!!.stop()
                    _mediaPlayer = null
                }
                exoPlayer.stop()

            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                // The service has regained focus, resume playback
                if (_mediaPlayer != null) {
                    _mediaPlayer!!.stop()
                }
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                    !(audioFilename?.startsWith("/storage/emulated/0/Android/data/protect.babysleepsounds/files/Music/SelectedSounds") == true)
                ) {
                    if (audioFilename != null) {
                        _mediaPlayer = LoopingAudioPlayer(this, File(audioFilename))
                        _mediaPlayer!!.start()
                    }
                } else {
                    val dataSourceFactory = DefaultDataSourceFactory(this, "exoplayer-sample")
                    val audioSource = audioFilename?.toUri()?.let { MediaItem.fromUri(it) }?.let {
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(it)
                    }
                    if (audioSource != null) {
                        exoPlayer.prepare(audioSource)
                    }
                    exoPlayer.playWhenReady = true

                }
            }

            else -> {
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        audioFilename = intent.getStringExtra(AUDIO_FILENAME_ARG)
        var res = 0
        // Request audio focus
        if (audioFilename != null) {
            // Request audio focus for playback
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                res = audioManager.requestAudioFocus(focusRequest!!)
            } else {
                // For API level lower than 26, use the older method
                res = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }

        // Your app has been granted audio focus
        // Start your media playback here
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
            !(audioFilename?.startsWith("/storage/emulated/0/Android/data/protect.babysleepsounds/files/Music/SelectedSounds") == true)
        ) {

            if (audioFilename != null && res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.i(TAG, "Received intent to start playback")
                if (_mediaPlayer != null) {
                    _mediaPlayer!!.stop()
                }
                _mediaPlayer = LoopingAudioPlayer(this, File(audioFilename))
                _mediaPlayer!!.start()
            } else {
                Log.i(TAG, "Received intent to stop playback")
                if (_mediaPlayer != null) {
                    _mediaPlayer!!.stop()
                    _mediaPlayer = null
                }
                exoPlayer.stop()
                stopForeground(true)
                stopSelf()
            }
        } else {

            val dataSourceFactory = DefaultDataSourceFactory(this, "exoplayer-sample")
            val audioSource = audioFilename?.toUri()?.let { MediaItem.fromUri(it) }?.let {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(it)
            }
            if (audioSource != null && res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                exoPlayer.prepare(audioSource)
            } else {

                exoPlayer.stop()
//                _mediaPlayer = null

                stopForeground(true)
                stopSelf()
            }
            exoPlayer.playWhenReady = true

        }


        // If this service is killed, let is remain dead until explicitly started again.
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {

        if (_mediaPlayer != null) {
            _mediaPlayer!!.stop()
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            exoPlayer.release()
        }
        // Abandon audio focus when playback stops
        // Abandon audio focus when playback stops
        focusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BabySleepSounds"
        const val AUDIO_FILENAME_ARG = "AUDIO_FILENAME_ARG"
        const val ACTION_PLAY = "protect.babysleepsounds.action.PLAY"
        const val ACTION_PAUSE = "protect.babysleepsounds.action.PAUSE"
    }
}