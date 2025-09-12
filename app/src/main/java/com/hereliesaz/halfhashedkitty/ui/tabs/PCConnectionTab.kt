package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.runtime.Composable
import com.hereliesaz.halfhashedkitty.MainViewModel
import com.hereliesaz.halfhashedkitty.ui.screens.ScannerScreen

@Composable
fun PCConnectionTab(viewModel: MainViewModel) {
    ScannerScreen(
        instructionText = "To connect this Android app with the desktop application, please scan the QR code displayed on the 'Connection' tab of the desktop app.",
        onQrCodeScanned = { qrCodeValue ->
            viewModel.onQrCodeScanned(qrCodeValue)
        }
    )
}
