package fl.wearable.autosport.networking.requests

import io.reactivex.Single
import fl.wearable.autosport.networking.datamodels.webRTC.InternalMessageRequest
import fl.wearable.autosport.networking.datamodels.webRTC.InternalMessageResponse
import fl.wearable.autosport.networking.datamodels.webRTC.JoinRoomRequest
import fl.wearable.autosport.networking.datamodels.webRTC.JoinRoomResponse

/**
 * Represent WebRTC connection API
 * */
internal interface SocketAPI : CommunicationAPI {

    /**
     * Request joining a federated learning cycle
     * */
    fun joinRoom(joinRoomRequest: JoinRoomRequest): Single<JoinRoomResponse>

    /**
     * Send message via PyGrid
     * */
    fun sendInternalMessage(internalMessageRequest: InternalMessageRequest): Single<InternalMessageResponse>
}