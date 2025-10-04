package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
        ScannerScreen(
            instructionText = "To connect this Android app with the desktop application, please scan the QR code displayed on the 'Connection' tab of the desktop app.",
            onQrCodeScanned = { qrCodeValue ->
                viewModel.onQrCodeScanned(qrCodeValue)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Or Connect Manually")

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.manualInput.value,
            onValueChange = { viewModel.manualInput.value = it },
            label = { Text("IP Address or Relay Room ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connection Type:")
            Spacer(modifier = Modifier.weight(1f))
            RadioButton(
                selected = viewModel.connectionType.value == MainViewModel.ConnectionType.RELAY,
                onClick = { viewModel.connectionType.value = MainViewModel.ConnectionType.RELAY }
            )
            Text("Relay")
            RadioButton(
                selected = viewModel.connectionType.value == MainViewModel.ConnectionType.DIRECT,
                onClick = { viewModel.connectionType.value = MainViewModel.ConnectionType.DIRECT }
            )
            Text("Direct")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { viewModel.connectManually() }) {
            Text("Connect")
        }
    }
}