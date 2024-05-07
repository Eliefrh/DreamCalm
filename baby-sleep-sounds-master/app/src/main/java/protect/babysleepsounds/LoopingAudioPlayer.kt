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

/**
 * This is the LoopingAudioPlayer class that handles the audio playback.
 * It provides methods to start and stop the audio playback.
 *
 * @param context This is the context in which the LoopingAudioPlayer class is used.
 * @param _wavFile This is the audio file that will be played.
 */
class LoopingAudioPlayer(context: Context, private val _wavFile: File) {
    private var _wakeLock: WakeLock? = null
    /**
     * This is the start function that starts the audio playback.
     * It acquires a wake lock and starts the playback thread.
     */
    fun start() {
        if (_playbackThread != null) {
            if (_wakeLock != null) {
                _wakeLock!!.acquire()
            }
            _playbackThread!!.start()
        } else {
            Log.w(TAG, "Audio playback already stopped, cannot start again")
        }
    }
    /**
     * This is the stop function that stops the audio playback.
     * It interrupts the playback thread and releases the wake lock.
     */
    fun stop() {
        Log.i(TAG, "Requesting audio playback to stop")
        if (_playbackThread != null) {
            _playbackThread!!.interrupt()
            _playbackThread = null
            if (_wakeLock != null) {
                _wakeLock!!.release()
            }
        }
    }
    /**
     * This is the playback thread that handles the audio playback.
     * It reads the audio file and writes it to the audio track.
     */
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

    /**
     * This is the initializer block that initializes the wake lock.
     */
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
        /**
         * This is the tag used for logging.
         */
        const val TAG = "BabySleepSounds"

        /**
         * This is the frequency used for the audio track.
         */
        private const val FREQUENCY = 44100
        /**
         * This is the channel configuration used for the audio track.
         */
        private const val CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_OUT_STEREO
        /**
         * This is the audio encoding used for the audio track.
         */
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}