package org.openmined.syft.demo.federated.datasource

import android.content.res.Resources
import org.openmined.syft.demo.R
import org.openmined.syft.demo.federated.domain.Batch
import java.io.BufferedReader
import java.io.InputStreamReader

private var DATASIZE: Int? = null
private var LABELSIZE: Int? = null

class LocalAllinoneDataSource constructor(
    private val resources: Resources
) {
    private var dataReader = returnDataReader()
    var numLabel = 1

    fun loadDataBatch(batchSize: Int): Pair<Batch, Batch> {
        val trainInput = arrayListOf<List<Float>>()
        val labels = arrayListOf<List<Float>>()
        for (idx in 0..batchSize)
            readSample(trainInput, labels)

        DATASIZE = trainInput[0].size
        LABELSIZE = labels[0].size

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
        dataReader.close()
        dataReader = returnDataReader()
    }


    private fun returnDataReader() = BufferedReader(
        InputStreamReader(
            resources.openRawResource(R.raw.p2_shuf)
        )
    )
}