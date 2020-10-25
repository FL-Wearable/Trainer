package fl.wearable.autosport.monitor

internal sealed class StateChangeMessage {
    data class Charging(val charging: Boolean) : StateChangeMessage()
    data class NetworkStatus(val connected: Boolean) : StateChangeMessage()
}