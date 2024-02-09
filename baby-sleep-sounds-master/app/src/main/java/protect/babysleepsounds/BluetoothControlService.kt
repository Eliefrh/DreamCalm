package protect.babysleepsounds
import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
class BluetoothControlService : Service() {

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {
                val keyCode = it.getIntExtra("keyEvent", KeyEvent.KEYCODE_UNKNOWN)
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    stopStartPlayback()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Register BroadcastReceiver
        val intentFilter = IntentFilter("ON_KEY_DOWN")

        registerReceiver(bluetoothReceiver, intentFilter)

        // Display a persistent notification to indicate the service is running
        //startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    override fun onDestroy() {
        // Unregister BroadcastReceiver
        unregisterReceiver(bluetoothReceiver)
        super.onDestroy()
    }

    private fun stopStartPlayback() {
        val stopStartIntent = Intent("STOP_START_MUSIC")
        sendBroadcast(stopStartIntent)
        // Implement your playback start logic here
    }


    private fun createNotification() {
        // Create and return a notification to display as a foreground service
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val NOTIFICATION_ID = 123 // Unique ID for the notification
    }
}