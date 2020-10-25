package fl.wearable.autosport.lib;

import fl.wearable.autosport.sensors.AcceleratorSensorData;
import fl.wearable.autosport.sensors.GeoLocationData;
import fl.wearable.autosport.sensors.GyroscopeSensorData;
import fl.wearable.autosport.sensors.HeartRateSensorData;

import java.util.List;

public interface ISensorReadout {
    void setGeoLocationAlwaysActive(boolean state);

    void startSportActivity(boolean heartRate, boolean accelerator, boolean gyroscope, boolean geoLocation);

    void stopSportActivity();

    long getSportActivityDurationNs();

    long getSportActivityStartTimeRtc();

    long getSportActivityStopTimeRtc();

    long getSportActivityStartTimeNs();

    long getSportActivityStopTimeNs();

    boolean isSportActivityRunning();

    void resetSportActivity();

    <T extends SensorData> void registerDataListener(T[] clazz, IDataListener<T> dataListener);

    /*List<? extends HeartRateSensorData> getHeartRateData(int startFromIndex);
    List<? extends GeoLocationData> getGeoLocationData(int startFromIndex);
    List<? extends AcceleratorSensorData> getAcceleratorData(int startFromIndex);*/
    List<? extends HeartRateSensorData> getHeartRateData();

    List<? extends GeoLocationData> getGeoLocationData();

    List<? extends AcceleratorSensorData> getAcceleratorData();

    List<? extends GyroscopeSensorData> getGyroscopeData();

    float getAvgSpeed();

    float getGeoAccuracy();

    int getBestSatellitesCount();
}
