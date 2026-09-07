package com.server.server.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class BarcodeService {

    public String generateQRCodeBase64(String text) {
        return generateBase64(text, BarcodeFormat.QR_CODE, 300, 300);
    }

    public String generateBarcodeBase64(String text) {
        return generateBase64(text, BarcodeFormat.CODE_128, 400, 100);
    }

    private String generateBase64(String text, BarcodeFormat format, int width, int height) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, format, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error generating barcode/QR", e);
        }
    }

    /**
     * Reads a QR Code or Barcode from a Base64 image string.
     */
    public String readBarcodeFromBase64(String base64Image) {
        try {
            // Strip the data URI prefix if it came directly from a web frontend
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage bufferedImage = ImageIO.read(inputStream);

            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // MultiFormatReader automatically detects if it's a QR code or Barcode
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (Exception e) {
            throw new RuntimeException("Could not decode the provided image. Ensure the QR/Barcode is clearly visible.", e);
        }
    }
}