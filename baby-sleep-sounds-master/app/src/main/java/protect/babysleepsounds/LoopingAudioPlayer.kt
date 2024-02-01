package protect.babysleepsounds

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class LoopingAudioPlayer(context: Context, private val _wavFile: File) {
    private var _wakeLock: WakeLock? = null
    fun start() {
        if (_playbackThread != null) {
            _wakeLock?.acquire()
            _playbackThread!!.start()
        } else {
            Log.w(TAG, "Audio playback already stopped, cannot start again")
        }
    }

    fun stop() {
        Log.i(TAG, "Requesting audio playback to stop")
        if (_playbackThread != null) {
            _playbackThread!!.interrupt()
            _playbackThread = null
            _wakeLock?.release()
        }
    }

    private var _playbackThread: Thread? = Thread({
        Log.i(TAG, "Setting up audio playback")
        val bufferSize =
            AudioTrack.getMinBufferSize(FREQUENCY, CHANNEL_CONFIGURATION, AUDIO_ENCODING)
        val byteBufferSize = bufferSize * 2
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            FREQUENCY,
            CHANNEL_CONFIGURATION,
            AUDIO_ENCODING,
            bufferSize,
            AudioTrack.MODE_STREAM
        )
        var `is`: FileInputStream? = null
        try {
            val buffer = ByteArray(byteBufferSize)
            audioTrack.play()
            while (Thread.currentThread().isInterrupted == false) {
                `is` = FileInputStream(_wavFile)
                var read: Int
                while (Thread.currentThread().isInterrupted == false) {
                    read = `is`.read(buffer)
                    if (read <= 0) {
                        break
                    }
                    audioTrack.write(buffer, 0, read)
                }

                // File completed playback, start again
                try {
                    `is`.close()
                } catch (e: IOException) {
                    // Nothing to do, we are finished with the file anyway
                }
            }
        } catch (e: IOException) {
            Log.d(TAG, "Failed to read file", e)
        } finally {
            try {
                `is`?.close()
            } catch (e: IOException) {
                Log.d(TAG, "Failed to close file", e)
            }
            audioTrack.release()
        }
        Log.i(TAG, "Finished playback")
    }, "PlaybackThread")

    init {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _wakeLock = if (powerManager != null) {
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "protect.babysleepsounds:LoopingAudioPlayerWakeLock"
            )
        } else {
            Log.w(TAG, "Failed to acquire a wakelock")
            null
        }
    }

    companion object {
        const val TAG = "BabySleepSounds"
        private const val FREQUENCY = 44100
        private const val CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_OUT_STEREO
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}