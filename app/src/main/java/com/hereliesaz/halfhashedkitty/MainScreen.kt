package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.halfhashedkitty.ui.tabs.AttackTab
import com.hereliesaz.halfhashedkitty.ui.tabs.ConnectTab
import com.hereliesaz.halfhashedkitty.ui.tabs.HashcatSetupTab
import com.hereliesaz.halfhashedkitty.ui.tabs.HashtopolisTab
import com.hereliesaz.halfhashedkitty.ui.tabs.InstructionsOverlay
import com.hereliesaz.halfhashedkitty.ui.tabs.MaskTab
import com.hereliesaz.halfhashedkitty.ui.tabs.SniffTab
import com.hereliesaz.halfhashedkitty.ui.tabs.ScreenTitle
import com.hereliesaz.halfhashedkitty.ui.tabs.TerminalTab
import com.hereliesaz.halfhashedkitty.ui.tabs.WordlistTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hashtopolisViewModel: HashtopolisViewModel,
    piControlViewModel: PiControlViewModel,
    sniffViewModel: SniffViewModel
) {
    var selectedId by remember { mutableStateOf("Attack") }
    var showInstructions by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.half_hashed_kitty_banner),
            contentDescription = "background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f),
            contentScale = ContentScale.Crop
        )
        Scaffold(containerColor = Color.Transparent) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column {
                    AzNavRail {
                        azRailItem(
                            id = "Attack",
                            color = Color.White,
                            text = "Attack"
                        ) { selectedId = "Attack"; if (showInstructions) showInstructions = false }
                        azRailItem(
                            id = "Wordlist",
                            color = Color.White,
                            text = "Wordlist"
                        ) {
                            selectedId = "Wordlist"; if (showInstructions) showInstructions = false
                        }
                        azRailItem(id = "Mask", color = Color.White, text = "Mask") {
                            selectedId = "Mask"; if (showInstructions) showInstructions = false
                        }
                        azRailItem(
                            id = "Terminal",
                            color = Color.White,
                            text = "Terminal"
                        ) {
                            selectedId = "Terminal"; if (showInstructions) showInstructions = false
                        }
                        azRailItem(
                            id = "Sniff",
                            color = Color.White,
                            text = "Sniff"
                        ) {
                            selectedId = "Sniff"; if (showInstructions) showInstructions =
                            false
                        }
                        azRailItem(
                            id = "Hashtopolis",
                            color = Color.White,
                            text = "Hashtopolis"
                        ) {
                            selectedId = "Hashtopolis"; if (showInstructions) showInstructions =
                            false
                        }
                        azRailItem(
                            id = "Connect",
                            color = Color.White,
                            text = "Connect"
                        ) {
                            selectedId = "Connect"; if (showInstructions) showInstructions =
                            false
                        }
                        azRailItem(
                            id = "Setup",
                            color = Color.White,
                            text = "Setup"
                        ) {
                            selectedId = "Setup"; if (showInstructions) showInstructions =
                            false
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    ScreenTitle(title = selectedId) {
                        showInstructions = true
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!showInstructions) {
                            when (selectedId) {
                                "Attack" -> AttackTab(viewModel)
                                "Wordlist" -> WordlistTab(viewModel)
                                "Mask" -> MaskTab()
                                "Terminal" -> TerminalTab(viewModel)
                                "Sniff" -> SniffTab(sniffViewModel)
                                "Hashtopolis" -> HashtopolisTab(hashtopolisViewModel)
                                "Connect" -> ConnectTab(viewModel)
                                "Hashcat Setup" -> HashcatSetupTab()
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
    }
}
// Preview for MainScreen
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Preview()
@Composable
fun DefaultPreview() {
    val mainViewModel = MainViewModel(
        Application(),
        HashcatApiClient(),
        Cap2HashcatApiClient()
    )
    MainScreen(
        mainViewModel,
        HashtopolisViewModel(HashtopolisApiClient()),
        viewModel(),
        viewModel(factory = SniffViewModel.SniffViewModelFactory(HashcatApiClient(), mainViewModel))
    )
}
