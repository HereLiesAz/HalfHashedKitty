package com.hereliesaz.halfhashedkitty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hereliesaz.halfhashedkitty.ui.theme.HalfHashedKittyTheme // Changed here

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory(
            application,
            HashcatApiClient(),
            Cap2HashcatApiClient()
        )
    }
    private val hashtopolisViewModel: HashtopolisViewModel by viewModels()
    private val piControlViewModel: PiControlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HalfHashedKittyTheme { // Changed here
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(mainViewModel, hashtopolisViewModel, piControlViewModel)
                }
            }
        }
    }
}