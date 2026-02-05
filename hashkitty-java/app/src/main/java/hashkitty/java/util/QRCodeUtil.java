package hashkitty.java.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * A utility class for generating QR code images using the ZXing ("Zebra Crossing") library.
 * <p>
 * This is used primarily to generate the "Mobile Connection" QR code, which allows the Android
 * client to scan the screen and automatically configure the WebSocket connection details
 * (IP address, port, and Room ID).
 * </p>
 */
public class QRCodeUtil {

    /**
     * Generates a JavaFX Image containing a QR code for the specified text.
     *
     * @param text   The content to encode in the QR code (e.g., "ws://192.168.1.5:5001/ws?roomId=xyz").
     * @param width  The desired width of the resulting image in pixels.
     * @param height The desired height of the resulting image in pixels.
     * @return A {@link WritableImage} containing the QR code, or {@code null} if input text is empty or generation fails.
     */
    public static Image generateQRCodeImage(String text, int width, int height) {
        // Validation: Empty text cannot generate a valid QR code.
        if (text == null || text.isEmpty()) {
            return null;
        }

        try {
            // Instantiate the ZXing QR code writer.
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Encode the text into a BitMatrix (a 2D array of bits representing black/white modules).
            // BarcodeFormat.QR_CODE specifies the symbology.
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            // Create a JavaFX WritableImage to hold the pixel data.
            WritableImage writableImage = new WritableImage(width, height);
            PixelWriter pixelWriter = writableImage.getPixelWriter();

            // Iterate over the BitMatrix and write pixels to the image.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Check the bit value at (x, y). True means black (data), False means white (background).
                    // Using JavaFX Color constants.
                    pixelWriter.setColor(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return writableImage;

        } catch (WriterException e) {
            // Handle exceptions during encoding (e.g., content too large for dimensions).
            e.printStackTrace();
            return null;
        }
    }
}
