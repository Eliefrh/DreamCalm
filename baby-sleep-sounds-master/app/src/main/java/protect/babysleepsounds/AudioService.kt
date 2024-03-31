package protect.babysleepsounds

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File

class AudioService : Service() {
    private var _mediaPlayer: LoopingAudioPlayer? = null

    override fun onBind(intent: Intent): IBinder? {
        // Used only in case of bound services.
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if(intent.hasExtra(AUDIO_FILENAME_ARG)){
            audioFilename = intent.getStringExtra(AUDIO_FILENAME_ARG)
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
            stopSelf()
        }

        // If this service is killed, let is remain dead until explicitly started again.
        return START_NOT_STICKY
    }




    override fun onDestroy() {
        if (_mediaPlayer != null) {
            _mediaPlayer!!.stop()
        }
        super.onDestroy()
    }

    companion object {
        private var audioFilename: String? = null
        private const val TAG = "BabySleepSounds"
        const val AUDIO_FILENAME_ARG = "AUDIO_FILENAME_ARG"
        const val ACTION_PLAY = "protect.babysleepsounds.action.PLAY"
        const val ACTION_PAUSE = "protect.babysleepsounds.action.PAUSE" }
}