package com.example.hashcatgui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@ExperimentalCoroutinesApi
class HashtopolisViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var hashtopolisApiClient: HashtopolisApiClient

    private lateinit var viewModel: HashtopolisViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = HashtopolisViewModel(hashtopolisApiClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAgents successfully fetches agents`() = runTest {
        // Given
        val agents = listOf(Agent(1, "Agent1", "Running", "now"))
        `when`(hashtopolisApiClient.getAgents(viewModel.serverUrl.value, viewModel.apiKey.value)).thenReturn(agents)

        // When
        viewModel.getAgents()

        // Then
        assertEquals(1, viewModel.agents.size)
        assertEquals("Agent1", viewModel.agents[0].name)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `getAgents handles network error`() = runTest {
        // Given
        val errorMessage = "Network error"
        `when`(hashtopolisApiClient.getAgents(viewModel.serverUrl.value, viewModel.apiKey.value)).thenThrow(RuntimeException(errorMessage))

        // When
        viewModel.getAgents()

        // Then
        assertEquals(0, viewModel.agents.size)
        assertEquals("Error fetching agents: $errorMessage", viewModel.errorMessage.value)
    }
}
