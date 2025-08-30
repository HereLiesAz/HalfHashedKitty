package com.hereliesaz.halfhashedkitty

import android.util.Log
import androidx.lifecycle.ViewModel

class PiControlViewModel : ViewModel() {

    fun onQrCodeScanned(qrCodeValue: String) {
        // For now, just log the scanned value
        Log.d("PiControlViewModel", "Scanned QR Code: $qrCodeValue")
    }
}
