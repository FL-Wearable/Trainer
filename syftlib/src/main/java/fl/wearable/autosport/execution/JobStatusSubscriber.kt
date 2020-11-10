package fl.wearable.autosport.execution

import android.util.Log
import androidx.annotation.VisibleForTesting
import fl.wearable.autosport.datasource.JobLocalDataSource
import fl.wearable.autosport.networking.datamodels.ClientConfig
import fl.wearable.autosport.proto.SyftModel
import org.openmined.syftproto.execution.v1.StateOuterClass
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * This is passed as argument to [SyftJob.start] giving the overridden callbacks for different phases of the job cycle.
 * ```kotlin
 * val jobStatusSubscriber = object : JobStatusSubscriber() {
 *      override fun onReady(
 *      model: SyftModel,
 *      plans: ConcurrentHashMap<String, Plan>,
 *      clientConfig: ClientConfig
 *      ) {
 *      }
 *
 *      override fun onRejected(timeout: String) {
 *      }
 *
 *      override fun onError(throwable: Throwable) {
 *      }
 * }
 *
 * job.start(jobStatusSubscriber)
 * ```
 */
@ExperimentalUnsignedTypes
open class JobStatusSubscriber {
    /**
     * This method is called when KotlinSyft has downloaded all the plans and protocols from PyGrid and it is ready to train the model.
     * @param model stores the model weights given by PyGrid
     * @param plans is a HashMap of all the planIDs and their plans.
     * @param clientConfig has hyper parameters like batchsize, learning rate, number of steps, etc
     */
    open fun onReady(
        model: SyftModel,
        plans: ConcurrentHashMap<String, Plan>,
        clientConfig: ClientConfig
    ) {
    }

    /**
     * This method is called when the job cycle finishes successfully. Override this method to clear the worker and the jobs
     */
    open fun onComplete() {}

    /**
     * This method is called when the worker has been rejected from the cycle by the PyGrid
     * @param timeout is the timestamp indicating the time after which the worker should retry joining into the cycle. This will be empty if it is the last cycle.
     */
    open fun onRejected(timeout: String) {}

    /**
     * This method is called when the job throws an error
     * @param throwable contains the error message
     */
    open fun onError(throwable: Throwable) {}

    /**
     * Calls the respective user callbacks upon receiving a [JobStatusMessage]
     */
    internal fun onJobStatusMessage(jobStatusMessage: JobStatusMessage) {
        when (jobStatusMessage) {
            is JobStatusMessage.JobReady -> {
                if (jobStatusMessage.clientConfig != null)
                    onReady(
                        jobStatusMessage.model,
                        jobStatusMessage.plans,
                        jobStatusMessage.clientConfig
                    )
                else
                    onError(JobErrorThrowable.DownloadIncomplete("Client config not available yet"))
            }
            is JobStatusMessage.JobCycleRejected -> onRejected(jobStatusMessage.timeout)
            //add all the other messages as and when needed
        }
    }
    fun saveModel(dir: File,
                          model: SyftModel){
        val modelId = model.pyGridModelId ?: throw IllegalStateException("Model id not initiated")
        saveTorchModel(
            "$dir/models",
            "$modelId.pb",
            "$modelId.pt"
        )
    }
    fun saveTorchModel(parentDir: String, torchModelPath: String, fileName: String): String {
        val parent = File(parentDir)
        if (!parent.exists()) parent.mkdirs()
        val file = File(parent, fileName)
        file.createNewFile()
        val modelpb = File(parent, torchModelPath)

        val scriptModel = StateOuterClass.State.parseFrom(modelpb.readBytes())
        Log.d("save to pt", "here2")
        return saveTorchModel(file, scriptModel)
    }

    @ExperimentalUnsignedTypes
    @VisibleForTesting
    internal fun saveTorchModel(file: File, model: StateOuterClass.State): String {
        file.outputStream().use {
            it.write(model.toByteArray())
        }
        Log.d("write to pt","write finished")
        return file.absolutePath
    }
}