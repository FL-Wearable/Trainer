package fl.wearable.autosport.federated.ui.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fl.wearable.autosport.federated.service.WorkerRepository

@Suppress("UNCHECKED_CAST")
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class WorkInfoViewModelFactory(private val workerRepository: WorkerRepository) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkInfoViewModel::class.java))
            return WorkInfoViewModel(
                workerRepository
            ) as T
        throw IllegalArgumentException("unknown view model class")
    }
}