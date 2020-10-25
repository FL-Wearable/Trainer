package fl.wearable.autosport.sensors;

import fl.wearable.autosport.lib.SensorData;

public class GyroscopeSensorData extends SensorData {
    private final float[] gyroscope;

    public GyroscopeSensorData(long timestamp, float[] gyroscope, int accuracy) {
        super(timestamp, accuracy);
        this.gyroscope = gyroscope.clone();
    }

    public float[] getGyroscope() {
        return gyroscope;
    }
}
