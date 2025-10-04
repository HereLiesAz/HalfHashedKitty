package com.hereliesaz.halfhashedkitty

import android.app.Application
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.io.ByteArrayInputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var resources: Resources

    private lateinit var hashcatApiClient: HashcatApiClient
    private lateinit var cap2hashcatApiClient: Cap2HashcatApiClient
    private val incomingMessages = MutableSharedFlow<String>()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        `when`(application.resources).thenReturn(resources)
        val mockInputStream = ByteArrayInputStream("0 MD5\n10 SHA1".toByteArray())
        `when`(resources.openRawResource(R.raw.modes)).thenReturn(mockInputStream)

        hashcatApiClient = mock()
        `when`(hashcatApiClient.incomingMessages).thenReturn(incomingMessages)
        cap2hashcatApiClient = mock()
        viewModel = MainViewModel(application, hashcatApiClient, cap2hashcatApiClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onQrCodeScanned connects to relay and joins room`() = runTest(testDispatcher) {
        // Given
        val roomId = "test-room"
        val connectJob = launch { viewModel.onQrCodeScanned(roomId) }
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val roomInfo = RoomInfo(type = "room_id", id = roomId)
        val message = WebSocketMessage(type = "room_id", payload = Json.encodeToString(roomInfo), room_id = roomId)
        incomingMessages.emit(Json.encodeToString(message))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(hashcatApiClient).connect(any(), eq(roomId), any())
        assertTrue(viewModel.isConnected.value)
        assertTrue(viewModel.terminalOutput.any { it.contains("Successfully connected to relay and joined room: $roomId") })
        connectJob.cancel()
    }

    @Test
    fun `startAttack sends message when connected`() = runTest(testDispatcher) {
        // Given
        val roomId = "test-room"
        val connectJob = launch { viewModel.onQrCodeScanned(roomId) }
        val roomInfo = RoomInfo(type = "room_id", id = roomId)
        val connectMessage = WebSocketMessage(type = "room_id", payload = Json.encodeToString(roomInfo), room_id = roomId)
        incomingMessages.emit(Json.encodeToString(connectMessage))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectedHashMode.value = viewModel.hashModes.first()
        viewModel.hashToCrack.value = "some/path/to/hash"
        viewModel.wordlistPath.value = "some/path/to/wordlist"

        // When
        viewModel.startAttack()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val captor = argumentCaptor<WebSocketMessage>()
        verify(hashcatApiClient).sendMessage(captor.capture())
        val sentMessage = captor.firstValue
        assertEquals("attack", sentMessage.type)
        assertTrue(sentMessage.payload.contains("\"mode\":\"0\""))
        assertTrue(viewModel.terminalOutput.any { it.startsWith("Attack command sent for job ID:") })
        connectJob.cancel()
    }

    @Test
    fun `startAttack does not send message when not connected`() = runTest(testDispatcher) {
        // Given
        // ViewModel is not connected by default

        // When
        viewModel.startAttack()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(hashcatApiClient, never()).sendMessage(any())
        assertTrue(viewModel.terminalOutput.contains("Not connected. Please scan the QR code from the desktop client."))
    }
}