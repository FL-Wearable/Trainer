package fl.wearable.autosport.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
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
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import fl.wearable.autosport.R
import fl.wearable.autosport.databinding.ActivityLoginBinding
import fl.wearable.autosport.federated.ui.main.MainActivity
import fl.wearable.autosport.sync.UpdateModel
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
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
    private val MODEL_KEY: String = "checkpoint"
    private val MODEL_PATH: String = "/model"
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    private var mGeneratorExecutor: ScheduledExecutorService? = null
    private var mDataItemGeneratorFuture: ScheduledFuture<*>? = null
    private var mDataItemListAdapter: DataItemAdapter? = null
    private val mDataItemList: ListView? = null


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


        binding.button.setOnClickListener {
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

        binding.startWearable.setOnClickListener {
            Log.d(TAG, "Generating RPC")

            StartWearableActivityTask().execute()
        }


        binding.send.setOnClickListener {
            val dir = File(filesDir, "model")
            if (!dir.exists()) {
                Log.d(TAG, "dir doesn't exist, create it now")
                dir.mkdir()
            }
            val modelFile = File(dir, "model.pt")
            if (!modelFile.exists()) {
                Log.d(TAG, "no model")
            }
            toAsset(modelFile)?.let { it1 -> sendPhoto(it1) }

            val modelfile = File("${this.filesDir}/models", "model.pb")
            val updateModel = object : UpdateModel() {
                override fun onPostExecute(bool: Boolean?) {
                    super.onPostExecute(bool)
                }
            }
            updateModel.execute(modelfile)
        }

        binding.url.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    binding.button.performClick()
                    true
                }
                else -> false
            }
        }

        // Stores DataItems received by the local broadcaster or from the paired watch.
        mDataItemListAdapter = DataItemAdapter(this, android.R.layout.simple_list_item_1)
        mDataItemList?.adapter = mDataItemListAdapter
        mGeneratorExecutor = ScheduledThreadPoolExecutor(1)
    }

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
        Log.d(TAG, "onDataChanged: $dataEvents")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                mDataItemListAdapter!!.add(
                    Event("DataItem Changed", event.dataItem.toString())
                )
            } else if (event.type == DataEvent.TYPE_DELETED) {
                mDataItemListAdapter!!.add(
                    Event("DataItem Deleted", event.dataItem.toString())
                )
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(
            TAG, "onMessageReceived() A message from watch was received:"
                 + messageEvent.requestId
                 + " "
                 + messageEvent.path
        )
        mDataItemListAdapter!!.add(
            Event("Message from watch", messageEvent.toString())
        )
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG,"onCapabilityChanged: $capabilityInfo")
        mDataItemListAdapter!!.add(
            Event("onCapabilityChanged", capabilityInfo.toString())
        )
    }

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

    private fun sendPhoto(asset: Asset) {
        val dataMap =
                PutDataMapRequest.create(MODEL_PATH)
        dataMap.dataMap.putAsset(MODEL_KEY, asset)
        dataMap.dataMap.putLong("time", Date().time)
        val request = dataMap.asPutDataRequest()
        request.setUrgent()
        val dataItemTask = Wearable.getDataClient(this).putDataItem(request)
        dataItemTask.addOnSuccessListener { dataItem ->
            Log.d(TAG, "Sending image was successful: $dataItem")
        }
    }

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

    /** A View Adapter for presenting the Event objects in a list  */
    private class DataItemAdapter(private val mContext: Context, unusedResource: Int) :
        ArrayAdapter<Event?>(mContext, unusedResource) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val holder: ViewHolder
            if (convertView == null) {
                holder = ViewHolder()
                val inflater = mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null)
                convertView.tag = holder
                holder.text1 = convertView.findViewById<View>(android.R.id.text1) as TextView
                holder.text2 = convertView.findViewById<View>(android.R.id.text2) as TextView
            } else {
                holder = convertView.tag as ViewHolder
            }
            val event: Event? = getItem(position)
            holder.text1!!.setText(event!!.title)
            holder.text2!!.setText(event!!.text)
            return convertView!!
        }

        private inner class ViewHolder {
            var text1: TextView? = null
            var text2: TextView? = null
        }
    }

    private class Event(var title: String, var text: String)

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


}