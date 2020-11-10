package fl.wearable.autosport.federated.domain

import android.content.Context
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
import fl.wearable.autosport.login.LoginActivity
import fl.wearable.autosport.networking.datamodels.ClientConfig
import fl.wearable.autosport.proto.SyftModel
import org.pytorch.IValue
import org.pytorch.Tensor
import java.util.concurrent.ConcurrentHashMap

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class TrainingTask(
    private val configuration: SyftConfiguration,
    private val authToken: String,
    private val sportDataRepository: SportDataRepository
) {
    private var syftWorker: Syft? = null

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
                saveModel(LoginActivity.applicationContext().filesDir,model)
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
                val batchData =
                        sportDataRepository.loadDataBatch(batchSize)
                val modelParams = model.paramArray ?: return
                val paramIValue = IValue.listFrom(*modelParams)
                val output = plan.execute(
                    batchData.first,
                    batchData.second,
                    batchIValue,
                    lr, paramIValue
                )?.toTuple()
                output?.let { outputResult ->
                    val paramSize = model.stateTensorSize!!
                    val beginIndex = outputResult.size - paramSize
                    val updatedParams =
                            outputResult.slice(beginIndex until outputResult.size)
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