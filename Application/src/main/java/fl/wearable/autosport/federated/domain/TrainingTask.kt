package fl.wearable.autosport.federated.domain

import android.content.ContentValues.TAG
import android.util.Log
import androidx.work.ListenableWorker.Result
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import fl.wearable.autosport.Syft
import fl.wearable.autosport.federated.logging.ActivityLogger
import fl.wearable.autosport.federated.ui.ContentState
import fl.wearable.autosport.domain.SyftConfiguration
import fl.wearable.autosport.execution.JobStatusSubscriber
import fl.wearable.autosport.execution.Plan
import fl.wearable.autosport.execution.SyftJob
import fl.wearable.autosport.networking.datamodels.ClientConfig
import fl.wearable.autosport.proto.SyftModel
import org.pytorch.IValue
import org.pytorch.Tensor
import java.util.concurrent.ConcurrentHashMap
import com.google.gson.GsonBuilder
import fl.wearable.autosport.login.LoginActivity
import java.io.File
import java.util.Arrays

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class TrainingTask(
    private val configuration: SyftConfiguration,
    private val authToken: String,
    private val sportDataRepository: SportDataRepository
) {
    private var syftWorker: Syft? = null
    val gsonPretty = GsonBuilder().setPrettyPrinting().create()
    val file =  File(LoginActivity.applicationContext().filesDir,"tmp.json")
    var dataTable = mutableMapOf<Int, Array<FloatArray>>()
    fun runTask(logger: ActivityLogger): Single<Result> {
        syftWorker = Syft.getInstance(configuration, authToken)
        val sportJob = syftWorker!!.newJob("smartsport", "1.0.0")
        val statusPublisher = PublishProcessor.create<Result>()

        logger.postLog("Smartsport job started \n\nChecking for download and upload speeds")
        logger.postState(ContentState.Loading)
        val jobStatusSubscriber = object : JobStatusSubscriber() {
            override fun onReady(
                model: SyftModel,
                plans: ConcurrentHashMap<String, Plan>,
                clientConfig: ClientConfig
            ) {
                logger.postLog("Model ${model.modelName} received.\n\nStarting training process")
                trainingProcess(sportJob, model, plans, clientConfig, logger)
            }

            override fun onComplete() {
                syftWorker?.dispose()
                statusPublisher.offer(Result.success())
            }

            override fun onRejected(timeout: String) {
                logger.postLog("We've been rejected for the time $timeout")
                statusPublisher.offer(Result.retry())
            }

            override fun onError(throwable: Throwable) {
                logger.postLog("There was an error $throwable")
                Log.e(TAG, throwable.toString())
                statusPublisher.offer(Result.failure())
            }
        }
        sportJob.start(jobStatusSubscriber)
        return statusPublisher.onBackpressureBuffer().firstOrError()
    }

    fun disposeTraining() {
        syftWorker?.dispose()
    }

    private fun trainingProcess(
        sportJob: SyftJob,
        model: SyftModel,
        plans: ConcurrentHashMap<String, Plan>,
        clientConfig: ClientConfig,
        logger: ActivityLogger
    ) {
        var result = -0.0f
        plans["training_plan"]?.let { plan ->
            repeat(clientConfig.properties.maxUpdates) { step ->
                logger.postEpoch(step + 1)
                val batchSize = (clientConfig.planArgs["batch_size"]
                                 ?: error("batch_size doesn't exist")).toInt()
                val batchIValue = IValue.from(
                    Tensor.fromBlob(longArrayOf(batchSize.toLong()), longArrayOf(1))
                )
                val lr = IValue.from(
                    Tensor.fromBlob(
                        floatArrayOf(
                            (clientConfig.planArgs["lr"] ?: error("lr doesn't exist")).toFloat()
                        ),
                        longArrayOf(1)
                    )
                )
                val momentum = IValue.from(
                    Tensor.fromBlob(
                        floatArrayOf(
                            (clientConfig.planArgs["momentum"] ?: error("momentum doesn't exist")).toFloat()
                        ),
                        longArrayOf(1)
                    )
                )
                val small_val = IValue.from(
                    Tensor.fromBlob(
                        floatArrayOf(
                            (clientConfig.planArgs["small_val"] ?: error("small_val doesn't exist")).toFloat()
                        ),
                        longArrayOf(1)
                    )
                )

                val batchData =
                        sportDataRepository.loadDataBatch(batchSize)
                val modelParams = model.paramArray ?: return
                val paramIValue = IValue.listFrom(*modelParams)

                val output = plan.execute(
                    batchData.first,
                    batchData.second,
                    batchIValue,
                    lr,
                    momentum,
                    paramIValue,
                    minusParamIValue,
                    small_val
                )?.toTuple()
                output?.let { outputResult ->
                    val paramSize = model.stateTensorSize!!
                    val beginIndex = outputResult.size - paramSize
                    val updatedParams =
                            outputResult.slice(beginIndex until outputResult.size)
                    var index = 0
                    for (param in updatedParams) {
                        var tensorTmp = param.toTensor().dataAsFloatArray
                        var tensorShape = param.toTensor().shape()
                        var indexRow = 0
                        var indexCol = 0
                        if (tensorShape.size>1) {
                            var values = Array(tensorShape[0].toInt(),{FloatArray(tensorShape[1].toInt())})
                            while (indexRow < tensorShape[0]){
                                while (indexCol < tensorShape[1]){
                                    values[indexRow][indexCol]=tensorTmp[indexRow*(tensorShape[1].toInt())+indexCol]
                                    indexCol++
                                }
                                indexCol=0
                                indexRow++
                            }
                            dataTable[index] = values
                        }
                        else{
                            var values = Array(1,{FloatArray(tensorShape[0].toInt())})
                            while (indexCol<tensorShape[0]){
                                values[0][indexCol] = tensorTmp[indexCol]
                                indexCol++
                            }
                            dataTable[index] = values

                        }
                        index++
                        /*var iter = param.toTensor().dataAsFloatArray.iterator()
                        Log.d(TAG, "the $index iter is")
                        while (iter.hasNext()) {
                            Log.d(TAG, iter.next().toString())
                        }*/
                    }
                    var jsonData = gsonPretty.toJson(dataTable)
                    file.writeText(jsonData)
                    model.updateModel(updatedParams)
                    result = outputResult[0].toTensor().dataAsFloatArray.last()
                }
                logger.postState(ContentState.Training)
                logger.postData(result)
            }
            logger.postLog("Training done!\n reporting diff")
            val diff = sportJob.createDiff()
            sportJob.report(diff)
            logger.postLog("reported the model to PyGrid")
        }
    }
}