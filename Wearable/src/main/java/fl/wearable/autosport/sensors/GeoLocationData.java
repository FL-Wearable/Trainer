package fl.wearable.autosport.sensors;

import android.location.Location;

import fl.wearable.autosport.lib.SensorData;

public class GeoLocationData extends SensorData {
    private Location location;

    public GeoLocationData(long timestamp, Location location) {
        super(timestamp, -1);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
