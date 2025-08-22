package com.hereliesaz.halfhashedkitty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
            Cap2HashcatApiClient(),
            ToolManager(applicationContext)
        )
    }
    private val hashtopolisViewModel: HashtopolisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HalfHashedKittyTheme { // Changed here
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(mainViewModel, hashtopolisViewModel)
                }
            }
        }
    }
}
