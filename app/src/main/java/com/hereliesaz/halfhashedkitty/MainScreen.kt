package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.halfhashedkitty.ui.tabs.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hashtopolisViewModel: HashtopolisViewModel,
    piControlViewModel: PiControlViewModel
) {
    var selectedId by remember { mutableStateOf("Input") }
    var showInstructions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Instructions")
                        Switch(
                            checked = showInstructions,
                            onCheckedChange = { showInstructions = it }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AzNavRail {
                azRailItem(id = "Input", text = "Input") { selectedId = "Input" }
                azRailItem(id = "Attack", text = "Attack") { selectedId = "Attack" }
                azRailItem(id = "Wordlist", text = "Wordlist") { selectedId = "Wordlist" }
                azRailItem(id = "Mask", text = "Mask") { selectedId = "Mask" }
                azRailItem(id = "Capture", text = "Capture") { selectedId = "Capture" }
                azRailItem(id = "Terminal", text = "Terminal") { selectedId = "Terminal" }
                azRailItem(id = "Output", text = "Output") { selectedId = "Output" }
                azRailItem(id = "Hashtopolis", text = "Hashtopolis") { selectedId = "Hashtopolis" }
                azRailItem(id = "Pi Control", text = "Pi Control") { selectedId = "Pi Control" }
                azRailItem(id = "PC Connect", text = "PC Connect") { selectedId = "PC Connect" }
            }

            Box(modifier = Modifier.fillMaxSize()) {
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
                    "PC Connect" -> PCConnectionTab()
                }
                if (showInstructions) {
                    InstructionsOverlay(selectedId) {
                        showInstructions = false
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
    // Also need to update the theme in the Preview if it uses it directly
    // For now, MainScreen is called which now uses the correct theme
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
