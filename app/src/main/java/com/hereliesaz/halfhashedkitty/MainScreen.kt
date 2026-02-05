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

/**
 * The primary composable function representing the main UI structure of the application.
 * <p>
 * This function sets up the navigation rail (sidebar), the main content area, and the
 * dynamic switching between different "Tabs" (Attack, Sniff, Settings, etc.).
 * It also handles the "Instructions Overlay" logic which shows help text for the active screen.
 * </p>
 *
 * @param viewModel             The primary ViewModel for the app.
 * @param hashtopolisViewModel  ViewModel for Hashtopolis integration features.
 * @param piControlViewModel    ViewModel for Pi/Sniffing features (legacy naming, kept for compatibility).
 * @param sniffViewModel        ViewModel for the Sniff tab features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hashtopolisViewModel: HashtopolisViewModel,
    piControlViewModel: PiControlViewModel,
    sniffViewModel: SniffViewModel
) {
    // State to track which tab is currently selected. Defaults to "Attack".
    var selectedId by remember { mutableStateOf("Attack") }
    // State to track if the help/instructions overlay is visible.
    var showInstructions by remember { mutableStateOf(false) }

    // Root container.
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image (The Half-Hashed Kitty Banner).
        // Applied with 50% opacity (alpha) to allow text readability on top.
        Image(
            painter = painterResource(id = R.drawable.half_hashed_kitty_banner),
            contentDescription = "background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f),
            contentScale = ContentScale.Crop
        )

        // Main Scaffold structure.
        // Container color is set to Transparent so the background image shows through.
        Scaffold(containerColor = Color.Transparent) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Navigation Column (Left Sidebar).
                Column {
                    // Custom Navigation Rail component.
                    AzNavRail {
                        // Attack Tab
                        azRailItem(
                            id = "Attack",
                            color = Color.White,
                            text = "Attack"
                        ) {
                            selectedId = "Attack"
                            if (showInstructions) showInstructions = false // Hide help when switching tabs
                        }
                        // Wordlist Tab
                        azRailItem(
                            id = "Wordlist",
                            color = Color.White,
                            text = "Wordlist"
                        ) {
                            selectedId = "Wordlist"
                            if (showInstructions) showInstructions = false
                        }
                        // Mask Tab
                        azRailItem(id = "Mask", color = Color.White, text = "Mask") {
                            selectedId = "Mask"
                            if (showInstructions) showInstructions = false
                        }
                        // Terminal Tab (Logs)
                        azRailItem(
                            id = "Terminal",
                            color = Color.White,
                            text = "Terminal"
                        ) {
                            selectedId = "Terminal"
                            if (showInstructions) showInstructions = false
                        }
                        // Sniff Tab
                        azRailItem(
                            id = "Sniff",
                            color = Color.White,
                            text = "Sniff"
                        ) {
                            selectedId = "Sniff"
                            if (showInstructions) showInstructions = false
                        }
                        // Hashtopolis Tab
                        azRailItem(
                            id = "Hashtopolis",
                            color = Color.White,
                            text = "Hashtopolis"
                        ) {
                            selectedId = "Hashtopolis"
                            if (showInstructions) showInstructions = false
                        }
                        // Connect Tab (Relay/Direct setup)
                        azRailItem(
                            id = "Connect",
                            color = Color.White,
                            text = "Connect"
                        ) {
                            selectedId = "Connect"
                            if (showInstructions) showInstructions = false
                        }
                        // Setup Tab (Hashcat installation helper)
                        azRailItem(
                            id = "Setup",
                            color = Color.White,
                            text = "Setup"
                        ) {
                            selectedId = "Setup"
                            if (showInstructions) showInstructions = false
                        }
                    }
                }

                // Main Content Column (Right side).
                Column(modifier = Modifier.weight(1f)) {
                    // Title Bar with Help Button.
                    ScreenTitle(title = selectedId) {
                        // Callback when help icon is clicked.
                        showInstructions = true
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Render the content of the selected tab if instructions are NOT showing.
                        if (!showInstructions) {
                            when (selectedId) {
                                "Attack" -> AttackTab(viewModel)
                                "Wordlist" -> WordlistTab(viewModel)
                                "Mask" -> MaskTab()
                                "Terminal" -> TerminalTab(viewModel)
                                "Sniff" -> SniffTab(sniffViewModel)
                                "Hashtopolis" -> HashtopolisTab(hashtopolisViewModel)
                                "Connect" -> ConnectTab(viewModel)
                                "Hashcat Setup" -> HashcatSetupTab() // Mapped to the "Setup" rail item.
                                "Setup" -> HashcatSetupTab() // Redundant mapping to ensure "Setup" ID works.
                            }
                        }
                        // Render instructions overlay if active.
                        if (showInstructions) {
                            InstructionsOverlay(selectedId) {
                                // Callback to close instructions.
                                showInstructions = false
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview provider for the MainScreen.
 * Mocking dependencies is required here because ViewModels usually require Android context.
 */
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Preview()
@Composable
fun DefaultPreview() {
    // Create dummy ViewModels for the preview.
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.MainViewModelFactory(
            Application(),
            HashcatApiClient(),
            Cap2HashcatApiClient()
        )
    )
    val sniffViewModel: SniffViewModel = viewModel(
        factory = SniffViewModel.SniffViewModelFactory(HashcatApiClient(), mainViewModel)
    )
    MainScreen(
        mainViewModel,
        HashtopolisViewModel(HashtopolisApiClient()),
        viewModel(),
        sniffViewModel
    )
}
