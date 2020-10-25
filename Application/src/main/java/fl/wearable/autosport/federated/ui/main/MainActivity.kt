package fl.wearable.autosport.federated.ui.main

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_main.chart
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_main.toolbar
import fl.wearable.autosport.R
import fl.wearable.autosport.databinding.ActivityMainBinding
import fl.wearable.autosport.federated.datasource.LocalAllinoneDataSource
import fl.wearable.autosport.federated.domain.SportDataRepository
import fl.wearable.autosport.federated.service.WorkerRepository
import fl.wearable.autosport.federated.ui.ContentState
import fl.wearable.autosport.federated.ui.ProcessData
import fl.wearable.autosport.domain.SyftConfiguration

const val AUTH_TOKEN = "authToken"
const val BASE_URL = "baseUrl"
private const val TAG = "MainActivity"

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        setSupportActionBar(toolbar)

        this.viewModel = initiateViewModel(
            intent.getStringExtra("baseURL"),
            intent.getStringExtra("authToken")
        )
        binding.viewModel = this.viewModel

        viewModel.getRunningWorkInfo()?.observe(this, viewModel.getWorkInfoObserver())

        binding.buttonFirst.setOnClickListener { launchForegroundCycle() }
        binding.buttonSecond.setOnClickListener { launchBackgroundCycle() }

        viewModel.processState.observe(
            this,
            Observer { onProcessStateChanged(it) }
        )

        viewModel.processData.observe(
            this,
            Observer { onProcessData(it) }
        )

        viewModel.steps.observe(
            this,
            Observer { binding.step.text = it })
    }


    private fun launchBackgroundCycle() {
        viewModel.submitJob().observe(this, viewModel.getWorkInfoObserver())
    }

    private fun launchForegroundCycle() {
        val config = SyftConfiguration.builder(this, viewModel.baseUrl)
                .setMessagingClient(SyftConfiguration.NetworkingClients.HTTP)
                .setCacheTimeout(0L)
                .build()
        val LocalAllinoneDataSource = LocalAllinoneDataSource(resources)
        val dataRepository = SportDataRepository(LocalAllinoneDataSource)
        viewModel.launchForegroundTrainer(config, dataRepository)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.disposeTraining()
        finish()
    }

    private fun onProcessData(it: ProcessData?) {
        processData(
            it ?: ProcessData(
                emptyList()
            )
        )
    }

    private fun onProcessStateChanged(contentState: ContentState?) {
        when (contentState) {
            ContentState.Training -> {
                progressBar.visibility = ProgressBar.GONE
                binding.chartHolder.visibility = View.VISIBLE
            }
            ContentState.Loading -> {
                progressBar.visibility = ProgressBar.VISIBLE
                binding.chartHolder.visibility = View.GONE
            }
        }
    }

    private fun processData(processState: ProcessData) {
        val entries = mutableListOf<Entry>()
        processState.data.forEachIndexed { index, value ->
            entries.add(Entry(index.toFloat(), value))
        }
        val dataSet = LineDataSet(entries, "loss")
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.setMaxVisibleValueCount(0)
        chart.setNoDataText("Waiting for data")
        chart.invalidate()
    }

    private fun initiateViewModel(baseUrl: String?, authToken: String?): MainActivityViewModel {
        if (baseUrl == null || authToken == null)
            throw IllegalArgumentException("Sport trainer called without proper arguments")
        return ViewModelProvider(
            this,
            MainActivityViewModelFactory(
                baseUrl,
                authToken,
                WorkerRepository(this)
            )
        ).get(MainActivityViewModel::class.java)
    }
}
