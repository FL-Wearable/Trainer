package fl.wearable.autosport.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import fl.wearable.autosport.R
import fl.wearable.autosport.databinding.ActivityLoginBinding
import fl.wearable.autosport.federated.ui.main.MainActivity
import fl.wearable.autosport.sync.DataLayerListenerService
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import java.util.HashSet
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val TAG = "LoginActivity"

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class LoginActivity : AppCompatActivity(),
                        DataClient.OnDataChangedListener,
                        MessageClient.OnMessageReceivedListener,
                        CapabilityClient.OnCapabilityChangedListener {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    // for transmissions
    private val MODEL_KEY: String = "checkpoint"
    private val FILE_PATH: String = "/sync"
    private var mGeneratorExecutor: ScheduledExecutorService? = null
    private var mDataItemGeneratorFuture: ScheduledFuture<*>? = null


    init {
        instance = this
    }

    companion object {
        private var instance: LoginActivity? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        loginViewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        ).get(LoginViewModel::class.java)
        val dir = File(filesDir, "sync")
        if (!dir.exists()) {
            Log.d(TAG, "dir doesn't exist, create it now")
            dir.mkdir()
        }
        binding.button.setOnClickListener {
            val dataFile = File(dir, "data.csv")
            if (!dataFile.exists()) {
                Log.d(TAG, "no data to train")
                Toast.makeText(
                    applicationContext,
                    "No data to train, please sync with wearable app",
                    Toast.LENGTH_SHORT
                ).show()

            }
            else {
                val baseUrl = binding.url.text.toString()
                val valid = loginViewModel.checkUrl(baseUrl)
                if (valid) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("baseURL", baseUrl)
                    intent.putExtra("authToken", loginViewModel.getAuthToken())
                    startActivity(intent)
                } else {
                    binding.error.text = getString(R.string.error_url)
                    binding.error.visibility = TextView.VISIBLE
                }
            }
        }

        // ------ send ------------
        binding.startWearable.setOnClickListener {
            Log.d(TAG, "Generating RPC")

            StartWearableActivityTask().execute()
        }

        // for transmissions
        binding.send.setOnClickListener {
            val dir = File(filesDir, "sync")
            if (!dir.exists()) {
                Log.d(TAG, "dir doesn't exist, create it now")
                dir.mkdir()
            }
            val modelFile = File(dir, "model.pb")
            if (!modelFile.exists()) {
                Log.d(TAG, "no model")
            }
            toAsset(modelFile)?.let { it1 -> sendModel(it1) }
        }

        mGeneratorExecutor = ScheduledThreadPoolExecutor(1)

        // ------ send ------------

        binding.url.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    binding.button.performClick()
                    true
                }
                else -> false
            }
        }


    }

    // ------ send/rec ------------
    override fun onResume() {
        super.onResume()
        mDataItemGeneratorFuture =
                mGeneratorExecutor!!.scheduleWithFixedDelay(
                    DataItemGenerator(), 1, 5, TimeUnit.SECONDS
                )
        Wearable.getDataClient(this).addListener(this)
        Wearable.getMessageClient(this).addListener(this)
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
    }

    override fun onPause() {
        super.onPause()
        mDataItemGeneratorFuture!!.cancel(true /* mayInterruptIfRunning */)
        Wearable.getDataClient(this).removeListener(this)
        Wearable.getMessageClient(this).removeListener(this)
        Wearable.getCapabilityClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // ------ send ------------
        Log.d(TAG, "onDataChanged: $dataEvents")
        // ------ send ------------

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val key = DataMapItem.fromDataItem(event.dataItem).dataMap.containsKey(
                    DataLayerListenerService.DATA_KEY
                )
                if (DataLayerListenerService.FILE_PATH.equals(path) && key) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val photoAsset =
                            dataMapItem.dataMap.getAsset(DataLayerListenerService.DATA_KEY)
                    // Loads image on background thread.
                    LoadBitmapAsyncTask().execute(photoAsset)
                } else if (event.dataItem.assets.containsKey(MODEL_KEY)) {
                    Log.d(TAG, "It's the model")
                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
                    Log.d(TAG, "Data Changed for COUNT_PATH")
                } else {
                    Log.d(TAG, "Unrecognized path: $path")
                }
            }
        }

    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // ------ send ------------
        Log.d(
            TAG, "onMessageReceived() A message from watch was received:"
                 + messageEvent
        )
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged:" + capabilityInfo);
    }

    // ------ send ------------
    // for transmissions
    private fun toAsset(file: File): Asset? {
        var `is`: FileInputStream? = null
        val bFile = ByteArray(file.length().toInt())
        try {
            `is` = FileInputStream(file)
            `is`.read(bFile)
            `is`.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Asset.createFromBytes(bFile)
    }

    // for transmissions
    private fun sendModel(asset: Asset) {
        val dataMap =
                PutDataMapRequest.create(FILE_PATH)
        dataMap.dataMap.putAsset(MODEL_KEY, asset)
        dataMap.dataMap.putLong("time", Date().time)
        val request = dataMap.asPutDataRequest()
        request.setUrgent()
        val dataItemTask = Wearable.getDataClient(this).putDataItem(request)
        dataItemTask.addOnSuccessListener { dataItem ->
            Log.d(TAG, "Sending model was successful: $dataItem")
        }
    }

    // for transmissions
    private class DataItemGenerator : Runnable {
        private val COUNT_KEY: String = "count"
        private val COUNT_PATH: String = "/count"
        private var count = 0
        override fun run() {
            val putDataMapRequest = PutDataMapRequest.create(COUNT_PATH)
            putDataMapRequest.dataMap.putInt(COUNT_KEY, count++)
            val request = putDataMapRequest.asPutDataRequest()
            request.setUrgent()
            Log.d(TAG, "Generating DataItem: $request")
            val dataItemTask = Wearable.getDataClient(applicationContext()).putDataItem(request)
            try {
                val dataItem = Tasks.await(dataItemTask)
                Log.d(TAG, "DataItem saved: $dataItem")
            } catch (exception: ExecutionException) {
                Log.e(TAG, "Task failed: $exception")
            } catch (exception: InterruptedException) {
                Log.e(TAG, "Interrupt occurred: $exception")
            }
        }
    }

    private class StartWearableActivityTask : AsyncTask<Void?, Void?, Void?>() {
        private val START_ACTIVITY_PATH: String ="/start-activity"
        override fun doInBackground(vararg p0: Void?): Void? {
            val nodes: Collection<String> = getNodes()!!
            for (node in nodes) {
                sendStartActivityMessage(node)
            }
            return null
        }

        @WorkerThread
        private fun getNodes(): Collection<String>? {
            val results = HashSet<String>()
            val nodeListTask = Wearable.getNodeClient(applicationContext()).connectedNodes
            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                val nodes = Tasks.await(nodeListTask)
                for (node in nodes) {
                    results.add(node.id)
                }
            } catch (exception: ExecutionException) {
                Log.e(TAG, "Task failed: $exception")
            } catch (exception: InterruptedException) {
                Log.e(TAG, "Interrupt occurred: $exception")
            }
            return results
        }

        @WorkerThread
        private fun sendStartActivityMessage(node: String) {
            val sendMessageTask = instance?.let { Wearable.getMessageClient(it).sendMessage(
                node, START_ACTIVITY_PATH, ByteArray(0)
            ) }
            try {
                val result = sendMessageTask?.let { Tasks.await(it) }
                Log.d(TAG, "Message sent: $result")
            } catch (exception: ExecutionException) {
                Log.e(TAG, "Task failed: $exception")
            } catch (exception: InterruptedException) {
                Log.e(TAG, "Interrupt occurred: $exception")
            }
        }
    }

    // ------ rec ------------
    private class LoadBitmapAsyncTask :
        AsyncTask<Asset?, Void?, Boolean?>() {
         override fun doInBackground(vararg params: Asset?): Boolean? {
                 return if (params.size > 0) {
                val asset = params[0]
                val getFdForAssetResponseTask =
                        asset?.let { Wearable.getDataClient(applicationContext()).getFdForAsset(it) }
                try {
                    val getFdForAssetResponse = getFdForAssetResponseTask?.let { Tasks.await(it) }
                    val assetInputStream = getFdForAssetResponse?.inputStream
                    if (assetInputStream != null) {
                        val dir = File(applicationContext().filesDir, "sync")
                        if (!dir.exists()) {
                            dir.mkdir()
                        }
                        val dataFile = File(dir, "data.cav")
                        try {
                            FileOutputStream(dataFile, true).use { fos ->
                                val buffer = ByteArray(8 * 1024)
                                var read: Int
                                while (assetInputStream.read(buffer).also { read = it } != -1) {
                                    fos.write(buffer, 0, read)
                                }
                                fos.flush()
                                Log.d(TAG, "file saved")
                                return true
                            }
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } finally {
                            try {
                                assetInputStream.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                        false
                    } else {
                        Log.w(TAG, "Requested an unknown Asset.")
                        null
                    }
                } catch (exception: ExecutionException) {
                    Log.e(TAG, "Failed retrieving asset, Task failed: $exception")
                    null
                } catch (exception: InterruptedException) {
                    Log.e(TAG, "Failed retrieving asset, interrupt occurred: $exception")
                    null
                }
            } else {
                Log.e(TAG, "Asset must be non-null")
                null
            }
        }

        override fun onPostExecute(b: Boolean?) {
            if (b!!) {
                Log.d(TAG, "received file.")
            }
        }
    }



}