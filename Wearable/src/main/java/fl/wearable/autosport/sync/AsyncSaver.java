package fl.wearable.autosport.sync;


import android.content.Context;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import fl.wearable.autosport.lib.BitUtility;
import fl.wearable.autosport.lib.FileItem;
import fl.wearable.autosport.lib.ISensorReadout;
import fl.wearable.autosport.model.Classifier;
import fl.wearable.autosport.model.Utils;
import fl.wearable.autosport.sensors.AcceleratorSensorData;
import fl.wearable.autosport.sensors.GeoLocationData;
import fl.wearable.autosport.sensors.GyroscopeSensorData;
import fl.wearable.autosport.sensors.HeartRateSensorData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.ArrayList;

import static fl.wearable.autosport.lib.Constants.COMMA_DELIMITER;
import static fl.wearable.autosport.lib.Constants.FILE_HEADER;
import static fl.wearable.autosport.lib.Constants.NEW_LINE_SEPARATOR;

public class AsyncSaver extends AsyncTask<ISensorReadout, Float, Pair> {
    private static final String TAG = AsyncSaver.class.getSimpleName();

    private final Consumer<Pair> finishedCallback;
    private final File targetDirectory;
    private Classifier classifier;
    private Context mContext;
    private float[] tmp;
    private ArrayList<Integer> recent_n_result = new ArrayList<>();
    private ArrayList<Integer> inferenceResults = new ArrayList<>();


    public AsyncSaver(Consumer<Pair> finishedCallback, File targetDirectory, Context context) {
        this.finishedCallback = finishedCallback;
        this.targetDirectory = targetDirectory;
        mContext = context;
        classifier = new Classifier(Utils.assetFilePath(mContext,"model.pt"));
    }

    @Override
    protected Pair doInBackground(ISensorReadout... iSensorReadouts) {
        if (iSensorReadouts == null || iSensorReadouts.length == 0) {
            return new Pair(0,0);
        }

        int succeeded = 0;
        int inferenceResult = 0;
        FileWriter fw = null;
        long curentCsvName = System.currentTimeMillis();
        for (ISensorReadout sensorReadout : iSensorReadouts) {
            List<? extends HeartRateSensorData> heartRateSensorData = sensorReadout.getHeartRateData();
            List<? extends GeoLocationData> geoLocationData = sensorReadout.getGeoLocationData();
            List<? extends AcceleratorSensorData> acceleratorSensorData = sensorReadout.getAcceleratorData();
            List<? extends GyroscopeSensorData> gyroscopeSensorData = sensorReadout.getGyroscopeData();

            Log.i(TAG, "No. of events "
                    + heartRateSensorData.size() + " heart, "
                    + geoLocationData.size() + " geo, "
                    + acceleratorSensorData.size() + " acceleration."
                    + gyroscopeSensorData.size() + " geroscope.");
            int len = acceleratorSensorData.size();
            len = Math.min(len, gyroscopeSensorData.size());
            //len = Math.min(len, heartRateSensorData.size());
            try {
                //binary file for display
                FileOutputStream fos = new FileOutputStream(new File(targetDirectory, System.currentTimeMillis() + ".trk"));
                // csv file for inference and training
                fw = new FileWriter(new File(targetDirectory, curentCsvName + ".csv"));
                fos.write(FileItem.HEADER); // header
                fos.write(BitUtility.getBytes(FileItem.VERSION)); // version

                FileItem.writeField(fos, BitUtility.getBytes((short)0x1001), BitUtility.getBytes(sensorReadout.getSportActivityStartTimeRtc()));
                FileItem.writeField(fos, BitUtility.getBytes((short)0x1002), BitUtility.getBytes(sensorReadout.getSportActivityStopTimeRtc()));
                long startTimestampNs = sensorReadout.getSportActivityStartTimeNs();
                FileItem.writeField(fos, BitUtility.getBytes((short)0x1003), BitUtility.getBytes(startTimestampNs));
                long stopTimestampNs = sensorReadout.getSportActivityStopTimeNs();
                FileItem.writeField(fos, BitUtility.getBytes((short)0x1004), BitUtility.getBytes(stopTimestampNs));

                float avgHeartRate = 0;
                int maxHeartRate = 0;
                int countHeartRate = 0;
                for (HeartRateSensorData data : heartRateSensorData) {
                    if (data.getTimestamp() >= startTimestampNs && data.getTimestamp() <= stopTimestampNs && data.getAccuracy() >= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                        avgHeartRate = avgHeartRate * ((float)countHeartRate / (++countHeartRate)) + (float)data.getHeartRate() / countHeartRate;
                        if (data.getHeartRate() > maxHeartRate) {
                            maxHeartRate = data.getHeartRate();
                        }
                    }
                }

                FileItem.writeField(fos, BitUtility.getBytes((short)0x1011), BitUtility.getBytes(avgHeartRate));
                FileItem.writeField(fos, BitUtility.getBytes((short)0x1012), BitUtility.getBytes(maxHeartRate));

                // total ascent und descent, average speed as of the GNSS
                boolean firstAltitude = true;
                double lastAltitude = 0;
                double totalAscent = 0;
                double totalDescent = 0;
                float avgSpeed = 0;
                int countSpeed = 0;
                for (GeoLocationData data : geoLocationData) {
                    if (data.getTimestamp() >= startTimestampNs && data.getTimestamp() <= stopTimestampNs) {
                        Location location = data.getLocation();

                        if (firstAltitude) {
                            firstAltitude = false;
                        } else {
                            double diff = location.getAltitude() - lastAltitude;
                            if (diff > 0) {
                                totalAscent += diff;
                            } else {
                                totalDescent -= diff;
                            }
                        }

                        lastAltitude = location.getAltitude();

                        avgSpeed = avgSpeed * ((float)countSpeed / (++countSpeed)) + location.getSpeed() / countSpeed;
                    }
                }

                FileItem.writeField(fos, BitUtility.getBytes((short)0x1015), BitUtility.getBytes((float)totalAscent));
                FileItem.writeField(fos, BitUtility.getBytes((short)0x1016), BitUtility.getBytes((float)totalDescent));
                FileItem.writeField(fos, BitUtility.getBytes((short)0x1017), BitUtility.getBytes(avgSpeed));
                for (HeartRateSensorData data : heartRateSensorData) {
                    // 2 = data, 0 = n/a, 1 = heart, 1 = first version
                    FileItem.writeField(fos, BitUtility.getBytes((short)0x2011), BitUtility.getBytes(data.getTimestamp()), BitUtility.getBytes(data.getHeartRate()), BitUtility.getBytes(data.getAccuracy()));
                }
                
                // end of file marker
                FileItem.writeField(fos, BitUtility.getBytes((short)0xffff));
                fos.close();
                
                fw.append(FILE_HEADER);
                fw.append(NEW_LINE_SEPARATOR);
                // store individual events
                // TODO: improve classifier with periodical summarized result
                for (int i = 0; i < len; i++) {
                    fw.append(String.valueOf(acceleratorSensorData.get(i).getAcceleration()[0]));
                    fw.append(COMMA_DELIMITER);
                    fw.append(String.valueOf(acceleratorSensorData.get(i).getAcceleration()[1]));
                    fw.append(COMMA_DELIMITER);
                    fw.append(String.valueOf(acceleratorSensorData.get(i).getAcceleration()[2]));
                    fw.append(COMMA_DELIMITER);

                    fw.append(String.valueOf(gyroscopeSensorData.get(i).getGyroscope()[0]));
                    fw.append(COMMA_DELIMITER);
                    fw.append(String.valueOf(gyroscopeSensorData.get(i).getGyroscope()[1]));
                    fw.append(COMMA_DELIMITER);
                    fw.append(String.valueOf(gyroscopeSensorData.get(i).getGyroscope()[2]));
                    fw.append(COMMA_DELIMITER);

                    //fw.append(String.valueOf(heartRateSensorData.get(i).getHeartRate()));
                    //fw.append(COMMA_DELIMITER);

                    tmp = Arrays.copyOf(acceleratorSensorData.get(i).getAcceleration(), 6);
                    System.arraycopy(gyroscopeSensorData.get(i).getGyroscope(), 0, tmp, 3, 3);
                    int result = classifier.predict(tmp, (float) 0.6);
                    fw.append(String.valueOf(result));
                    fw.append(NEW_LINE_SEPARATOR);

                    if (recent_n_result.size()>100){
                        recent_n_result.clear();
                    }
                    recent_n_result.add(result);

                    if(i % 100 == 0 && i>1){
                        int predicted_sport_index = 3;
                        predicted_sport_index = mostFrequent(recent_n_result.toArray());
                        fw.append((char) predicted_sport_index);
                        inferenceResults.add(predicted_sport_index);
                    }
                }
                if (inferenceResults != null && inferenceResults.size()>1)
                    inferenceResult =  mostFrequent(inferenceResults.toArray());
                Log.d(TAG, "overall inference result is " + inferenceResult);
                succeeded+=1;
            } catch (Exception ex) {
                Log.e(TAG, "Failed to write data file " + ex.getClass().getSimpleName() + " " + ex.getMessage());
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        curentCsvName = curentCsvName * succeeded;
        Pair pair = new Pair(inferenceResult,curentCsvName);

        return pair;
    }

    @Override
    protected void onPostExecute(Pair pair) {
        if (this.finishedCallback != null) {
            this.finishedCallback.accept(pair);
        }
    }
    static int mostFrequent(Object[] arr)
    {

        Arrays.sort(arr);

        int max_count = 1, res = (int) arr[0];
        int curr_count = 1;

        for (int i = 1; i < arr.length; i++)
        {
            if (arr[i] == arr[i - 1])
                curr_count++;
            else
            {
                if (curr_count > max_count)
                {
                    max_count = curr_count;
                    res = (int) arr[i - 1];
                }
                curr_count = 1;
            }
        }

        if (curr_count > max_count)
        {
            max_count = curr_count;
            res = (int) arr[arr.length - 1];
        }

        return res;
    }
}
