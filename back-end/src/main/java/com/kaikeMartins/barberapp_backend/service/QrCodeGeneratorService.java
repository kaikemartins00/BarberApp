
package com.kaikeMartins.barberapp_backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
public class QrCodeGeneratorService {

    public String gerarBase64(String conteudo) {

        try {
            QRCodeWriter writer = new QRCodeWriter();

            BitMatrix matrix = writer.encode(
                    conteudo,
                    BarcodeFormat.QR_CODE,
                    350,
                    350,
                    Map.of(EncodeHintType.MARGIN, 2)
            );

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(matrix, "PNG", output);

            return Base64.getEncoder().encodeToString(output.toByteArray());

        } catch (WriterException | IOException e) {throw new IllegalStateException(
                "Não foi possível gerar o QR Code Pix.", e);
        }
    }
}
