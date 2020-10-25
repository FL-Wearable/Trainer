package fl.wearable.autosport.integration

import android.net.NetworkCapabilities
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import fl.wearable.autosport.Syft
import fl.wearable.autosport.common.AbstractSyftWorkerTest
import fl.wearable.autosport.domain.SyftConfiguration
import fl.wearable.autosport.execution.JobErrorThrowable
import fl.wearable.autosport.execution.JobStatusSubscriber
import fl.wearable.autosport.integration.clients.HttpClientMock
import fl.wearable.autosport.integration.clients.SocketClientMock
import fl.wearable.autosport.integration.execution.ShadowPlan
import org.robolectric.annotation.Config

@ExperimentalUnsignedTypes
class AuthenticationTest : AbstractSyftWorkerTest() {

    @Test
    @Config(shadows = [ShadowPlan::class])
    fun `Test success workflow`() {
        val socketClient = SocketClientMock(
            authenticateSuccess = true,
            cycleSuccess = true
        )
        val httpClient = HttpClientMock(
            pingSuccess = true, downloadSpeedSuccess = true,
            uploadSuccess = true, downloadPlanSuccess = true, downloadModelSuccess = true
        )

        val syftConfiguration = SyftConfiguration(
            context,
            networkingSchedulers,
            computeSchedulers,
            context.filesDir,
            true,
            batteryCheckEnabled = true,
            networkConstraints = networkConstraints,
            transportMedium = NetworkCapabilities.TRANSPORT_WIFI,
            cacheTimeOut = 0,
            maxConcurrentJobs = 1,
            socketClient = socketClient.getMockedClient(),
            httpClient = httpClient.getMockedClient(),
            messagingClient = SyftConfiguration.NetworkingClients.SOCKET
        )

        val syftWorker = Syft.getInstance(syftConfiguration, "test token")
        val job = syftWorker.newJob("test", "1")
        val jobStatusSubscriber = spy<JobStatusSubscriber>()
        job.start(jobStatusSubscriber)
        verify(jobStatusSubscriber).onReady(any(), any(), any())
        syftWorker.dispose()
        verify(jobStatusSubscriber).onComplete()
    }

    @Test
    @Config(shadows = [ShadowPlan::class])
    fun `Test workflow with no auth token`() {
        val socketClient = SocketClientMock(
            authenticateSuccess = true,
            cycleSuccess = true
        )
        val httpClient = HttpClientMock(
            pingSuccess = true, downloadSpeedSuccess = true,
            uploadSuccess = true, downloadPlanSuccess = true, downloadModelSuccess = true
        )

        val syftConfiguration = SyftConfiguration(
            context,
            networkingSchedulers,
            computeSchedulers,
            context.filesDir,
            true,
            batteryCheckEnabled = true,
            networkConstraints = networkConstraints,
            transportMedium = NetworkCapabilities.TRANSPORT_WIFI,
            cacheTimeOut = 0,
            maxConcurrentJobs = 1,
            socketClient = socketClient.getMockedClient(),
            httpClient = httpClient.getMockedClient(),
            messagingClient = SyftConfiguration.NetworkingClients.SOCKET
        )

        val syftWorker = Syft.getInstance(syftConfiguration)
        val job = syftWorker.newJob("test", "1")
        val jobStatusSubscriber = spy<JobStatusSubscriber>()
        job.start(jobStatusSubscriber)
        verify(jobStatusSubscriber).onReady(any(), any(), any())
        syftWorker.dispose()
        verify(jobStatusSubscriber).onComplete()
    }

    @Test
    @Config(shadows = [ShadowPlan::class])
    fun `Test workflow with authentication failure`() {
        val socketClient = SocketClientMock(
            authenticateSuccess = false,
            cycleSuccess = true
        )
        val httpClient = HttpClientMock(
            pingSuccess = true, downloadSpeedSuccess = true,
            uploadSuccess = true, downloadPlanSuccess = true, downloadModelSuccess = true
        )

        val syftConfiguration = SyftConfiguration(
            context,
            networkingSchedulers,
            computeSchedulers,
            context.filesDir,
            true,
            batteryCheckEnabled = true,
            networkConstraints = networkConstraints,
            transportMedium = NetworkCapabilities.TRANSPORT_WIFI,
            cacheTimeOut = 0,
            maxConcurrentJobs = 1,
            socketClient = socketClient.getMockedClient(),
            httpClient = httpClient.getMockedClient(),
            messagingClient = SyftConfiguration.NetworkingClients.SOCKET
        )

        val syftWorker = Syft.getInstance(syftConfiguration)
        val job = syftWorker.newJob("test", "1")
        val jobStatusSubscriber = spy<JobStatusSubscriber>()
        job.start(jobStatusSubscriber)
        argumentCaptor<Throwable>().apply {
            verify(jobStatusSubscriber).onError(capture())
            assert(firstValue is JobErrorThrowable.AuthenticationFailure)
        }
        syftWorker.dispose()
        verify(jobStatusSubscriber, never()).onComplete()
    }

    @Test
    @Config(shadows = [ShadowPlan::class])
    fun `Test workflow with cycle rejected`() {
        val socketClient = SocketClientMock(
            authenticateSuccess = true,
            cycleSuccess = false
        )
        val httpClient = HttpClientMock(
            pingSuccess = true, downloadSpeedSuccess = true,
            uploadSuccess = true, downloadPlanSuccess = true, downloadModelSuccess = true
        )
        val syftConfiguration = SyftConfiguration(
            context,
            networkingSchedulers,
            computeSchedulers,
            context.filesDir,
            true,
            batteryCheckEnabled = true,
            networkConstraints = networkConstraints,
            transportMedium = NetworkCapabilities.TRANSPORT_WIFI,
            cacheTimeOut = 0,
            maxConcurrentJobs = 1,
            socketClient = socketClient.getMockedClient(),
            httpClient = httpClient.getMockedClient(),
            messagingClient = SyftConfiguration.NetworkingClients.SOCKET
        )

        val syftWorker = Syft.getInstance(syftConfiguration)
        val job = syftWorker.newJob("test", "1")
        val jobStatusSubscriber = spy<JobStatusSubscriber>()
        job.start(jobStatusSubscriber)
        verify(jobStatusSubscriber).onRejected(any())
        syftWorker.dispose()
        verify(jobStatusSubscriber).onComplete()
    }

}