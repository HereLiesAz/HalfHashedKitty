package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.hereliesaz.halfhashedkitty.ui.tabs.MaskTab
import com.hereliesaz.halfhashedkitty.ui.tabs.OutputTab
import com.hereliesaz.halfhashedkitty.ui.tabs.PiControlTab
import com.hereliesaz.halfhashedkitty.ui.tabs.SetupTab
import com.hereliesaz.halfhashedkitty.ui.tabs.TerminalTab
import com.hereliesaz.halfhashedkitty.ui.tabs.WordlistTab

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hashtopolisViewModel: HashtopolisViewModel,
    piControlViewModel: PiControlViewModel
) {
    var selectedId by remember { mutableStateOf("Input") }

    Row(modifier = Modifier.fillMaxSize()) {
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
        }
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
