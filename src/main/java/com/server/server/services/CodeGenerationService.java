package com.server.server.services;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;

@Service
public class CodeGenerationService {

    public String generateQRCode(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return convertToBase64(bitMatrix);
    }

    public String generateBarcode(String text, int width, int height) throws Exception {
        Code128Writer barcodeWriter = new Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.CODE_128, width, height);
        return convertToBase64(bitMatrix);
    }

    private String convertToBase64(BitMatrix bitMatrix) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        byte[] pngData = outputStream.toByteArray();
        // Prefixed so front-end can drop it directly into <img src="..." />
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }
}