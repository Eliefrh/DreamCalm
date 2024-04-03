package protect.babysleepsounds

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.browse.MediaBrowser
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

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
                setNotification()
            } else {
                Log.i(TAG, "Received intent to stop playback")
                if (_mediaPlayer != null) {
                    _mediaPlayer!!.stop()
                    _mediaPlayer = null
                }
                stopForeground(true)
                stopSelf()
            }
            when (intent.action) {
                ACTION_PLAY -> {
                    setNotification()
                }
            }
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


    private fun setNotification() {
        var channelId = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel()
        }

        // Create a pending intent for the Play action
        val playIntent = Intent(this, AudioService::class.java).apply {
            action = ACTION_PLAY
        }
        val playPendingIntent = PendingIntent.getService(
            this, 0, playIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Create a pending intent for the Pause action
        val pauseIntent = Intent(this, AudioService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 0, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Create a pending intent for the Stop action
        val stopIntent = Intent(this, AudioService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.playing_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notificationPlaying))
            .addAction(R.drawable.ic_play, "play", playPendingIntent) // #0
            .addAction(R.drawable.ic_play, "stop", pausePendingIntent) // #1


        // Creates an explicit intent for the Activity
        val resultIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            this, 0,
            resultIntent, PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(resultPendingIntent)
        startForeground(NOTIFICATION_ID, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = NOTIFICATION_CHANNEL_ID
        val channelName = getString(R.string.notificationChannelName)
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_LOW
        )
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service?.createNotificationChannel(chan)
            ?: Log.w(TAG, "Could not get NotificationManager")
        return channelId
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
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = TAG
        const val AUDIO_FILENAME_ARG = "AUDIO_FILENAME_ARG"
        const val ACTION_PLAY = "protect.babysleepsounds.action.PLAY"
        const val ACTION_PAUSE = "protect.babysleepsounds.action.PAUSE"
        const val ACTION_STOP = "protect.babysleepsounds.action.STOP"
    }
}