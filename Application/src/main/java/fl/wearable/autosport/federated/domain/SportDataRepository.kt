package fl.wearable.autosport.federated.domain

import fl.wearable.autosport.federated.datasource.LocalAllinoneDataSource
import org.pytorch.IValue
import org.pytorch.Tensor

class SportDataRepository constructor(
    private val LocalAllinoneDataSource: LocalAllinoneDataSource
) {
    fun loadDataBatch(batchSize: Int): Pair<IValue, IValue> {
        val data = LocalAllinoneDataSource.loadDataBatch(batchSize)
        val tensorsX = IValue.from(Tensor.fromBlob(data.first.flattenedArray, data.first.shape))

        val tensorsY = IValue.from(Tensor.fromBlob(data.second.flattenedArray, data.second.shape))
        return Pair(tensorsX, tensorsY)
    }
}