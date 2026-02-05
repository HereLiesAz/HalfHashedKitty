package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel
import com.hereliesaz.halfhashedkitty.ui.screens.ScannerScreen

/**
 * Composable function for the "Connect" tab.
 * <p>
 * This screen handles the connection to the desktop application.
 * It offers two primary methods:
 * 1. **Relay Connection:** Connecting via a shared "Room ID" on a public relay server.
 * 2. **Direct Connection:** Connecting directly to the desktop's IP address (LAN).
 * </p>
 * <p>
 * It includes a QR Code scanner for easy configuration and a manual input field.
 * </p>
 */
@Composable
fun ConnectTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Connect to Desktop", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle Switch for Connection Type (Relay vs Direct).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Relay")
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Switch(
                checked = viewModel.connectionType.value == MainViewModel.ConnectionType.DIRECT,
                onCheckedChange = { isChecked ->
                    // Update ViewModel state based on toggle.
                    viewModel.connectionType.value = if (isChecked) {
                        MainViewModel.ConnectionType.DIRECT
                    } else {
                        MainViewModel.ConnectionType.RELAY
                    }
                }
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text("Direct (LAN)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QR Code Scanner Area.
        // Wrapped in a Box to constrain the aspect ratio (square).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false) // Allow it to shrink if needed, but prefer weight.
                .aspectRatio(1f)
        ) {
            // Embed the camera scanner view.
            ScannerScreen(
                instructionText = "Scan the QR code from the desktop app to connect.",
                onQrCodeScanned = { qrCodeValue ->
                    // Callback when a code is successfully detected.
                    viewModel.onQrCodeScanned(qrCodeValue)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Or Connect Manually")

        Spacer(modifier = Modifier.height(8.dp))

        // Manual Input Field.
        OutlinedTextField(
            value = viewModel.manualInput.value,
            onValueChange = { viewModel.manualInput.value = it },
            label = { Text("IP Address or Relay Room ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connect Button.
        Button(onClick = { viewModel.connectManually() }) {
            Text("Connect")
        }
    }
}
