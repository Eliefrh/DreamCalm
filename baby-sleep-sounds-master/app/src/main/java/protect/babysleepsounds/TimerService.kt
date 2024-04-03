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

class TimerService : Service() {
    private lateinit var notificationManager: NotificationManager
    private var countDownTimer: CountDownTimer? = null
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    }
    private fun createNotificationChannel() {
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

        if (intent != null && intent.hasExtra("countdownDuration")) {
                startTimer(intent.getIntExtra("countdownDuration", 0))
            }else{
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

    private fun startTimer(countdownDuration: Int) {
        countDownTimer = object : CountDownTimer(countdownDuration.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeLeftFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                updateNotification(timeLeftFormatted)
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
    private fun sendTimerUpdateBroadcast(timeLeftFormatted: String) {
        val intent = Intent("TIMER_UPDATE")
        intent.putExtra("timeLeftFormatted", timeLeftFormatted)
        sendBroadcast(intent)
    }
    private fun sendTimerFinishBroadcast() {
        val intent = Intent("TIMER_FINISH")
        sendBroadcast(intent)
    }


    private fun stopTimer(timerChanged : Boolean = false) {
        if(timerChanged){
            countDownTimer?.cancel()
        }else{
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

    private fun updateNotification(timeLeftFormatted : String) {
        notificationBuilder.setContentText(timeLeftFormatted)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notificationBuilder.build())
    }

}