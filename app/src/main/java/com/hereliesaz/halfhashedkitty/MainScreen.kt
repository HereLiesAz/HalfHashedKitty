package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.halfhashedkitty.ui.tabs.AttackTab
import com.hereliesaz.halfhashedkitty.ui.tabs.CaptureTab
import com.hereliesaz.halfhashedkitty.ui.tabs.HashtopolisTab
import com.hereliesaz.halfhashedkitty.ui.tabs.InputTab
import com.hereliesaz.halfhashedkitty.ui.tabs.InstructionsOverlay // Assuming this import might be needed or is already present
import com.hereliesaz.halfhashedkitty.ui.tabs.MaskTab
import com.hereliesaz.halfhashedkitty.ui.tabs.OutputTab
import com.hereliesaz.halfhashedkitty.ui.tabs.PCConnectionTab
import com.hereliesaz.halfhashedkitty.ui.tabs.PiControlTab
import com.hereliesaz.halfhashedkitty.ui.tabs.TerminalTab
import com.hereliesaz.halfhashedkitty.ui.tabs.WordlistTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hashtopolisViewModel: HashtopolisViewModel,
    piControlViewModel: PiControlViewModel
) {
    var selectedId by remember { mutableStateOf("Input") }
    var showInstructions by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AzNavRail {
                azRailItem(id = "Input", text = "Input") { selectedId = "Input"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Attack", text = "Attack") { selectedId = "Attack"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Wordlist", text = "Wordlist") { selectedId = "Wordlist"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Mask", text = "Mask") { selectedId = "Mask"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Capture", text = "Capture") { selectedId = "Capture"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Terminal", text = "Terminal") { selectedId = "Terminal"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Output", text = "Output") { selectedId = "Output"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Hashtopolis", text = "Hashtopolis") { selectedId = "Hashtopolis"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Pi Control", text = "Pi Control") { selectedId = "Pi Control"; if (showInstructions) showInstructions = false }
                azRailItem(id = "PC Connect", text = "PC Connect") { selectedId = "PC Connect"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Instructions", text = "Instructions") { showInstructions = !showInstructions }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (!showInstructions) {
                    when (selectedId) {
                        "Input" -> InputTab(viewModel)
                        "Attack" -> AttackTab(viewModel)
                        "Wordlist" -> WordlistTab(viewModel)
                        "Mask" -> MaskTab()
                        "Capture" -> CaptureTab(viewModel)
                        "Terminal" -> TerminalTab(viewModel)
                        "Output" -> OutputTab(viewModel)
                        "Hashtopolis" -> HashtopolisTab(hashtopolisViewModel)
                        "Pi Control" -> PiControlTab(piControlViewModel)
                        "PC Connect" -> PCConnectionTab(viewModel)
                        // Note: The "Instructions" item in AzNavRail only toggles the overlay
                        // It doesn't show a dedicated tab content here.
                    }
                }
                if (showInstructions) {
                    InstructionsOverlay(selectedId) {
                        showInstructions = false // This allows the overlay to close itself
                    }
                }
            }
        }
    }
}

// Preview for MainScreen
// Suppressed for preview mode, where it's acceptable to construct ViewModels directly.
@Suppress("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen(
        MainViewModel(
            Application(),
            HashcatApiClient(),
            Cap2HashcatApiClient(),
            ToolManager(Application())
        ),
        HashtopolisViewModel(),
        PiControlViewModel()
    )
}
