package com.phishshield.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class QRCodeService {

    private static final Logger log = LoggerFactory.getLogger(QRCodeService.class);

    /**
     * Decodes QR Code content from uploaded MultipartFile
     * @param file uploaded image
     * @return decoded text/URL or null if no QR found
     */
    public String decodeQRCode(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try (InputStream is = file.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(is);
            if (bufferedImage == null) {
                log.warn("Uploaded QR image could not be decoded as a valid BufferedImage");
                return null;
            }

            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.Collections.singletonList(BarcodeFormat.QR_CODE));

            Result result = new MultiFormatReader().decode(bitmap, hints);
            if (result != null && result.getText() != null) {
                return result.getText().trim();
            }
        } catch (NotFoundException e) {
            log.debug("No QR code detected in image: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error while decoding QR code: {}", e.getMessage());
        }

        return null;
    }
}
