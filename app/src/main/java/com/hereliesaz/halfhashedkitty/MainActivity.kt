package com.hereliesaz.halfhashedkitty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hereliesaz.halfhashedkitty.ui.theme.HalfHashedKittyTheme

/**
 * The single Activity for the Half-Hashed Kitty Android application.
 * <p>
 * This app uses the "Single Activity" architecture pattern common in Jetpack Compose applications.
 * {@code MainActivity} serves as the entry point and container for the Compose UI hierarchy.
 * It is responsible for initializing the ViewModels and setting the content view to {@link MainScreen}.
 * </p>
 */
class MainActivity : ComponentActivity() {

    /**
     * Lazily initializes the MainViewModel.
     * Uses a custom factory to inject dependencies (Application context and API clients).
     */
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory(
            application,
            HashcatApiClient(),
            Cap2HashcatApiClient()
        )
    }

    /**
     * Lazily initializes the HashtopolisViewModel.
     * Uses the default factory as it currently has no complex dependencies in its constructor (or handles them internally).
     */
    private val hashtopolisViewModel: HashtopolisViewModel by viewModels()

    /**
     * Lazily initializes the PiControlViewModel.
     * This ViewModel manages interactions with a Raspberry Pi (e.g., for sniffing).
     */
    private val piControlViewModel: PiControlViewModel by viewModels()

    /**
     * Lazily initializes the SniffViewModel.
     * It requires access to the shared API client used by MainViewModel to coordinate messages.
     */
    private val sniffViewModel: SniffViewModel by viewModels {
        SniffViewModel.SniffViewModelFactory(
            mainViewModel.getApiClient(), // Dependency injection: Sharing the WebSocket client.
            mainViewModel
        )
    }

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the Jetpack Compose UI content.
        setContent {
            // Apply the custom application theme (Typography, Colors, Shapes).
            HalfHashedKittyTheme {
                // A surface container using the 'background' color from the theme.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Render the main screen composable, passing in the ViewModels.
                    MainScreen(mainViewModel, hashtopolisViewModel, piControlViewModel, sniffViewModel)
                }
            }
        }
    }
}
