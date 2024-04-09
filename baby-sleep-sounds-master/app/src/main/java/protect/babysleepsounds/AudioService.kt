package protect.babysleepsounds

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
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

    override fun onBind(intent: Intent): IBinder? {
        // Used only in case of bound services.
        return null
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val audioFilename = intent.getStringExtra(AUDIO_FILENAME_ARG)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {


            if (audioFilename != null) {
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
                stopForeground(true)
                stopSelf()
            }
            _mediaPlayer = LoopingAudioPlayer(this, File(audioFilename))
            _mediaPlayer!!.start()
        } else {
//            mediaPlayer?.stop()
//            mediaPlayer?.reset()
//
//            mediaPlayer = MediaPlayer()
//            if (audioFilename != null) {
//                mediaPlayer?.setDataSource(this, audioFilename.toUri())
//            }
//            mediaPlayer?.prepare()
//            mediaPlayer?.start()
//            mediaPlayer?.start()
//            val audioUri = audioFilename?.toUri()
//            val mediaItem = MediaItem.fromUri(audioUri!!)
            val dataSourceFactory = DefaultDataSourceFactory(this, "exoplayer-sample")
            val audioSource = audioFilename?.toUri()?.let { MediaItem.fromUri(it) }?.let {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(it)
            }
            if (audioSource != null) {
                exoPlayer.prepare(audioSource)
            } else {
                exoPlayer!!.stop()
//                _mediaPlayer = null

                stopForeground(true)
                stopSelf()
            }
            exoPlayer.playWhenReady = true

        }

        // If this service is killed, let is remain dead until explicitly started again.
        return START_NOT_STICKY
    }




    override fun onDestroy() {
        if (_mediaPlayer != null) {
            _mediaPlayer!!.stop()
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            exoPlayer.release()
        }
        super.onDestroy()
    }


    companion object {

        private const val TAG = "BabySleepSounds"
        const val AUDIO_FILENAME_ARG = "AUDIO_FILENAME_ARG"
        const val ACTION_PLAY = "protect.babysleepsounds.action.PLAY"
        const val ACTION_PAUSE = "protect.babysleepsounds.action.PAUSE" }
}