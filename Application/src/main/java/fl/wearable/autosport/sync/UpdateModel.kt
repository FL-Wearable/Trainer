package fl.wearable.autosport.sync

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.os.AsyncTask
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

open class UpdateModel() : AsyncTask<File, Float, Boolean>() {
    private var mmInStream: InputStream? = null
    private var mmOutStream: OutputStream? = null
    private var mmSocket: BluetoothSocket? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun doInBackground(vararg p0: File): Boolean {
        if (p0 == null) {
            return false
        }
        var file: File = p0[0]
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        run(file)
        return true
    }


    fun run(file: File) {
        val pairedDevices = bluetoothAdapter!!.bondedDevices
        var mmPhone: BluetoothDevice? = null
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                mmPhone = device
                //String deviceName = device.getName();
                //String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
        var tmp: BluetoothSocket? = null

        // Get UUID
        val DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        try {
            // Use the UUID of the device that discovered // TODO Maybe need extra device object
            if (mmPhone != null) {
                Log.i(ContentValues.TAG, "Device Name: " + mmPhone.name)
                Log.i(ContentValues.TAG, "Device UUID: " + mmPhone.uuids[0].uuid)
                tmp = mmPhone.createRfcommSocketToServiceRecord(mmPhone.uuids[0].uuid)
            } else Log.d(ContentValues.TAG, "Device is null.")
        } catch (e: NullPointerException) {
            Log.d(
                ContentValues.TAG,
                " UUID from device is null, Using Default UUID, Device name: " + mmPhone!!.name
            )
            try {
                tmp = mmPhone.createRfcommSocketToServiceRecord(DEFAULT_UUID)
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
        } catch (e: IOException) {
        }
        mmSocket = tmp

        // Get stream
        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null
        try {
            tmpIn = mmSocket!!.inputStream
        } catch (e: IOException) {
            Log.e(ContentValues.TAG, "Error occurred when creating input stream", e)
        }
        try {
            tmpOut = mmSocket!!.outputStream
        } catch (e: IOException) {
            Log.e(ContentValues.TAG, "Error occurred when creating output stream", e)
        }
        mmInStream = tmpIn
        mmOutStream = tmpOut

        // Connect to the remote device through the socket. This call blocks
        // until it succeeds or throws an exception.
        try {
            mmSocket!!.connect()
        } catch (connectException: IOException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket!!.close()
            } catch (closeException: IOException) {
                Log.e(ContentValues.TAG, "Could not close the client socket", closeException)
            }
            return
        }
        var succeeded = 0
        val totalRequests = file.totalSpace
        val currentRequest = 0
        val oneRequestValue = 1f / totalRequests

        try {
            val `is`: InputStream = FileInputStream(file)
            val fileSize = `is`.available()
            var read: Int
            var totalData: Long = 0
            val buffer = ByteArray(1024 * 1024)
            while (`is`.read(buffer).also { read = it } > 0) {
                mmOutStream!!.write(buffer, 0, read)
                totalData += read.toLong()
            }
            `is`.close()
            publishProgress((currentRequest + 1) * oneRequestValue)
            succeeded++
        } catch (ex: Exception) {
            Log.d(
                ContentValues.TAG,
                "Failed to upload file " + file.name + " to  paired phone due to " + ex.javaClass.simpleName + " " + ex.message
            )
        }

        try {
            mmOutStream!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            mmSocket!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    protected fun onProgressUpdate(vararg values: Float) {
        for (value in values) {
            Log.d(ContentValues.TAG, "Progress(" + values.size + ") " + value)
        }
    }

    override fun onPostExecute(bool: Boolean?) {
        Log.d(ContentValues.TAG, "Activity data uploaded.")
    }

}