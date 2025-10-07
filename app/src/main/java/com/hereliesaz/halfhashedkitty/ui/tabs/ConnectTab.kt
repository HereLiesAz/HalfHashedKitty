package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel
import com.hereliesaz.halfhashedkitty.ui.screens.ScannerScreen

@Composable
fun ConnectTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Connection Type Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Relay")
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Switch(
                checked = viewModel.connectionType.value == MainViewModel.ConnectionType.DIRECT,
                onCheckedChange = { isChecked ->
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

        // QR Code Scanner (constrained size)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .aspectRatio(1f)
        ) {
            ScannerScreen(
                instructionText = "Scan the QR code from the desktop app to connect.",
                onQrCodeScanned = { qrCodeValue ->
                    viewModel.onQrCodeScanned(qrCodeValue)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Or Connect Manually")

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.manualInput.value,
            onValueChange = { viewModel.manualInput.value = it },
            label = { Text("IP Address or Relay Room ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { viewModel.connectManually() }) {
            Text("Connect")
        }
    }
}