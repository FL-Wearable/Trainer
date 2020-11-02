package fl.wearable.autosport.lib;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import fl.wearable.autosport.sensors.AcceleratorListener;
import fl.wearable.autosport.sensors.AcceleratorSensorData;
import fl.wearable.autosport.sensors.GeoLocationData;
import fl.wearable.autosport.sensors.GeoLocationListener;
import fl.wearable.autosport.sensors.GnssMeasurementsCallback;
import fl.wearable.autosport.sensors.GnssNavigationMessageCallback;
import fl.wearable.autosport.sensors.GnssStatusCallback;
import fl.wearable.autosport.sensors.GyroscopeListener;
import fl.wearable.autosport.sensors.GyroscopeSensorData;
import fl.wearable.autosport.sensors.HeartRateListener;
import fl.wearable.autosport.sensors.HeartRateSensorData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class SensorCollector extends Service implements ISensorReadout, ISensorConsumer {
    private static final String TAG = SensorCollector.class.getSimpleName();

    private GeoLocationListener geoLocationListener = new GeoLocationListener(this);
    private GnssStatusCallback gnssStatusCallback = new GnssStatusCallback();
    private GnssMeasurementsCallback gnssMeasurementsCallback = new GnssMeasurementsCallback();
    private GnssNavigationMessageCallback gnssNavigationMessageCallback = new GnssNavigationMessageCallback();
    private HeartRateListener heartRateListener = new HeartRateListener(this);
    private AcceleratorListener acceleratorListener = new AcceleratorListener(this);
    private GyroscopeListener gyroscopeListener = new GyroscopeListener(this);

    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor, mAcceleratorSensor, mGyroscopeSensor;
    private LocationManager mLocationManager;
    private final IBinder binder = new LocalBinder();

    private List<HeartRateSensorData> heartRateData = new ArrayList<>();
    private List<GeoLocationData> geoLocationData = new ArrayList<>();
    private List<AcceleratorSensorData> acceleratorData = new ArrayList<>();
    private List<GyroscopeSensorData> gyroscopeData = new ArrayList<>();

    private final Object geoSensorStateLock = new Object();
    //TODO: bug here
    private Collection<IDataListener<? extends SensorData>> acceleratorListeners = new HashSet<>();
    private Collection<IDataListener<? extends SensorData>> gyroscopeListeners = new HashSet<>();
    private Collection<IDataListener<? extends SensorData>> geoLocationListeners = new HashSet<>();
    private Collection<IDataListener<? extends SensorData>> heartRateListeners = new HashSet<>();

    private boolean mGeoLocationShouldBeActive = false, mIsGeoLocationActive = false, mIsGeoLocationRecorded = false;
    private long startTime, startTimeRtc, stopTime, stopTimeRtc;

    public SensorCollector() {
        Log.d(TAG, "Creating a new instance " + hashCode());
    }

    public class LocalBinder extends Binder {
        public ISensorReadout getService() {
            return SensorCollector.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // guarantee the service to be restarted in case of a crash
        Log.d(TAG, "onStartCommand " + intent);
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind " + intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        deactivateAllSensors(true);
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved " + rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind " + intent);
        return binder;
    }

    @Override
    public void setGeoLocationAlwaysActive(boolean state) {
        synchronized (geoSensorStateLock) {
            if (mGeoLocationShouldBeActive == state) {
                // no changes needed
                return;
            }

            if (state && !mIsGeoLocationActive) {
                // should be on, but it is not (doesn't matter the recording)
                activateGeoSensor();
            } else if (!state && mIsGeoLocationActive && !mIsGeoLocationRecorded) {
                // should be off, but it is on and is not being recorder right now
                deactivateGeoSensor();
            }

            // sync variables
            mGeoLocationShouldBeActive = state;
        }
    }

    @Override
    public void startSportActivity(boolean heartRate, boolean acceleration, boolean gyroscope, boolean geoLocation) {
        activateSensors(heartRate, acceleration, gyroscope, geoLocation);
        startTime = SystemClock.elapsedRealtimeNanos();
        startTimeRtc = System.currentTimeMillis();
        stopTime = 0;
        stopTimeRtc = 0;
    }

    @Override
    public void stopSportActivity() {
        deactivateAllSensors(false);
        if (stopTime == 0) {
            stopTime = SystemClock.elapsedRealtimeNanos();
            stopTimeRtc = System.currentTimeMillis();
        }
    }

    @Override
    public long getSportActivityDurationNs() {
        if (startTime == 0) {
            return 0;
        } else if (stopTime == 0) {
            return SystemClock.elapsedRealtimeNanos() - startTime;
        } else {
            return stopTime - startTime;
        }
    }

    @Override
    public long getSportActivityStartTimeRtc() {
        return startTimeRtc;
    }

    @Override
    public long getSportActivityStopTimeRtc() {
        return stopTimeRtc;
    }

    @Override
    public long getSportActivityStartTimeNs() {
        return startTime;
    }

    @Override
    public long getSportActivityStopTimeNs() {
        return stopTime;
    }

    @Override
    public boolean isSportActivityRunning() {
        return startTime != 0 && stopTime == 0;
    }

    @Override
    public void resetSportActivity() {
        startTime = 0;
        startTimeRtc = 0;
        stopTime = 0;
        stopTimeRtc = 0;
        synchronized (heartRateData) {
            heartRateData.clear();
        }
        synchronized (geoLocationData) {
            geoLocationData.clear();
        }
        synchronized (acceleratorData) {
            acceleratorData.clear();
        }
        synchronized (gyroscopeData) {
            gyroscopeData.clear();
        }
    }

    private List<? extends HeartRateSensorData> getHeartRateData(int startFromIndex) {
        List<HeartRateSensorData> copy = new ArrayList<>();
        try {
            synchronized (heartRateData) {
                Iterator<HeartRateSensorData> iterator = heartRateData.listIterator(startFromIndex);
                while (iterator.hasNext()) {
                    copy.add(iterator.next());
                }
            }
        } catch (IndexOutOfBoundsException ex) {
            Log.e(TAG, "Requested data elements of of range " + startFromIndex + " while having (not-synced) " + heartRateData.size());
        }

        return copy;
    }

    @Override
    public List<? extends HeartRateSensorData> getHeartRateData() {
        return getHeartRateData(0);
    }

    private List<? extends GeoLocationData> getGeoLocationData(int startFromIndex) {
        List<GeoLocationData> copy = new ArrayList<>();
        try {
            synchronized (geoLocationData) {
                Iterator<GeoLocationData> iterator = geoLocationData.listIterator(startFromIndex);
                while (iterator.hasNext()) {
                    copy.add(iterator.next());
                }
            }
        } catch (IndexOutOfBoundsException ex) {
            Log.e(TAG, "Requested data elements of of range " + startFromIndex + " while having (not-synced) " + geoLocationData.size());
        }

        return copy;
    }

    @Override
    public List<? extends GeoLocationData> getGeoLocationData() {
        return getGeoLocationData(0);
    }

    private List<? extends AcceleratorSensorData> getAcceleratorData(int startFromIndex) {
        List<AcceleratorSensorData> copy = new ArrayList<>();
        try {
            synchronized (acceleratorData) {
                Iterator<AcceleratorSensorData> iterator = acceleratorData.listIterator(startFromIndex);
                while (iterator.hasNext()) {
                    copy.add(iterator.next());
                }
            }
        } catch (IndexOutOfBoundsException ex) {
            Log.e(TAG, "Requested data elements of of range " + startFromIndex + " while having (not-synced) " + acceleratorData.size());
        }

        return copy;
    }

    @Override
    public List<? extends AcceleratorSensorData> getAcceleratorData() {
        return getAcceleratorData(0);
    }

    private List<? extends GyroscopeSensorData> getGyroscopeData(int startFromIndex) {
        List<GyroscopeSensorData> copy = new ArrayList<>();
        try {
            synchronized (gyroscopeData) {
                Iterator<GyroscopeSensorData> iterator = gyroscopeData.listIterator(startFromIndex);
                while (iterator.hasNext()) {
                    copy.add(iterator.next());
                }
            }
        } catch (IndexOutOfBoundsException ex) {
            Log.e(TAG, "Requested data elements of of range " + startFromIndex + " while having (not-synced) " + acceleratorData.size());
        }

        return copy;
    }

    @Override
    public List<? extends GyroscopeSensorData> getGyroscopeData() {
        return getGyroscopeData(0);
    }


    @Override
    public <T extends SensorData> void registerDataListener(T[] clazz, IDataListener<T> dataListener) {
        Class requestedClass = clazz.getClass().getComponentType();
        if (requestedClass.isAssignableFrom(AcceleratorSensorData.class)) {
            synchronized (acceleratorListeners) {
                acceleratorListeners.add(dataListener);
                Log.i(TAG, "Total acceleration listeners " + acceleratorListeners.size());
            }
        } else if (requestedClass.isAssignableFrom(GyroscopeSensorData.class)) {
            synchronized (gyroscopeListeners) {
                gyroscopeListeners.add(dataListener);
                Log.i(TAG, "Total gyroscope listeners " + gyroscopeListeners.size());
            }
        } else if (requestedClass.isAssignableFrom(GeoLocationData.class)) {
            synchronized (geoLocationListeners) {
                geoLocationListeners.add(dataListener);
                Log.i(TAG, "Total geo location listeners " + geoLocationListeners.size());
            }
        } else if (requestedClass.isAssignableFrom(HeartRateSensorData.class)) {
            synchronized (heartRateListeners) {
                heartRateListeners.add(dataListener);
                Log.i(TAG, "Total heart rate listeners " + heartRateListeners.size());
            }
        } else {
            Log.e(TAG, "Can't register a listener for an unsupported class " + requestedClass.getName());
        }
    }

    @Override
    public float getAvgSpeed() {
        return geoLocationListener.getAvgSpeed();
    }

    @Override
    public float getGeoAccuracy() {
        return geoLocationListener.getLastPositionAccuracy();
    }

    @Override
    public int getBestSatellitesCount() {
        return gnssStatusCallback.getBestSatellitesCount();
    }

    @Override
    public void addData(SensorData data) {
        if (data instanceof HeartRateSensorData) {
            synchronized (heartRateData) {
                heartRateData.add((HeartRateSensorData)data);
            }

            synchronized (heartRateListeners) {
                for (IDataListener listener : heartRateListeners) {
                    listener.onDataReceived(data);
                }
            }
        } else if (data instanceof GeoLocationData) {
            if (mIsGeoLocationRecorded) {
                synchronized (geoLocationData) {
                    geoLocationData.add((GeoLocationData) data);
                }
            }

            synchronized (geoLocationListeners) {
                for (IDataListener listener : geoLocationListeners) {
                    listener.onDataReceived(data);
                }
            }
        } else if (data instanceof AcceleratorSensorData) {
            synchronized (acceleratorData) {
                acceleratorData.add((AcceleratorSensorData) data);
            }

            synchronized (acceleratorListeners) {
                for (IDataListener listener : acceleratorListeners) {
                    listener.onDataReceived(data);
                }
            }
        } else if (data instanceof GyroscopeSensorData) {
            synchronized (gyroscopeData) {
                gyroscopeData.add((GyroscopeSensorData) data);
            }

            synchronized (gyroscopeListeners) {
                for (IDataListener listener : gyroscopeListeners) {
                    listener.onDataReceived(data);
                }
            }
        } else {
            Log.w(TAG, "Unsupported sensor data type of " + data.getClass().getName());
        }
    }

    // ISensorConsumer

    private void activateGeoSensor() {
        synchronized (geoSensorStateLock) {
            Log.d(TAG, "Activating the geo location receiver.");

            if (mLocationManager == null) {
                mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            }

            boolean gnssProviderAvailable = false;
            for (String provider : mLocationManager.getProviders(false)) {
                Log.d(TAG, "Geo location provider found " + provider);
                if (LocationManager.GPS_PROVIDER.equals(provider)) {
                    gnssProviderAvailable = true;
                }
            }

            if (gnssProviderAvailable) {
                try {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, geoLocationListener);
                    mLocationManager.registerGnssStatusCallback(gnssStatusCallback);
                    mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsCallback);
                    mLocationManager.registerGnssNavigationMessageCallback(gnssNavigationMessageCallback);

                    mIsGeoLocationActive = true;
                } catch (SecurityException ex) {
                    Log.e(TAG, "Access denied to the GNSS receiver.", ex);
                }
            } else {
                Log.e(TAG, "No GNSS provider found.");
            }
        }
    }

    // internal implementation

    private void deactivateGeoSensor() {
        synchronized (geoSensorStateLock) {
            if (mLocationManager != null) {
                mLocationManager.removeUpdates(geoLocationListener);
                mLocationManager.unregisterGnssStatusCallback(gnssStatusCallback);
                mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsCallback);
                mLocationManager.unregisterGnssNavigationMessageCallback(gnssNavigationMessageCallback);

                mIsGeoLocationActive = false;
            }
        }
    }

    private void activateSensors(boolean heartRate, boolean accelerator, boolean gyroscope, boolean geoLocation) {
        if (mSensorManager == null) {
            mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        }

        if (heartRate) {
            Log.d(TAG, "Activating the heart rate senor.");

            if (mHeartRateSensor == null) {
                mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            }

            if (mHeartRateSensor == null) {
                Log.e(TAG, "No access to the heart rate sensor.");
            } else {
                mSensorManager.registerListener(heartRateListener, mHeartRateSensor, 40000);
            }
        }

        if (accelerator) {
            Log.d(TAG, "Activating the accelerometer .");

            if (mAcceleratorSensor == null) {
                mAcceleratorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }

            if (mAcceleratorSensor == null) {
                Log.e(TAG, "No access to the accelerometer .");
            } else {
                //Note that the following lines specifying the delay in microseconds only works from Android 2.3 (API level 9) onwards.
                mSensorManager.registerListener(acceleratorListener, mAcceleratorSensor, 40000);
            }
        }

        if (gyroscope) {
            Log.d(TAG, "Activating the gyroscope.");

            if (mGyroscopeSensor == null) {
                mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            }

            if (mGyroscopeSensor == null) {
                Log.e(TAG, "No access to the gyroscope.");
            } else {
                mSensorManager.registerListener(gyroscopeListener, mGyroscopeSensor, 40000);
            }
        }


        if (geoLocation) {
            synchronized (geoSensorStateLock) {
                if (!mIsGeoLocationActive) {
                    activateGeoSensor();
                }

                mIsGeoLocationRecorded = true;
            }
        }
    }

    /**
     * Deactivates all sensors by removing all listeners, but those that should work all the time in the background.
     *
     * @param disposing if true deactivate even the background listeners, false keeps the background listeners
     */
    private void deactivateAllSensors(boolean disposing) {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(heartRateListener);
            mSensorManager.unregisterListener(acceleratorListener);
            mSensorManager.unregisterListener(gyroscopeListener);
        }

        synchronized (geoSensorStateLock) {
            if (disposing || !mGeoLocationShouldBeActive) {
                deactivateGeoSensor();
            }

            mIsGeoLocationRecorded = false;
        }
    }


}
