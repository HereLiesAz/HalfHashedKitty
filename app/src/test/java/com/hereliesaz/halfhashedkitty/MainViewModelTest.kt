package com.hereliesaz.halfhashedkitty

import android.app.Application
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.junit.Assert.*
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream

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

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        `when`(application.resources).thenReturn(resources)
        hashcatApiClient = mock()
        viewModel = MainViewModel(application, hashcatApiClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startAttack successfully starts and polls for status`() = runTest(testDispatcher) {
        // Given
        val jobId = "123"
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, wordlist = "wordlist")
        val attackResponse = AttackResponse(jobId = jobId, status = "Running")
        val crackedResponse = AttackResponse(jobId = jobId, status = "Cracked", crackedPassword = "password")

        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenReturn(attackResponse)
        `when`(hashcatApiClient.getAttackStatus(viewModel.serverUrl.value, jobId)).thenReturn(crackedResponse)

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = Pair(0, "MD5")

        // When
        viewModel.startAttack()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.terminalOutput.contains("Attack started with job ID: $jobId"))
        assertTrue(viewModel.terminalOutput.contains("Job $jobId: Cracked"))
        assertEquals("password", viewModel.crackedPassword.value)
    }

    @Test
    fun `startAttack handles network error`() = runTest(testDispatcher) {
        // Given
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, wordlist = "wordlist")
        val errorMessage = "Network error"
        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenThrow(RuntimeException(errorMessage))

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = Pair(0, "MD5")

        // When
        viewModel.startAttack()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.terminalOutput.contains("Error starting attack: $errorMessage"))
    }

    @Test
    fun `pollForStatus handles exhausted status`() = runTest(testDispatcher) {
        // Given
        val jobId = "123"
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, wordlist = "wordlist")
        val attackResponse = AttackResponse(jobId = jobId, status = "Running")
        val exhaustedResponse = AttackResponse(jobId = jobId, status = "Exhausted")

        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenReturn(attackResponse)
        `when`(hashcatApiClient.getAttackStatus(viewModel.serverUrl.value, jobId)).thenReturn(exhaustedResponse)

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = Pair(0, "MD5")

        // When
        viewModel.startAttack()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.terminalOutput.contains("Attack finished. Password not found."))
    }
}
