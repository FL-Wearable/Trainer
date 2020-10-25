package fl.wearable.autosport.sensors;

import fl.wearable.autosport.lib.SensorData;

public class AcceleratorSensorData extends SensorData {
    private final float[] acceleration;

    public AcceleratorSensorData(long timestamp, float[] acceleration, int accuracy) {
        super(timestamp, accuracy);
        this.acceleration = acceleration.clone();
    }

    public float[] getAcceleration() {
        return acceleration;
    }
}
