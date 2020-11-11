package fl.wearable.autosport.federated.datasource

import android.content.ContentValues.TAG
import android.content.res.Resources
import android.nfc.Tag
import fl.wearable.autosport.federated.domain.Batch
import fl.wearable.autosport.login.LoginActivity.Companion.applicationContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import android.util.Log
import fl.wearable.autosport.R

private var DATASIZE: Int? = null
private var LABELSIZE: Int? = null

@ExperimentalStdlibApi
class LocalAllinoneDataSource constructor(
    private val resources: Resources )
{
    private var dataReader = returnDataReader()
    var numLabel = 1

    fun loadDataBatch(batchSize: Int): Pair<Batch, Batch> {
        val trainInput = arrayListOf<List<Float>>()
        val labels = arrayListOf<List<Float>>()
        for (idx in 0..batchSize)
            readSample(trainInput, labels)

        DATASIZE = trainInput[0].size
        LABELSIZE = labels[0].size
        Log.d(TAG, "datasize is " + DATASIZE)
        val trainingData = Batch(
            trainInput.flatten().toFloatArray(),
            longArrayOf(trainInput.size.toLong(), DATASIZE!!.toLong())
        )
        val trainingLabel = Batch(
            labels.flatten().toFloatArray(),
            longArrayOf(labels.size.toLong(), LABELSIZE!!.toLong())
        )
        return Pair(trainingData, trainingLabel)
    }

    private fun readSample(
        trainInput: ArrayList<List<Float>>,
        labels: ArrayList<List<Float>>
    ) {
        val sample = readLine()

        trainInput.add(
            sample.drop(numLabel).map { it.trim().toFloat() }
        )
        labels.add(
            sample.take(numLabel).map { it.trim().toFloat() }
        )
    }

    private fun readLine(): List<String> {
        var x = dataReader.readLine()?.split(",")
        if (x == null) {
            restartReader()
            x = dataReader.readLine()?.split(",")
        }
        if (x == null)
            throw Exception("cannot read from dataset file")
        return x
    }

    private fun restartReader() {
        //dataReader.close()
        dataReader = returnDataReader()
    }


    private fun returnDataReader() = BufferedReader(
        InputStreamReader(
            //resources.openRawResource(R.raw.smalldata)
            readFileToStream(applicationContext().filesDir, "sync", "data.csv")
        )
    )
    private fun readFileToStream(dir: File, path: String, fileName: String): FileInputStream {
        val dir = File(dir, path)
        val dataFile = File(dir, fileName)
        return dataFile.inputStream()

    }
}