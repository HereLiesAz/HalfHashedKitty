package com.hereliesaz.halfhashedkitty

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/HashtopolisViewModelTest.kt
import kotlinx.coroutines.test.UnconfinedTestDispatcher
========
import kotlinx.coroutines.test.StandardTestDispatcher
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/HashtopolisViewModelTest.kt
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

@ExperimentalCoroutinesApi
class HashtopolisViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/HashtopolisViewModelTest.kt
    private val testDispatcher = UnconfinedTestDispatcher()
========
    private val testDispatcher = StandardTestDispatcher()
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/HashtopolisViewModelTest.kt

    private lateinit var hashtopolisApiClient: HashtopolisApiClient

    private lateinit var viewModel: HashtopolisViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/HashtopolisViewModelTest.kt
========
        hashtopolisApiClient = mock()
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/HashtopolisViewModelTest.kt
        viewModel = HashtopolisViewModel(hashtopolisApiClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/HashtopolisViewModelTest.kt
    fun `getAgents successfully fetches agents`() = runTest {
========
    fun `getAgents successfully fetches agents`() = runTest(testDispatcher) {
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/HashtopolisViewModelTest.kt
        // Given
        val agents = listOf(Agent(1, "Agent1", "Running", "now"))
        `when`(hashtopolisApiClient.getAgents(viewModel.serverUrl.value, viewModel.apiKey.value)).thenReturn(agents)

        // When
        viewModel.getAgents()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.agents.size)
        assertEquals("Agent1", viewModel.agents[0].name)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
<<<<<<<< HEAD:app/src/test/java/com/example/halfhashedkitty/HashtopolisViewModelTest.kt
    fun `getAgents handles network error`() = runTest {
========
    fun `getAgents handles network error`() = runTest(testDispatcher) {
>>>>>>>> origin/feature/fix-tests:app/src/test/java/com/hereliesaz/halfhashedkitty/HashtopolisViewModelTest.kt
        // Given
        val errorMessage = "Network error"
        `when`(hashtopolisApiClient.getAgents(viewModel.serverUrl.value, viewModel.apiKey.value)).thenThrow(RuntimeException(errorMessage))

        // When
        viewModel.getAgents()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(0, viewModel.agents.size)
        assertEquals("Error fetching agents: $errorMessage", viewModel.errorMessage.value)
    }
}
