package fl.wearable.autosport.domain

import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import fl.wearable.autosport.datasource.JobLocalDataSource
import fl.wearable.autosport.datasource.JobRemoteDataSource
import fl.wearable.autosport.execution.JobStatusMessage
import fl.wearable.autosport.execution.Plan
import fl.wearable.autosport.execution.Protocol
import fl.wearable.autosport.networking.datamodels.ClientConfig
import fl.wearable.autosport.proto.SyftModel
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

internal const val PLAN_OP_TYPE = "torchscript"
private const val TAG = "JobDownloader"
private const val FILEDIR = "FilesDir location"

@ExperimentalUnsignedTypes
internal class JobRepository(
    private val jobLocalDataSource: JobLocalDataSource,
    private val jobRemoteDataSource: JobRemoteDataSource
) {

    private val trainingParamsStatus = AtomicReference(DownloadStatus.NOT_STARTED)
    val status: DownloadStatus
        get() = trainingParamsStatus.get()

    fun getDiffScript(config: SyftConfiguration) =
            jobLocalDataSource.getDiffScript(config)

    fun persistToLocalStorage(
        input: InputStream,
        parentDir: String,
        fileName: String,
        overwrite: Boolean = false
    ): String {
        return jobLocalDataSource.save(input, parentDir, fileName, overwrite)
    }

    fun downloadData(
        workerId: String,
        config: SyftConfiguration,
        requestKey: String,
        networkDisposable: CompositeDisposable,
        jobStatusProcessor: PublishProcessor<JobStatusMessage>,
        clientConfig: ClientConfig?,
        plans: ConcurrentHashMap<String, Plan>,
        model: SyftModel,
        protocols: ConcurrentHashMap<String, Protocol>
    ) {
        Log.d(TAG, "beginning download")
        trainingParamsStatus.set(DownloadStatus.RUNNING)

        networkDisposable.add(
            Single.zip(
                getDownloadables(
                    workerId,
                    config,
                    requestKey,
                    model,
                    plans,
                    protocols
                )
            ) { successMessages ->
                successMessages.joinToString(
                    ",",
                    prefix = "files ",
                    postfix = " downloaded successfully"
                )
            }
                    .compose(config.networkingSchedulers.applySingleSchedulers())
                    .subscribe(
                        { successMsg: String ->
                            Log.d(TAG, successMsg)
                            trainingParamsStatus.set(DownloadStatus.COMPLETE)
                            jobStatusProcessor.offer(
                                JobStatusMessage.JobReady(
                                    model,
                                    plans,
                                    clientConfig
                                )
                            )
                        },
                        { e -> jobStatusProcessor.onError(e) }
                    )
        )
    }

    private fun getDownloadables(
        workerId: String,
        config: SyftConfiguration,
        request: String,
        model: SyftModel,
        plans: ConcurrentHashMap<String, Plan>,
        protocols: ConcurrentHashMap<String, Protocol>
    ): List<Single<String>> {
        val downloadList = mutableListOf<Single<String>>()
        plans.forEach { (_, plan) ->
            downloadList.add(
                processPlans(
                    workerId,
                    config,
                    request,
                    "${config.filesDir}/plans",
                    plan
                )
            )
        }
        Log.d(FILEDIR, config.filesDir.toString())
        protocols.forEach { (_, protocol) ->
            protocol.protocolFileLocation = "${config.filesDir}/protocols"
            downloadList.add(
                processProtocols(
                    workerId,
                    config,
                    request,
                    protocol.protocolFileLocation,
                    protocol.protocolId
                )
            )
        }
        downloadList.add(processModel(workerId, config, request, model))
        return downloadList
    }

    private fun processModel(
        workerId: String,
        config: SyftConfiguration,
        requestKey: String,
        model: SyftModel
    ): Single<String> {
        val modelId = model.pyGridModelId ?: throw IllegalStateException("Model id not initiated")
        return jobRemoteDataSource.downloadModel(workerId, requestKey, modelId)
                .flatMap { modelInputStream ->
                    jobLocalDataSource.saveAsync(
                        modelInputStream,
                        "${config.filesDir}/models",
                        //"$modelId.pb"
                        "model.pb"
                    )
                }.flatMap { modelFile ->
                    Single.create<String> { emitter ->
                        model.loadModelState(modelFile)
                        emitter.onSuccess(modelFile)
                    }
                }
                .compose(config.networkingSchedulers.applySingleSchedulers())
    }

    private fun processPlans(
        workerId: String,
        config: SyftConfiguration,
        requestKey: String,
        destinationDir: String,
        plan: Plan
    ): Single<String> {
        return jobRemoteDataSource.downloadPlan(
            workerId,
            requestKey,
            plan.planId,
            PLAN_OP_TYPE
        )
                .flatMap { planInputStream ->
                    jobLocalDataSource.saveAsync(
                        planInputStream,
                        destinationDir,
                        "${plan.planId}.pb"
                    )
                }.flatMap { filepath ->
                    Single.create<String> { emitter ->
                        val torchscriptLocation = jobLocalDataSource.saveTorchScript(
                            destinationDir,
                            filepath,
                            "torchscript_${plan.planId}.pt"
                        )
                        plan.loadScriptModule(torchscriptLocation)
                        emitter.onSuccess(filepath)
                    }
                }
                .compose(config.networkingSchedulers.applySingleSchedulers())

    }

    private fun processProtocols(
        workerId: String,
        config: SyftConfiguration,
        requestKey: String,
        destinationDir: String,
        protocolId: String
    ): Single<String> {
        return jobRemoteDataSource.downloadProtocol(workerId, requestKey, protocolId)
                .flatMap { protocolInputStream ->
                    jobLocalDataSource.saveAsync(
                        protocolInputStream,
                        destinationDir,
                        "$protocolId.pb"
                    )
                }
                .compose(config.networkingSchedulers.applySingleSchedulers())
    }
}

enum class DownloadStatus {
    NOT_STARTED, RUNNING, COMPLETE
}
