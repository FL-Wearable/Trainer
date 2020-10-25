package fl.wearable.autosport.sensors;

import fl.wearable.autosport.lib.SensorData;

public class HeartRateSensorData extends SensorData {
    private int heartRate;

    public HeartRateSensorData(long timestamp, int heartRate, int accuracy) {
        super(timestamp, accuracy);
        this.heartRate = heartRate;
    }

    public int getHeartRate() {
        return heartRate;
    }
}
