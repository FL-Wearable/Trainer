package fl.wearable.autosport.sensors;

import android.location.GnssMeasurementsEvent;
import android.util.Log;

public class GnssMeasurementsCallback extends GnssMeasurementsEvent.Callback {
    private static final String TAG = GnssMeasurementsCallback.class.getSimpleName();

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
        Log.d(TAG, eventArgs.getClock().toString());

        /*StringBuilder sb = new StringBuilder();
        for (GnssMeasurement measurement : eventArgs.getMeasurements()) {
            sb.append(measurement.getConstellationType());
            sb.append(", ");
        }
        Log.i(TAG, sb.toString());*/
    }

    @Override
    public void onStatusChanged(int status) {
        Log.d(TAG, "GnssMeasurementsEvent.onStatusChanged " + status);
    }
}
