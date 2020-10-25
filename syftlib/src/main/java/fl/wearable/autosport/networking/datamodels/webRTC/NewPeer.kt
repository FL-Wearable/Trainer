package fl.wearable.autosport.networking.datamodels.webRTC

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fl.wearable.autosport.networking.datamodels.NetworkModels

internal const val NEW_PEER_TYPE = "peer"

@Serializable
internal data class NewPeer(
    @SerialName("worker_id")
    val workerId: String
) : NetworkModels()
