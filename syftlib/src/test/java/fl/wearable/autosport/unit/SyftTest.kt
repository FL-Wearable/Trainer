package fl.wearable.autosport.unit

import android.net.NetworkCapabilities
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import fl.wearable.autosport.Syft
import fl.wearable.autosport.domain.SyftConfiguration
import fl.wearable.autosport.execution.SyftJob
import fl.wearable.autosport.monitor.DeviceMonitor
import fl.wearable.autosport.networking.clients.HttpClient
import fl.wearable.autosport.networking.clients.SocketClient
import fl.wearable.autosport.networking.datamodels.syft.AuthenticationRequest
import fl.wearable.autosport.networking.datamodels.syft.AuthenticationResponse
import fl.wearable.autosport.threading.ProcessSchedulers

internal class SyftTest {

    @Test
    @ExperimentalUnsignedTypes
    fun `Given a syft object when requestCycle is invoked then socket client calls authenticate api`() {
        val socketClient = mock<SocketClient> {
            on {
                authenticate(
                    AuthenticationRequest(
                        "auth token",
                        "model name",
                        "1.0.0"
                    )
                )
            }.thenReturn(
                Single.just(
                    AuthenticationResponse.AuthenticationSuccess(
                        "test id",
                        true
                    )
                )
            )
        }
        val httpClient = mock<HttpClient>() {
            on { apiClient } doReturn mock()
        }
        val schedulers = object : ProcessSchedulers {
            override val computeThreadScheduler: Scheduler
                get() = Schedulers.io()
            override val calleeThreadScheduler: Scheduler
                get() = AndroidSchedulers.mainThread()
        }

        val deviceMonitor = mock<DeviceMonitor> {
            on { isActivityStateValid() }.thenReturn(true)
            on { isNetworkStateValid() }.thenReturn(true)
            on { isBatteryStateValid() }.thenReturn(true)
        }

        val config = SyftConfiguration(
            mock(),
            schedulers,
            schedulers,
            mock(),
            true,
            batteryCheckEnabled = true,
            networkConstraints = listOf(),
            transportMedium = NetworkCapabilities.TRANSPORT_WIFI,
            cacheTimeOut = 0L,
            maxConcurrentJobs = 1,
            socketClient = socketClient,
            httpClient = httpClient,
            messagingClient = SyftConfiguration.NetworkingClients.SOCKET
        )
        val workerTest = spy(
            Syft(config, deviceMonitor, "auth token")
        )
        val syftJob = SyftJob.create(
            "model name",
            "1.0.0",
            workerTest,
            config
        )

        workerTest.executeCycleRequest(syftJob)
        verify(socketClient).authenticate(
            AuthenticationRequest(
                "auth token",
                "model name",
                "1.0.0"
            )
        )
    }

    @Test
    @ExperimentalUnsignedTypes
    fun `Given a syft object when requestCycle is invoked and speed test is not enabled then network status returns an empty result`() {
        val workerId = "test id"
        val socketClient = mock<SocketClient> {
            on {
                authenticate(
                    AuthenticationRequest(
                        "auth token",
                        "model name",
                        "1.0.0"
                    )
                )
            }.thenReturn(
                Single.just(
                    AuthenticationResponse.AuthenticationSuccess(
                        workerId,
                        false
                    )
                )
            )
        }
        val httpClient = mock<HttpClient>() {
            on { apiClient } doReturn mock()
        }
        val schedulers = object : ProcessSchedulers {
            override val computeThreadScheduler: Scheduler
                get() = Schedulers.io()
            override val calleeThreadScheduler: Scheduler
                get() = AndroidSchedulers.mainThread()
        }

        val deviceMonitor = mock<DeviceMonitor> {
            on { isActivityStateValid() }.thenReturn(true)
            on { isNetworkStateValid() }.thenReturn(true)
            on { isBatteryStateValid() }.thenReturn(true)
        }

        val config = SyftConfiguration(
            mock(),
            schedulers,
            schedulers,
            mock(),
            true,
            batteryCheckEnabled = true,
            networkConstraints = listOf(),
            transportMedium = NetworkCapabilities.TRANSPORT_WIFI,
            cacheTimeOut = 0L,
            maxConcurrentJobs = 1,
            socketClient = socketClient,
            httpClient = httpClient,
            messagingClient = SyftConfiguration.NetworkingClients.SOCKET
        )

        val workerTest = spy(
            Syft(config, deviceMonitor, "auth token")
        )
        val modelName = "model name"
        val version = "1.0.0"
        val syftJob = SyftJob.create(
            modelName,
            "1.0.0",
            workerTest,
            config
        )

        workerTest.executeCycleRequest(syftJob)
        verify(socketClient).authenticate(
            AuthenticationRequest(
                "auth token",
                "model name",
                "1.0.0"
            )
        )
        verifyNoMoreInteractions(socketClient)
    }
}
