package fl.wearable.autosport.sensors;

import android.location.GnssStatus;
import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;

public class GnssStatusCallback extends GnssStatus.Callback {
    private static final String TAG = GnssStatusCallback.class.getSimpleName();

    private static final long MAX_STATUS_AGE_MS = 10L * 1000;

    private long numSatellitesTimestamp;
    private int[] numSatellitesUsed = new int[8];

    public int getNumSatellitesUsed(int constellation) {
        return numSatellitesUsed[constellation];
    }

    public int getBestSatellitesCount() {
        int max = 0;

        if (numSatellitesTimestamp >= SystemClock.elapsedRealtime() - MAX_STATUS_AGE_MS) {
            for (int num : numSatellitesUsed) {
                if (num > max)
                    max = num;
            }
        }

        return max;
    }

    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
        numSatellitesTimestamp = SystemClock.elapsedRealtime();
        int[] localNumSatellitesUsed = new int[numSatellitesUsed.length];

        for (int n = status.getSatelliteCount() - 1; n >= 0; n--) {
            if (status.usedInFix(n)) {
                int constellation = status.getConstellationType(n);
                if (constellation < localNumSatellitesUsed.length && constellation >= 0) {
                    localNumSatellitesUsed[constellation]++;
                }
            }
        }

        for (int n = localNumSatellitesUsed.length - 1; n >= 0; n--) {
            numSatellitesUsed[n] = localNumSatellitesUsed[n];
        }
        Log.d(TAG, "Num satellites used " + Arrays.toString(localNumSatellitesUsed));

                /*StringBuilder sb = new StringBuilder();

                int numGps = 0, numGlonass = 0, numBeidou = 0, numOthers = 0,
                    numGpsUsed = 0, numGlonassUsed = 0, numBeidouUsed = 0, numOthersUsed = 0;

                float avgSnrGps = 0, avgSnrGlonass = 0, avgSnrBeidou = 0;*/

        /*int numSatellitesUsed = 0;

        for (int n = status.getSatelliteCount() - 1; n >= 0; n--) {
            if (status.usedInFix(n)) {
                numSatellitesUsed++;
            }

                    /*boolean used = status.usedInFix(n);
                    float snr = status.getCn0DbHz(n);

                    switch (status.getConstellationType(n)) {
                        case GnssStatus.CONSTELLATION_GPS:
                            numGps++;
                            if (used) {
                                avgSnrGps = (float)numGpsUsed / (numGpsUsed + 1) * avgSnrGps + (float)snr / (numGpsUsed + 1);
                                numGpsUsed++;
                            }
                            break;

                        case GnssStatus.CONSTELLATION_GLONASS:
                            numGlonass++;
                            if (used) {
                                avgSnrGlonass = (float)numGlonassUsed / (numGlonassUsed + 1) * avgSnrGlonass + (float)snr / (numGlonassUsed + 1);
                                numGlonassUsed++;
                            }
                            break;

                        case GnssStatus.CONSTELLATION_BEIDOU:
                            numBeidou++;
                            if (used) {
                                avgSnrBeidou = (float)numBeidouUsed / (numBeidouUsed + 1) * avgSnrBeidou + (float)snr / (numBeidouUsed + 1);
                                numBeidouUsed++;
                            }
                            break;

                        default:
                            numOthers++;
                            if (used) numOthersUsed++;
                    }

                    //sb.append(status.getSvid(n));
                    //sb.append(status.usedInFix(n));
                    //sb.append(status.hasAlmanacData(n));
                    //sb.append(status.hasEphemerisData(n));
                    //sb.append(status.getAzimuthDegrees(n));
                    //sb.append(status.getElevationDegrees(n));
                    //sb.append(status.getCn0DbHz(n));
                    //status.getCarrierFrequencyHz(n);
                    //status.hasCarrierFrequencyHz(n);
                    //sb.append("; ");*/
        //}

                /*Log.i("milt/GNSS", "GPS " + numGpsUsed + "/" + numGps + " ~" + avgSnrGps
                        + " GLONASS " + numGlonassUsed + "/" + numGlonass + " ~" + avgSnrGlonass
                        + " BEIDOU " + numBeidouUsed + "/" + numBeidou + " ~" + avgSnrBeidou
                        + " OTH " + numOthersUsed + "/" + numOthers);*/

        //mGnssAccuracy = numSatellitesUsed;
    }
}
