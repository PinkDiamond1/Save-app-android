package net.opendasharchive.openarchive.features.media.preview

import android.app.Application
import android.util.Log
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import net.opendasharchive.openarchive.features.media.MediaWorker

class PreviewMediaListViewModel(
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableLiveData<UIState>()
    val uiState: LiveData<UIState>
        get() = _uiState

    val workState: LiveData<List<WorkInfo>>

    private val workManager = WorkManager.getInstance(application)
    private val TAG_MEDIA_UPLOADING = "TAG_MEDIA_UPLOADING"

    init {
        workState = workManager.getWorkInfosByTagLiveData(TAG_MEDIA_UPLOADING)
    }

    fun applyMedia(): Operation {
        val mediaWorker =
            OneTimeWorkRequestBuilder<MediaWorker>().addTag(TAG_MEDIA_UPLOADING).build()
        return workManager.enqueue(mediaWorker)

    }

    fun observeValuesForWorkState(activity: AppCompatActivity) {
        workState.observe(activity) { workInfo ->
            workInfo.forEach {
                when (it.state) {
                    WorkInfo.State.RUNNING -> {
                        Log.e("WorkManager", "Loading")
                        activity.finish()
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        Log.e("WorkManager", "Succeed")
                    }
                    WorkInfo.State.FAILED -> {
                        Log.e("WorkManager", "Failed")
                    }
                    else -> {
                        Log.d("WorkManager", "workInfo is null")
                    }
                }
            }
        }
    }

}