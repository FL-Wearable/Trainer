package fl.wearable.autosport.monitor.battery

import fl.wearable.autosport.domain.SyftConfiguration
import fl.wearable.autosport.monitor.BroadCastListener

@ExperimentalUnsignedTypes
internal class BatteryStatusRepository internal constructor(
    private val batteryStatusDataSource: BatteryStatusDataSource
) : BroadCastListener{
    companion object {
        fun initialize(configuration: SyftConfiguration): BatteryStatusRepository {
            return BatteryStatusRepository(BatteryStatusDataSource.initialize(configuration))
        }
    }

    fun getBatteryValidity() = batteryStatusDataSource.getBatteryValidity()

    fun getBatteryState() = BatteryStatusModel(
        batteryStatusDataSource.checkIfCharging(),
        batteryStatusDataSource.getBatteryLevel(),
        System.currentTimeMillis()
    )

    override fun subscribeStateChange() = batteryStatusDataSource.subscribeStateChange()

    override fun unsubscribeStateChange() {
        batteryStatusDataSource.unsubscribeStateChange()
    }

}