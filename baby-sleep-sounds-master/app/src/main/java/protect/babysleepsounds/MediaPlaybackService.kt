package protect.babysleepsounds

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log

/**
 * A service that handles media playback.
 *
 * This service is responsible for handling media playback actions such as play, pause, and stop.
 * It uses the MediaSessionCompat for handling media session actions.
 */
class MediaPlaybackService : Service() {
    /**
     * The MediaSessionCompat used for handling media session actions.
     */
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        // Initialize MediaSessionCompat
        mediaSession = MediaSessionCompat(this, "Tag").apply {
            // Set MediaSessionCompat to active
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                // Handle play action
                override fun onPlay() {
                    super.onPlay()
                    // Handle play button
                    Log.i("mediaButton", "Received intent to start playback")
                    // Send broadcast to start music
                    val stopStartIntent = Intent("STOP_START_MUSIC")
                    sendBroadcast(stopStartIntent)
                }
                // Handle pause action
                override fun onPause() {
                    super.onPause()
                    // Handle pause button
                    Log.i("mediaButton", "Received intent to pause playback")
                    // Send broadcast to pause music
                    val stopStartIntent = Intent("STOP_START_MUSIC")
                    sendBroadcast(stopStartIntent)
                }
                // Handle stop action
                override fun onStop() {
                    super.onStop()
                    // Handle stop button
                    Log.i("mediaButton", "Received intent to stop playback")
                    // Send broadcast to stop music
                    val stopStartIntent = Intent("STOP_START_MUSIC")
                    sendBroadcast(stopStartIntent)
                }
            })
            // Set playback state for MediaSessionCompat
            val state = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP)
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0f)
                .build()

            setPlaybackState(state)
        }
    }
    /**
     * Called when the service is started.
     *
     * Returns START_STICKY to make the service run continuously until it is explicitly stopped.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Your code here to handle start command
        return START_STICKY
    }
    /**
     * Called when the service is destroyed.
     *
     * Releases the MediaSessionCompat.
     */
    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }

    /**
     * Returns null as the service does not allow binding.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}