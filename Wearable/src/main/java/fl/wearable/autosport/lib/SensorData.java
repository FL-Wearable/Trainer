package fl.wearable.autosport.lib;

public abstract class SensorData {
    private long timestamp;
    private int accuracy;

    public SensorData(long timestamp, int accuracy) {
        this.timestamp = timestamp;
        this.accuracy = accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getAccuracy() {
        return accuracy;
    }
}
