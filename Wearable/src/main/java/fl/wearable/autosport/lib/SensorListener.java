package fl.wearable.autosport.lib;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import fl.wearable.autosport.sensors.HeartRateListener;

public abstract class SensorListener implements SensorEventListener {
    private static final String TAG = HeartRateListener.class.getSimpleName();

    private int accuracy;

    public int getAccuracy() {
        return accuracy;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String strAccuracy;
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                strAccuracy = "high";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                strAccuracy = "medium";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                strAccuracy = "low";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                strAccuracy = "unreliable";
                break;
            case SensorManager.SENSOR_STATUS_NO_CONTACT:
                strAccuracy = "no contact";
                break;
            default:
                strAccuracy = String.valueOf(accuracy);
        }

        Log.i(TAG, sensor.getName() + " " + strAccuracy);
        this.accuracy = accuracy;
    }
}
