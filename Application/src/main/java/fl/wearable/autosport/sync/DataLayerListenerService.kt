package fl.wearable.autosport.sync

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import fl.wearable.autosport.login.LoginActivity

/** Listens to DataItems and Messages from the local node.  */
class DataLayerListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(
            TAG,
            "onDataChanged: $dataEvents"
        )

        // Loop through the events and send a message back to the node that created the data item.
        for (event in dataEvents) {
            val uri = event.dataItem.uri
            val path = uri.path
            if (COUNT_PATH == path) {
                // Get the node id of the node that created the data item from the host portion of
                // the uri.
                val nodeId = uri.host
                // Set the data of the message to be the bytes of the Uri.
                val payload = uri.toString().toByteArray()

                // Send the rpc
                // Instantiates clients without member variables, as clients are inexpensive to
                // create. (They are cached and shared between GoogleApi instances.)
                val sendMessageTask = Wearable.getMessageClient(this)
                        .sendMessage(nodeId!!, DATA_ITEM_RECEIVED_PATH, payload)
                sendMessageTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(
                            TAG,
                            "Message sent successfully"
                        )
                    } else {
                        Log.d(
                            TAG,
                            "Message failed."
                        )
                    }
                }
            }
        }
    }

    @ExperimentalStdlibApi
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(
            TAG,
            "onMessageReceived: $messageEvent"
        )

        // Check to see if the message is to start an activity
        if (messageEvent.path == START_ACTIVITY_PATH) {
            val startIntent = Intent(this, LoginActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(startIntent)
        }
    }

    companion object {
        private const val TAG = "DataLayerService"
        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val DATA_ITEM_RECEIVED_PATH = "/data-item-received"
        const val COUNT_PATH = "/count"
        const val FILE_PATH = "/sync"
        const val DATA_KEY = "datafile"
    }
}