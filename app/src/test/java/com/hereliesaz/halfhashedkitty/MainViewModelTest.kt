package com.hereliesaz.halfhashedkitty

import android.app.Application
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt
import kotlinx.coroutines.test.*
========
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.junit.Assert.*
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt
import org.mockito.ArgumentMatchers.anyInt
========
import org.mockito.kotlin.mock
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt
import java.io.ByteArrayInputStream

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt
    private val testDispatcher = UnconfinedTestDispatcher()
========
    private val testDispatcher = StandardTestDispatcher()
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt

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
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt
========
        hashcatApiClient = mock()
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt
        viewModel = MainViewModel(application, hashcatApiClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt
    fun `startAttack successfully starts and polls for status`() = runTest {
========
    fun `startAttack successfully starts and polls for status`() = runTest(testDispatcher) {
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt
        // Given
        val jobId = "123"
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, attackMode = 0, wordlist = "wordlist")
        val attackResponse = AttackResponse(jobId = jobId, status = "Running")
        val crackedResponse = AttackResponse(jobId = jobId, status = "Cracked", crackedPassword = "password")

        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenReturn(attackResponse)
        `when`(hashcatApiClient.getAttackStatus(viewModel.serverUrl.value, jobId)).thenReturn(crackedResponse)

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = HashModeInfo(0, "MD5")
        viewModel.selectedAttackMode.value = HashModeInfo(0, "Straight")

        // When
        viewModel.startAttack()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.terminalOutput.contains("Attack started with job ID: $jobId"))
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt

        advanceTimeBy(6000)

========
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt
        assertTrue(viewModel.terminalOutput.contains("Job $jobId: Cracked"))
        assertEquals("password", viewModel.crackedPassword.value)
    }

    @Test
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt
    fun `startAttack handles network error`() = runTest {
========
    fun `startAttack handles network error`() = runTest(testDispatcher) {
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt
        // Given
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, attackMode = 0, wordlist = "wordlist")
        val errorMessage = "Network error"
        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenThrow(RuntimeException(errorMessage))

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = HashModeInfo(0, "MD5")
        viewModel.selectedAttackMode.value = HashModeInfo(0, "Straight")


        // When
        viewModel.startAttack()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.terminalOutput.contains("Error starting attack: $errorMessage"))
    }

    @Test
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt
    fun `pollForStatus handles exhausted status`() = runTest {
========
    fun `pollForStatus handles exhausted status`() = runTest(testDispatcher) {
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt
        // Given
        val jobId = "123"
        val attackRequest = AttackRequest(hash = "hash", hashType = 0, attackMode = 0, wordlist = "wordlist")
        val attackResponse = AttackResponse(jobId = jobId, status = "Running")
        val exhaustedResponse = AttackResponse(jobId = jobId, status = "Exhausted")

        `when`(hashcatApiClient.startAttack(viewModel.serverUrl.value, attackRequest)).thenReturn(attackResponse)
        `when`(hashcatApiClient.getAttackStatus(viewModel.serverUrl.value, jobId)).thenReturn(exhaustedResponse)

        viewModel.hashToCrack.value = "hash"
        viewModel.wordlistPath.value = "wordlist"
        viewModel.selectedHashMode.value = HashModeInfo(0, "MD5")
        viewModel.selectedAttackMode.value = HashModeInfo(0, "Straight")

        // When
        viewModel.startAttack()
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt

        advanceTimeBy(6000)
========
        testDispatcher.scheduler.advanceUntilIdle()
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt

        // Then
        assertTrue(viewModel.terminalOutput.contains("Attack finished. Password not found."))
    }
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/MainViewModelTest.kt

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
        assertEquals(HashModeInfo(0, "MD5"), viewModel.hashModes[0])
        assertEquals(HashModeInfo(1, "SHA1"), viewModel.hashModes[1])
        assertEquals(HashModeInfo(0, "MD5"), viewModel.selectedHashMode.value)
    }
========
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/MainViewModelTest.kt
}
