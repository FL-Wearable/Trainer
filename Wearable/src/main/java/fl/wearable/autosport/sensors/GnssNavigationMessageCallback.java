package fl.wearable.autosport.sensors;

import android.location.GnssNavigationMessage;
import android.util.Log;

public class GnssNavigationMessageCallback extends GnssNavigationMessage.Callback {
    private static final String TAG = GnssNavigationMessageCallback.class.getSimpleName();

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
        Log.d(TAG, "GnssNavigationMessage.onGnssNavigationMessageReceived");
    }

    @Override
    public void onStatusChanged(int status) {
        Log.d(TAG, "GnssNavigationMessage.onStatusChanged " + status);
    }
}
