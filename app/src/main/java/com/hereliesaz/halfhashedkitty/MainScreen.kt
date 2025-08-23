package com.hereliesaz.halfhashedkitty

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.NavItem
import com.hereliesaz.aznavrail.NavItemData
import com.hereliesaz.halfhashedkitty.ui.screens.ScannerScreen
import com.hereliesaz.halfhashedkitty.ui.tabs.*
import com.hereliesaz.halfhashedkitty.ui.theme.HalfHashedKittyTheme
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch

@Composable
fun MainScreen(mainViewModel: MainViewModel, hashtopolisViewModel: HashtopolisViewModel) {
    var selectedIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Setup", "Input", "Attack", "Wordlist", "Mask", "Output", "Capture", "Terminal", "Hashtopolis", "Command", "Scan QR")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val client = HttpClient(CIO) {
        install(WebSockets)
    }

    val navItems = tabs.mapIndexed { index, title ->
        NavItem(
            text = title,
            data = NavItemData.Action(onClick = { selectedIndex = index }),
            showOnRail = true
        )
    }

    HalfHashedKittyTheme {
        Row {
            AzNavRail(
                menuSections = listOf(
                    com.hereliesaz.aznavrail.NavRailMenuSection(
                        title = "Main",
                        items = navItems
                    )
                )
            )

            when (selectedIndex) {
                0 -> SetupTab(mainViewModel)
                1 -> InputTab(mainViewModel)
                2 -> AttackTab(mainViewModel)
                3 -> WordlistTab(mainViewModel)
                4 -> MaskTab(mainViewModel)
                5 -> OutputTab(mainViewModel)
                6 -> CaptureTab(mainViewModel)
                7 -> TerminalTab(mainViewModel)
                8 -> HashtopolisTab(hashtopolisViewModel)
                9 -> CommandBuilderTab(mainViewModel)
                10 -> ScannerScreen(onQrCodeScanned = { qrCode ->
                    Toast.makeText(context, "Connecting to $qrCode", Toast.LENGTH_SHORT).show()
                    val parts = qrCode.split(":")
                    if (parts.size == 2) {
                        val host = parts[0]
                        val port = parts[1].toIntOrNull()
                        if (port != null) {
                            coroutineScope.launch {
                                try {
                                    client.webSocket(method = io.ktor.http.HttpMethod.Get, host = host, port = port, path = "/") {
                                        Log.d("MainScreen", "WebSocket connection established")
                                        for (frame in incoming) {
                                            frame as? Frame.Text ?: continue
                                            val receivedText = frame.readText()
                                            Log.d("MainScreen", "Received: $receivedText")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainScreen", "WebSocket connection failed", e)
                                }
                            }
                        }
                    }
                })
            }
        }
    }
}

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
        HashtopolisViewModel()
    )
}
