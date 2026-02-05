package com.hereliesaz.halfhashedkitty

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Image analysis use case for CameraX that scans for QR codes using Google ML Kit.
 * <p>
 * This analyzer runs on each frame provided by the camera preview. It converts the frame
 * to an {@link InputImage} and passes it to the ML Kit Barcode Scanner.
 * </p>
 *
 * @param onQrCodeScanned Callback invoked when a QR code is successfully detected and decoded.
 */
@ExperimentalGetImage
class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Configure the scanner to look only for QR codes (performance optimization).
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    // Get an instance of the scanner client.
    private val scanner = BarcodeScanning.getClient(options)

    /**
     * Analyzes an individual frame from the camera.
     *
     * @param imageProxy The frame image data.
     */
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // Convert CameraX ImageProxy to ML Kit InputImage.
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Process the image.
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                // Check if any barcodes were found.
                if (barcodes.isNotEmpty()) {
                    // Extract the raw string value from the first detected barcode.
                    barcodes.first().rawValue?.let { onQrCodeScanned(it) }
                }
            }
            .addOnCompleteListener {
                // Important: Close the image proxy to allow the next frame to be delivered.
                imageProxy.close()
            }
    }
}
