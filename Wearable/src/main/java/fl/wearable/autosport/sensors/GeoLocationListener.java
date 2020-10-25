package fl.wearable.autosport.sensors;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import fl.wearable.autosport.lib.ISensorConsumer;

public class GeoLocationListener implements LocationListener {
    private static final String TAG = GeoLocationListener.class.getSimpleName();
    private static final long MAX_ACCURACY_AGE_NS = 10L * 1000 * 1000 * 1000;
    private ISensorConsumer consumer;
    private float[] lastSpeeds = new float[5];

    private long lastPositionTimestamp;
    private float lastPositionAccuracy;

    public GeoLocationListener(ISensorConsumer consumer) {
        this.consumer = consumer;
    }

    public float getAvgSpeed() {
        float result = 0;
        for (int n = 0; n < lastSpeeds.length; n++) {
            result += lastSpeeds[n] / lastSpeeds.length;
        }

        return  result;
    }

    public float getLastPositionAccuracy() {
        if (lastPositionTimestamp < SystemClock.elapsedRealtimeNanos() - MAX_ACCURACY_AGE_NS) {
            return Float.NaN;
        } else {
            return lastPositionAccuracy;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        GeoLocationData geoLocationData = new GeoLocationData(SystemClock.elapsedRealtimeNanos(), location);
        consumer.addData(geoLocationData);

        lastPositionTimestamp = location.getElapsedRealtimeNanos();
        lastPositionAccuracy = location.getAccuracy();

        Log.i(TAG, location.getLatitude() + ", " + location.getLongitude() + " +-" + location.getAccuracy() + " " + hashCode());

        for (int n = 1; n < lastSpeeds.length; n++) {
            lastSpeeds[n - 1] = lastSpeeds[n];
        }

        lastSpeeds[lastSpeeds.length - 1] = location.getSpeed();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Provider " + provider + " status changed to " + status + ".");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onProviderEnabled " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "onProviderDisabled " + provider);
    }
}
