package fl.wearable.autosport.federated.logging

import androidx.lifecycle.MutableLiveData
import fl.wearable.autosport.federated.ui.ContentState
import fl.wearable.autosport.federated.ui.ProcessData

interface ActivityLogger {
    val logText: MutableLiveData<String>

    val steps: MutableLiveData<String>

    val processState: MutableLiveData<ContentState>

    val processData: MutableLiveData<ProcessData>

    fun postState(status: ContentState)

    fun postData(result: Float)

    fun postEpoch(epoch: Int)

    fun postLog(message: String)
}