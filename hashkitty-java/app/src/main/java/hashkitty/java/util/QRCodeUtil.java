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
 * A utility class for generating QR code images using the ZXing library.
 */
public class QRCodeUtil {

    /**
     * Generates a QR code image from a given text.
     *
     * @param text  The text to encode in the QR code.
     * @param width The desired width of the QR code image.
     * @param height The desired height of the QR code image.
     * @return A JavaFX Image object representing the QR code, or null on failure.
     */
    public static Image generateQRCodeImage(String text, int width, int height) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            WritableImage writableImage = new WritableImage(width, height);
            PixelWriter pixelWriter = writableImage.getPixelWriter();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixelWriter.setColor(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return writableImage;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}