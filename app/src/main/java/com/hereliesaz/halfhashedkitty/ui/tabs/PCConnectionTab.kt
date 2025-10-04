package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.halfhashedkitty.MainViewModel
import com.hereliesaz.halfhashedkitty.ui.screens.ScannerScreen

@Composable
fun PCConnectionTab(viewModel: MainViewModel, onShowInstructions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ScreenTitle("PC Connect", onShowInstructions)
        ScannerScreen(
            instructionText = "To connect this Android app with the desktop application, please scan the QR code displayed on the 'Connection' tab of the desktop app.",
            onQrCodeScanned = { qrCodeValue ->
                viewModel.onQrCodeScanned(qrCodeValue)
            }
        )
    }
}