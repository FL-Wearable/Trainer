package fl.wearable.autosport.unit.execution

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import fl.wearable.autosport.execution.JobStatusMessage
import fl.wearable.autosport.execution.JobStatusSubscriber
import fl.wearable.autosport.execution.Plan
import fl.wearable.autosport.networking.datamodels.ClientConfig
import fl.wearable.autosport.proto.SyftModel
import java.util.concurrent.ConcurrentHashMap

@ExperimentalUnsignedTypes
internal class JobStatusSubscriberTest {
    private val subscriber = spy<JobStatusSubscriber>()

    @Test
    fun `given a job ready message verify status subscriber calls onReady`() {
        val model = mock<SyftModel>()
        val clientConfig = mock<ClientConfig>()
        val plans = ConcurrentHashMap<String, Plan>()
        val msg = JobStatusMessage.JobReady(model, plans, clientConfig)
        subscriber.onJobStatusMessage(msg)
        verify(subscriber).onReady(model, plans, clientConfig)
    }

    @Test
    fun `given a job cycle rejected message verify status subscriber calls onRejected`() {
        val timeout = "timeout string"
        val msg = JobStatusMessage.JobCycleRejected(timeout)
        subscriber.onJobStatusMessage(msg)
        verify(subscriber).onRejected(timeout)
    }
}