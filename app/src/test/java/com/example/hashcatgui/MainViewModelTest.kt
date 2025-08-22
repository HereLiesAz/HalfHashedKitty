package com.example.hashcatgui

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.junit.Assert.*
import org.mockito.ArgumentMatchers.anyInt
import java.io.ByteArrayInputStream

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var resources: Resources

    @Mock
    private lateinit var hashcatApiClient: HashcatApiClient

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        `when`(application.resources).thenReturn(resources)
        viewModel = MainViewModel(application)
        // Manually inject the mock client
        val clientField = viewModel.javaClass.getDeclaredField("apiClient")
        clientField.isAccessible = true
        clientField.set(viewModel, hashcatApiClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startAttack successfully starts and polls for status`() = runTest {
        // Given
        val jobId = "123"
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, attackMode = 0, wordlist = "wordlist")
        val attackResponse = AttackResponse(jobId = jobId, status = "Running")
        val crackedResponse = AttackResponse(jobId = jobId, status = "Cracked", crackedPassword = "password")

        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenReturn(attackResponse)
        `when`(hashcatApiClient.getAttackStatus(viewModel.serverUrl.value, jobId)).thenReturn(crackedResponse)

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = Pair(0, "MD5")
        viewModel.selectedAttackMode.value = Pair(0, "Straight")

        // When
        viewModel.startAttack()

        // Then
        assertTrue(viewModel.terminalOutput.contains("Attack started with job ID: $jobId"))

        advanceTimeBy(6000)

        assertTrue(viewModel.terminalOutput.contains("Job $jobId: Cracked"))
        assertEquals("password", viewModel.crackedPassword.value)
    }

    @Test
    fun `startAttack handles network error`() = runTest {
        // Given
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, attackMode = 0, wordlist = "wordlist")
        val errorMessage = "Network error"
        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenThrow(RuntimeException(errorMessage))

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = Pair(0, "MD5")
        viewModel.selectedAttackMode.value = Pair(0, "Straight")


        // When
        viewModel.startAttack()

        // Then
        assertTrue(viewModel.terminalOutput.contains("Error starting attack: $errorMessage"))
    }

    @Test
    fun `pollForStatus handles exhausted status`() = runTest {
        // Given
        val jobId = "123"
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, attackMode = 0, wordlist = "wordlist")
        val attackResponse = AttackResponse(jobId = jobId, status = "Running")
        val exhaustedResponse = AttackResponse(jobId = jobId, status = "Exhausted")

        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenReturn(attackResponse)
        `when`(hashcatApiClient.getAttackStatus(viewModel.serverUrl.value, jobId)).thenReturn(exhaustedResponse)

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = Pair(0, "MD5")
        viewModel.selectedAttackMode.value = Pair(0, "Straight")

        // When
        viewModel.startAttack()

        advanceTimeBy(6000)

        // Then
        assertTrue(viewModel.terminalOutput.contains("Attack finished. Password not found."))
    }

    @Test
    fun `loadHashModes loads modes from file`() = runTest {
        // Given
        val modes = "0 MD5\n1 SHA1"
        val inputStream = ByteArrayInputStream(modes.toByteArray())
        `when`(resources.openRawResource(anyInt())).thenReturn(inputStream)

        // When
        viewModel.loadHashModes()

        // Then
        assertEquals(2, viewModel.hashModes.size)
        assertEquals(Pair(0, "MD5"), viewModel.hashModes[0])
        assertEquals(Pair(1, "SHA1"), viewModel.hashModes[1])
        assertEquals(Pair(0, "MD5"), viewModel.selectedHashMode.value)
    }
}
