package com.busbooking.bus_ticket_management.utils;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component
public class QRCodeGenerator {
    public byte[] png(String text) {
        try {
            var matrix =
                    new MultiFormatWriter()
                            .encode(
                                    text,
                                    BarcodeFormat.QR_CODE,
                                    260,
                                    260,
                                    Map.of(
                                            EncodeHintType.CHARACTER_SET,
                                            "UTF-8",
                                            EncodeHintType.MARGIN,
                                            2));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Không tạo được mã QR.", ex);
        }
    }
}
