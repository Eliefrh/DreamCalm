package protect.babysleepsounds

import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    var selectedImageposition: Int? = null
    var isPlaying: Boolean = false
    var itemSelected: Boolean = true
    var frequenceChanged = false
    var recordingFilePath : String?= null
    var recordingElapsedTime : Long = 0
    var isGridViewClickable: Boolean = true
    var choosedGrid: Int = 0
}