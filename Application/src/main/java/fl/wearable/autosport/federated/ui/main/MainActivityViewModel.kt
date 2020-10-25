package fl.wearable.autosport.federated.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import io.reactivex.disposables.CompositeDisposable
import fl.wearable.autosport.federated.domain.SportDataRepository
import fl.wearable.autosport.federated.domain.TrainingTask
import fl.wearable.autosport.federated.logging.ActivityLogger
import fl.wearable.autosport.federated.service.EPOCH
import fl.wearable.autosport.federated.service.LOG
import fl.wearable.autosport.federated.service.LOSS_LIST
import fl.wearable.autosport.federated.service.STATUS
import fl.wearable.autosport.federated.service.WorkerRepository
import fl.wearable.autosport.federated.ui.ContentState
import fl.wearable.autosport.federated.ui.ProcessData
import fl.wearable.autosport.domain.SyftConfiguration

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class MainActivityViewModel(
    val baseUrl: String,
    private val authToken: String,
    private val workerRepository: WorkerRepository
) : ActivityLogger, ViewModel() {
    override val logText
        get() = logTextInternal
    private val logTextInternal = MutableLiveData<String>()

    override val steps
        get() = stepsInternal
    private val stepsInternal = MutableLiveData<String>()

    override val processState
        get() = processStateInternal
    private val processStateInternal = MutableLiveData<ContentState>()

    override val processData
        get() = processDataInternal
    private val processDataInternal = MutableLiveData<ProcessData>()


    private val compositeDisposable = CompositeDisposable()
    private var trainingTask: TrainingTask? = null

    override fun postState(status: ContentState) {
        processStateInternal.postValue(status)
    }

    override fun postData(result: Float) {
        processDataInternal.postValue(
            ProcessData(
                (processDataInternal.value?.data ?: emptyList()) + result
            )
        )
    }

    override fun postEpoch(epoch: Int) {
        stepsInternal.postValue("Step : $epoch")
    }

    override fun postLog(message: String) {
        logTextInternal.postValue("${logTextInternal.value ?: ""}\n\n$message")
    }

    fun launchForegroundTrainer(config: SyftConfiguration, dataRepository: SportDataRepository) {
        trainingTask = TrainingTask(
            config,
            authToken,
            dataRepository
        )
        compositeDisposable.add(trainingTask!!.runTask(this).subscribe())
    }

    fun disposeTraining() {
        compositeDisposable.clear()
        trainingTask?.disposeTraining()
    }

    fun getRunningWorkInfo() = workerRepository.getRunningWorkStatus()?.let {
        workerRepository.getWorkInfo(it)
    }


    fun submitJob(): LiveData<WorkInfo> {
        val requestId = workerRepository.getRunningWorkStatus()
                        ?: workerRepository.submitJob(authToken, baseUrl)
        return workerRepository.getWorkInfo(requestId)
    }

    fun cancelAllJobs() {
        workerRepository.cancelAllWork()
    }

    fun getWorkInfoObserver() = Observer { workInfo: WorkInfo? ->
        if (workInfo != null) {
            val progress = workInfo.progress
            progress.getFloat(LOSS_LIST, -2.0f).takeIf { it > -1 }?.let {
                postData(it)
            }
            progress.getInt(EPOCH, -2).takeIf { it > -1 }?.let {
                postEpoch(it)
            }
            progress.getString(LOG)?.let {
                postLog(it)
            }
            postState(
                ContentState.getObjectFromString(
                    progress.getString(STATUS)
                ) ?: ContentState.Training
            )
        }
    }

}