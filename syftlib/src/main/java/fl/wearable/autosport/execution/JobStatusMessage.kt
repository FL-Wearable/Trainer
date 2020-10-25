package fl.wearable.autosport.execution

import fl.wearable.autosport.networking.datamodels.ClientConfig
import fl.wearable.autosport.proto.SyftModel
import java.util.concurrent.ConcurrentHashMap

@ExperimentalUnsignedTypes
internal sealed class JobStatusMessage {
    class JobCycleRejected(val timeout: String) : JobStatusMessage()

    class JobReady(
        val model: SyftModel,
        val plans: ConcurrentHashMap<String, Plan>,
        val clientConfig: ClientConfig?
    ) : JobStatusMessage()
}