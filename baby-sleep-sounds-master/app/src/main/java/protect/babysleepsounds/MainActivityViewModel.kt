package protect.babysleepsounds

import android.net.Uri
import androidx.lifecycle.ViewModel

/**
 * This is the MainActivityViewModel class that extends ViewModel.
 * It is responsible for managing the data for MainActivity and other activities.
 */
class MainActivityViewModel : ViewModel() {
    /**
     * This is the selectedImageposition property that holds the position of the selected image.
     */
    var selectedImageposition: Int? = null
    /**
     * This is the isPlaying property that indicates whether the audio is playing.
     */
    var isPlaying: Boolean = false
    /**
     * This is the itemSelected property that indicates whether an item is selected.
     */
    var itemSelected: Boolean = true
    /**
     * This is the frequenceChanged property that indicates whether the frequency has changed.
     */
    var frequenceChanged = false
    /**
     * This is the recordingFilePath property that holds the file path of a recorded sound.
     */
    var recordingFilePath : String?= null
    /**
     * This is the selectedAudioUri property that holds the URI of the selected audio.
     */
    var selectedAudioUri : Uri? = null
    /**
     * This is the isplayingRecording property that indicates whether the recording is playing.
     */
    var isplayingRecording : Boolean = false
    /**
     * This is the recordingElapsedTime property that holds the elapsed time of the recording.
     */
    var recordingElapsedTime : Long = 0
    /**
     * This is the isGridViewClickable property that indicates whether the grid view is clickable.
     */
    var isGridViewClickable: Boolean = true
    /**
     * This is the choosedGrid property that holds the chosen grid.
     */
    var choosedGrid: Int = 0
    /**
     * This is the isSwitch1On property that indicates whether the switch is on.
     */
    var isSwitch1On: Boolean = true
}