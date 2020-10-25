package fl.wearable.autosport.sensors;

import android.hardware.SensorEvent;

import fl.wearable.autosport.lib.ISensorConsumer;
import fl.wearable.autosport.lib.SensorListener;

public class AcceleratorListener extends SensorListener {
    private static final String TAG = AcceleratorListener.class.getSimpleName();
    private final ISensorConsumer consumer;

    public AcceleratorListener(ISensorConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        consumer.addData(new AcceleratorSensorData(event.timestamp, event.values, event.accuracy));
        //Log.i(TAG, "ax : "+event.values[0]+" ay : "+event.values[1]+" az : "+event.values[2]);
    }
}
