package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.halfhashedkitty.ui.tabs.AttackTab
import com.hereliesaz.halfhashedkitty.ui.tabs.HashtopolisTab
import com.hereliesaz.halfhashedkitty.ui.tabs.InstructionsOverlay
import com.hereliesaz.halfhashedkitty.ui.tabs.MaskTab
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
    var selectedId by remember { mutableStateOf("Attack") }
    var showInstructions by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.half_hashed_kitty_banner),
            contentDescription = "background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.05f),
            contentScale = ContentScale.Crop
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AzNavRail {
                azRailItem(id = "Attack", text = "Attack") { selectedId = "Attack"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Wordlist", text = "Wordlist") { selectedId = "Wordlist"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Mask", text = "Mask") { selectedId = "Mask"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Terminal", text = "Terminal") { selectedId = "Terminal"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Hashtopolis", text = "Hashtopolis") { selectedId = "Hashtopolis"; if (showInstructions) showInstructions = false }
                azRailItem(id = "Pi Control", text = "Pi Control") { selectedId = "Pi Control"; if (showInstructions) showInstructions = false }
                azRailItem(id = "PC Connect", text = "PC Connect") { selectedId = "PC Connect"; if (showInstructions) showInstructions = false }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (!showInstructions) {
                    when (selectedId) {
                        "Attack" -> AttackTab(viewModel) { showInstructions = true }
                        "Wordlist" -> WordlistTab(viewModel) { showInstructions = true }
                        "Mask" -> MaskTab { showInstructions = true }
                        "Terminal" -> TerminalTab(viewModel) { showInstructions = true }
                        "Hashtopolis" -> HashtopolisTab(hashtopolisViewModel) { showInstructions = true }
                        "Pi Control" -> PiControlTab(piControlViewModel) { showInstructions = true }
                        "PC Connect" -> PCConnectionTab(viewModel) { showInstructions = true }
                    }
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
    MainScreen(
        MainViewModel(
            Application(),
            HashcatApiClient(),
            Cap2HashcatApiClient()
        ),
        HashtopolisViewModel(),
        PiControlViewModel()
    )
}