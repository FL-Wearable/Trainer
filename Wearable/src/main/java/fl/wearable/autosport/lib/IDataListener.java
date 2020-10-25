package fl.wearable.autosport.lib;

public interface IDataListener<T extends SensorData> {
    void onDataReceived(T data);
}
