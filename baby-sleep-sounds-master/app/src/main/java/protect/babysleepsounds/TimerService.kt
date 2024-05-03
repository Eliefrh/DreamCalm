package protect.babysleepsounds

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * A service that manages a countdown timer. The timer is controlled by intents sent to the service.
 * The remaining time is displayed in a persistent notification and broadcasted for other components.
 *
 * @property notificationManager The system service that manages notifications.
 * @property countDownTimer The countdown timer instance.
 * @property notificationBuilder The builder for creating notifications.
 */
class TimerService : Service() {
    private lateinit var notificationManager: NotificationManager
    // Countdown timer instance
    private var countDownTimer: CountDownTimer? = null
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    }
    private fun createNotificationChannel() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "timer_channel"
            val channelName = "Timer Channel"
            val channelDescription = "Channel for Timer Service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check if intent has countdown duration
        if (intent != null && intent.hasExtra("countdownDuration")) {
            // Start timer with countdown duration
            startTimer(intent.getIntExtra("countdownDuration", 0))
            }else{
            // if intent has a timer but no countdown duration, stop the timer, and if hte timer is disabled, on the notification show that the timer is disabled
            if (intent != null) {
                stopTimer(intent.getBooleanExtra("timerchanged", false))
                if(intent.hasExtra("timerDisabled")){
                    startForeground(1, createNotification("timer disabled"))
                }
            }
            }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    /**
     * Starts the countdown timer and updates the notification and broadcasts the remaining time at each tick.
     *
     * @param countdownDuration The duration of the countdown in milliseconds.
     */
    private fun startTimer(countdownDuration: Int) {
        // Initialize countdown timer
        countDownTimer = object : CountDownTimer(countdownDuration.toLong(), 1000) {
            // Called at every tick of the timer
            override fun onTick(millisUntilFinished: Long) {
                // Calculate hours, minutes, and seconds
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                // Format time left
                val timeLeftFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                // Update notification with time left
                updateNotification(timeLeftFormatted)
                // Send broadcast with time left
                sendTimerUpdateBroadcast(timeLeftFormatted)
            }

            override fun onFinish() {
                countDownTimer?.cancel()
                sendTimerFinishBroadcast()
            }
        }
        countDownTimer?.start()

        // Start service in foreground to avoid being killed by the system
        startForeground(1, createNotification())
    }
    /**
     * Sends a broadcast with the updated remaining time.
     *
     * @param timeLeftFormatted The remaining time formatted as a string.
     */
    private fun sendTimerUpdateBroadcast(timeLeftFormatted: String) {
        val intent = Intent("TIMER_UPDATE")
        intent.putExtra("timeLeftFormatted", timeLeftFormatted)
        sendBroadcast(intent)
    }
    /**
     * Sends a broadcast when the timer finishes.
     */
    private fun sendTimerFinishBroadcast() {
        val intent = Intent("TIMER_FINISH")
        sendBroadcast(intent)
    }

    /**
     * Stops the timer and cancels the foreground service.
     *
     * @param timerChanged A flag indicating whether the timer was changed.
     */
    private fun stopTimer(timerChanged : Boolean = false) {
        // Check if timer was changed, it gets canceled
        if(timerChanged){
            countDownTimer?.cancel()
        }else{
            // Finish the timer and send a broadcast that the timer has finished
            countDownTimer?.onFinish()
        if(countDownTimer != null){
            stopForeground(true)
        stopSelf()
        }
        }
    }


    private fun createNotification(content:String = "00:00:00"): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        // Create a pending intent for the Play action
        val playIntent = Intent(this, AudioService::class.java).apply {
            action = AudioService.ACTION_PLAY
        }
        val playPendingIntent = PendingIntent.getService(this, 0, playIntent,
            PendingIntent.FLAG_IMMUTABLE)

        // Create a pending intent for the Pause action
        val pauseIntent = Intent(this, AudioService::class.java).apply {
            action = AudioService.ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder = NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(R.drawable.playing_notification)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)

        return notificationBuilder.build()
    }
    /**
     * Updates the notification with the given time.
     *
     * @param timeLeftFormatted The remaining time formatted as a string.
     */
    private fun updateNotification(timeLeftFormatted : String) {
        notificationBuilder.setContentText(timeLeftFormatted)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notificationBuilder.build())
    }

}