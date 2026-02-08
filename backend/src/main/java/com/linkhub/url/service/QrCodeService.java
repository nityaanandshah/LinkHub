package com.linkhub.url.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class QrCodeService {

    private static final int DEFAULT_SIZE = 300;

    /**
     * Generate a QR code PNG image as a byte array.
     *
     * @param url  the URL to encode in the QR code
     * @param size the width/height in pixels (square)
     * @return PNG image bytes
     */
    public byte[] generateQrCode(String url, int size) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN, 2,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );

            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, size, size, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code for URL: " + url, e);
        }
    }

    public byte[] generateQrCode(String url) {
        return generateQrCode(url, DEFAULT_SIZE);
    }
}
