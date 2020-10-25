package fl.wearable.autosport.networking.datamodels.syft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fl.wearable.autosport.networking.datamodels.NetworkModels

@Serializable
internal data class SpeedCheckResponse(
    @SerialName("error")
    val error: String? = null
) : NetworkModels()