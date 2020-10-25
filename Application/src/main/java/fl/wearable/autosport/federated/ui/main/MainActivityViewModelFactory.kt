package fl.wearable.autosport.federated.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fl.wearable.autosport.federated.service.WorkerRepository
import fl.wearable.autosport.federated.ui.work.WorkInfoViewModel

@Suppress("UNCHECKED_CAST")
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class MainActivityViewModelFactory(
    private val baseURL: String,
    private val authToken: String,
    private val workerRepository: WorkerRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java))
            return MainActivityViewModel(
                baseURL,
                authToken,
                workerRepository
            ) as T
        throw IllegalArgumentException("unknown view model class")
    }
}