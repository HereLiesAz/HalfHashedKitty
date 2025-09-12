package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.runtime.Composable
import com.hereliesaz.halfhashedkitty.MainViewModel
import com.hereliesaz.halfhashedkitty.ui.screens.ScannerScreen

@Composable
fun PCConnectionTab(viewModel: MainViewModel) {
    ScannerScreen(
        instructionText = "Scan the QR code from the desktop application to connect.",
        onQrCodeScanned = { qrCodeValue ->
            viewModel.onQrCodeScanned(qrCodeValue)
        }
    )
}

private fun MainViewModel.onQrCodeScanned(qrCodeValue: String) {}
