package protect.babysleepsounds

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log

class MediaPlaybackService : Service() {
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "Tag").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    // Handle play button
                Log.i("mediaButton", "Received intent to start playback")
                    val stopStartIntent = Intent("STOP_START_MUSIC")
                    sendBroadcast(stopStartIntent)
                }

                override fun onPause() {
                    super.onPause()
                    // Handle pause button
                    Log.i("mediaButton", "Received intent to pause playback")
                    val stopStartIntent = Intent("STOP_START_MUSIC")
                    sendBroadcast(stopStartIntent)
                }

                override fun onStop() {
                    super.onStop()
                    // Handle stop button
                    Log.i("mediaButton", "Received intent to stop playback")
                    val stopStartIntent = Intent("STOP_START_MUSIC")
                    sendBroadcast(stopStartIntent)
                }
            })

            val state = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP)
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0f)
                .build()

            setPlaybackState(state)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Your code here to handle start command
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}