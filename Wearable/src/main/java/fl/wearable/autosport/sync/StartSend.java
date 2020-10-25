package fl.wearable.autosport.sync;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class StartSend extends AsyncTask<File, Float, Boolean> {
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private BluetoothSocket mmSocket;
    private BluetoothAdapter bluetoothAdapter;

    protected Boolean doInBackground(File[] files) {
        if (files == null || files.length == 0) {
            return false;
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        run(files);

        return null;
    }


    public void run(File[] files) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice mmPhone = null;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                Log.d(TAG, "Device Name: " + deviceName);
                if (deviceName.equals("HUAWEI")) {
                    mmPhone = device;
                    Log.d(TAG, "Find the phone");
                    break;
                }
            }
        }
        BluetoothSocket tmp = null;

        // Get UUID
        UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        try {
            // Use the UUID of the device that discovered // TODO Maybe need extra device object
            if (mmPhone != null) {
                Log.i(TAG, "Device Name: " + mmPhone.getName());
                Log.i(TAG, "Device UUID: " + mmPhone.getUuids()[0].getUuid());
                tmp = mmPhone.createRfcommSocketToServiceRecord(mmPhone.getUuids()[0].getUuid());
                mmSocket = tmp;
            } else Log.d(TAG, "Device is null.");
        } catch (NullPointerException e) {
            Log.d(TAG, " UUID from device is null, Using Default UUID, Device name: " + mmPhone.getName());
            try {
                tmp = mmPhone.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
        }


        // Get stream
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = mmSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        // Connect to the remote device through the socket. This call blocks
        // until it succeeds or throws an exception.
        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }
        int succeeded = 0;
        int totalRequests = files.length;
        int currentRequest = 0;
        float oneRequestValue = 1f / totalRequests;
        for (File file : files) {
            Log.d(TAG, "filename is " + file.getName());
            try {
                InputStream is = new FileInputStream(file);
                int fileSize = is.available();
                int read;
                long totalData = 0;
                byte[] buffer = new byte[1024 * 1024];
                while ((read = is.read(buffer)) > 0) {
                    mmOutStream.write(buffer, 0, read);
                    totalData += read;
                    publishProgress(currentRequest * oneRequestValue + ((fileSize > 0) ? oneRequestValue * totalData / fileSize : 0));
                    Log.d(TAG, "progress: " + currentRequest * oneRequestValue + ((fileSize > 0) ? oneRequestValue * totalData / fileSize : 0));
                }
                is.close();
                publishProgress((currentRequest + 1) * oneRequestValue);
                succeeded++;
            } catch (Exception ex) {
                Log.d(TAG, "Failed to upload file " + file.getAbsolutePath() + " to  paired phone due to " + ex.getClass().getSimpleName() + " " + ex.getMessage());
            }
        }

        try {
            mmOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onProgressUpdate(Float... values) {
        for (Float value : values) {
            Log.d(TAG, "Progress(" + values.length + ") " + value);
        }
    }

    @Override
    protected void onPostExecute(Boolean bool) {
        Log.d(TAG, "Activity data uploaded.");
    }

}
