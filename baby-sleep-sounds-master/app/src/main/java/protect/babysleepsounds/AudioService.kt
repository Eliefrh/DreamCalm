package protect.babysleepsounds

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import protect.babysleepsounds.MainActivity
import java.io.File

class AudioService : Service() {
    private var _mediaPlayer: LoopingAudioPlayer? = null
    override fun onBind(intent: Intent): IBinder? {
        // Used only in case of bound services.
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val audioFilename = intent.getStringExtra(AUDIO_FILENAME_ARG)
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

        // If this service is killed, let is remain dead until explicitly started again.
        return START_NOT_STICKY
    }

    private fun setNotification() {
        var channelId = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel()
        }
        val builder = NotificationCompat.Builder(this, channelId)
            .setOngoing(true)
            .setSmallIcon(R.drawable.playing_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notificationPlaying))

        // Creates an explicit intent for the Activity
        val resultIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            this, 0,
            resultIntent, 0
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
    }

    companion object {
        private const val TAG = "BabySleepSounds"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = TAG
        const val AUDIO_FILENAME_ARG = "AUDIO_FILENAME_ARG"
    }
}