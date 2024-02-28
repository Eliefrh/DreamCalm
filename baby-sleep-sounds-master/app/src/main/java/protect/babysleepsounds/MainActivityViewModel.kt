package protect.babysleepsounds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Timer
import java.util.TimerTask

class MainActivityViewModel : ViewModel() {
    /*la liste de films est conserve donc apres une simple changement on ne va pas encore demander la
     * liste a la base de donnee
     */
    var selectedImageposition: Int? = null
    var isPlaying: Boolean = false
    var itemSelected: Boolean = true

    var enteredSettings: Boolean = false
    var timerDisabled = false
    var timer: Timer? = null
    private var _remainingTime = MutableLiveData<Long>()
    val remainingTime: LiveData<Long> get() = _remainingTime

    private var startTimeMillis: Long = 0
    private var timeoutMs: Long = 0
    fun startTimer(timeoutMs: Long) {
        this.timeoutMs = timeoutMs
        startTimeMillis = System.currentTimeMillis()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                _remainingTime.postValue(0)
            }
        }, timeoutMs)
    }
    fun calculateAndUpdateRemainingTime() {
        val elapsedTime = System.currentTimeMillis() - startTimeMillis // Calculate elapsed time
        _remainingTime.value= (timeoutMs - elapsedTime) // Calculate remaining time

    }

    fun stopTimer(notNull : Boolean = false) {
        timer?.cancel()
        timer?.purge()
        if(!notNull){
        timer = null
    }
    }
}