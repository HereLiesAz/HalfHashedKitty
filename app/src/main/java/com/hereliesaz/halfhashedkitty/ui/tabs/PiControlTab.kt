package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.runtime.Composable
import com.hereliesaz.halfhashedkitty.PiControlViewModel
import com.hereliesaz.halfhashedkitty.ui.screens.ScannerScreen

@Composable
fun PiControlTab(piControlViewModel: PiControlViewModel) {
    ScannerScreen(onQrCodeScanned = { qrCodeValue ->
        piControlViewModel.onQrCodeScanned(qrCodeValue)
    })
}
