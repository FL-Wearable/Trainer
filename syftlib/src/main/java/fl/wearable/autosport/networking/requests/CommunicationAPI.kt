package fl.wearable.autosport.networking.requests

import io.reactivex.Single
import fl.wearable.autosport.networking.datamodels.syft.AuthenticationRequest
import fl.wearable.autosport.networking.datamodels.syft.AuthenticationResponse
import fl.wearable.autosport.networking.datamodels.syft.CycleRequest
import fl.wearable.autosport.networking.datamodels.syft.CycleResponseData
import fl.wearable.autosport.networking.datamodels.syft.ReportRequest
import fl.wearable.autosport.networking.datamodels.syft.ReportResponse


internal interface CommunicationAPI {
    fun authenticate(authRequest: AuthenticationRequest): Single<AuthenticationResponse>

    fun getCycle(cycleRequest: CycleRequest): Single<CycleResponseData>

    fun report(reportRequest: ReportRequest): Single<ReportResponse>
}