package com.phishshield.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.phishshield.dto.response.QRCodeItemResponse;
import com.phishshield.dto.response.QRPayloadType;
import com.phishshield.security.image.ImageSecurityValidator;
import com.phishshield.security.image.ValidatedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.*;

/**
 * QRCodeService performs secure decoding and structural analysis of QR code images:
 * 1. Delegates to ImageSecurityValidator to ensure zero spoofed binaries or decompression bombs.
 * 2. Uses ZXing QRCodeMultiReader + MultiFormatReader to extract single and multiple QR codes.
 * 3. Classifies payloads (HTTP/HTTPS, Dangerous Schemes, UPI, vCard, Plain Text).
 * 4. Normalizes and validates URLs while rejecting dangerous schemes (javascript:, data:, file:).
 */
@Service
public class QRCodeService {

    private static final Logger log = LoggerFactory.getLogger(QRCodeService.class);
    private static final int MAX_QR_CODES_PER_IMAGE = 10;
    private static final int MAX_DISPLAY_LENGTH = 120;

    private final ImageSecurityValidator imageSecurityValidator;

    public QRCodeService(ImageSecurityValidator imageSecurityValidator) {
        this.imageSecurityValidator = imageSecurityValidator;
    }

    /**
     * Decodes all QR codes in an uploaded MultipartFile after performing strict security validation.
     *
     * @param file uploaded image
     * @return list of decoded raw payloads (up to MAX_QR_CODES_PER_IMAGE)
     */
    public List<String> decodeAllQRCodes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Collections.emptyList();
        }

        ValidatedImage validatedImage = imageSecurityValidator.validateAndDecode(file);
        return decodeAllQRCodes(validatedImage.getImage());
    }

    /**
     * Decodes all QR codes from a validated BufferedImage.
     *
     * @param image validated BufferedImage
     * @return list of raw payload strings
     */
    public List<String> decodeAllQRCodes(BufferedImage image) {
        if (image == null) {
            return Collections.emptyList();
        }

        List<String> rawPayloads = new ArrayList<>();
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));

        // 1. Try QRCodeMultiReader to detect multiple QR codes across the image canvas
        try {
            QRCodeMultiReader multiReader = new QRCodeMultiReader();
            Result[] multiResults = multiReader.decodeMultiple(bitmap, hints);
            if (multiResults != null && multiResults.length > 0) {
                for (Result r : multiResults) {
                    if (r != null && r.getText() != null) {
                        String text = r.getText().trim();
                        if (!text.isEmpty() && !rawPayloads.contains(text)) {
                            rawPayloads.add(text);
                        }
                    }
                }
            }
        } catch (NotFoundException ignored) {
            // Normal: image may have only one QR code or multi-finder didn't group
        } catch (Exception e) {
            log.debug("Multi-QR reading notice: {}", e.getMessage());
        }

        // 2. Fallback to MultiFormatReader if QRCodeMultiReader found nothing
        if (rawPayloads.isEmpty()) {
            try {
                MultiFormatReader singleReader = new MultiFormatReader();
                Result singleResult = singleReader.decode(bitmap, hints);
                if (singleResult != null && singleResult.getText() != null) {
                    String text = singleResult.getText().trim();
                    if (!text.isEmpty()) {
                        rawPayloads.add(text);
                    }
                }
            } catch (NotFoundException ignored) {
                // No QR found in standard binarization
            } catch (Exception e) {
                log.debug("Single QR reading notice: {}", e.getMessage());
            }
        }

        // 3. Fallback to inverted luminance binarizer (for dark-theme / inverted QR codes)
        if (rawPayloads.isEmpty()) {
            try {
                BinaryBitmap invertedBitmap = new BinaryBitmap(new HybridBinarizer(source.invert()));
                MultiFormatReader singleReader = new MultiFormatReader();
                Result invertedResult = singleReader.decode(invertedBitmap, hints);
                if (invertedResult != null && invertedResult.getText() != null) {
                    String text = invertedResult.getText().trim();
                    if (!text.isEmpty()) {
                        rawPayloads.add(text);
                    }
                }
            } catch (Exception ignored) {
                // No QR found in inverted mode
            }
        }

        // Guard against processing explosion (limit to max 10 QR codes per image)
        if (rawPayloads.size() > MAX_QR_CODES_PER_IMAGE) {
            log.warn("Decoded {} QR codes, truncating to maximum allowed limit of {}", rawPayloads.size(), MAX_QR_CODES_PER_IMAGE);
            return new ArrayList<>(rawPayloads.subList(0, MAX_QR_CODES_PER_IMAGE));
        }

        return rawPayloads;
    }

    /**
     * Decodes the first QR Code content (for single-item backward compatibility).
     */
    public String decodeQRCode(MultipartFile file) {
        List<String> results = decodeAllQRCodes(file);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Classifies a decoded raw QR payload and verifies destination URLs.
     *
     * @param rawPayload raw text extracted from QR code
     * @param index ordinal index (1-based)
     * @return QRCodeItemResponse with classification, safe display, and extracted destination
     */
    public QRCodeItemResponse processAndClassifyPayload(String rawPayload, int index) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            return QRCodeItemResponse.builder()
                    .index(index)
                    .rawPayload("")
                    .safeDisplayPayload("(Empty payload)")
                    .payloadType(QRPayloadType.PLAIN_TEXT)
                    .scannableUrl(false)
                    .scanStatus("EMPTY")
                    .build();
        }

        String trimmed = rawPayload.trim();
        String safeDisplay = createSafeDisplayString(trimmed);
        String lower = trimmed.toLowerCase();

        // 1. Dangerous URI Scheme Check (javascript:, data:, file:, vbscript:, blob:, about:)
        if (isDangerousScheme(lower)) {
            String scheme = lower.substring(0, lower.indexOf(':') + 1);
            return QRCodeItemResponse.builder()
                    .index(index)
                    .rawPayload(trimmed)
                    .safeDisplayPayload(safeDisplay)
                    .payloadType(QRPayloadType.URL_DANGEROUS_SCHEME)
                    .scannableUrl(false)
                    .validationError("Dangerous URI scheme detected (" + scheme + "). Script execution or local file access blocked.")
                    .scanStatus("BLOCKED_DANGEROUS_SCHEME")
                    .build();
        }

        // 2. UPI Payment Intent Check (upi://pay?...)
        if (lower.startsWith("upi://")) {
            return QRCodeItemResponse.builder()
                    .index(index)
                    .rawPayload(trimmed)
                    .safeDisplayPayload(safeDisplay)
                    .payloadType(QRPayloadType.UPI_PAYMENT)
                    .scannableUrl(false)
                    .scanStatus("IDENTIFIED_UPI_PAYMENT")
                    .build();
        }

        // 3. Electronic Contact Card (vCard)
        if (trimmed.toUpperCase().startsWith("BEGIN:VCARD")) {
            return QRCodeItemResponse.builder()
                    .index(index)
                    .rawPayload(trimmed)
                    .safeDisplayPayload(safeDisplay)
                    .payloadType(QRPayloadType.VCARD)
                    .scannableUrl(false)
                    .scanStatus("IDENTIFIED_VCARD")
                    .build();
        }

        // 4. Standard Web URLs (http:// or https://)
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return validateWebUrl(trimmed, safeDisplay, index);
        }

        // 5. Unsupported URI Schemes (e.g. ftp://, tel:, mailto:, ws:)
        if (trimmed.contains("://") || (trimmed.indexOf(':') > 0 && trimmed.indexOf(':') < 10 && trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*"))) {
            String scheme = trimmed.substring(0, trimmed.indexOf(':') + 1);
            return QRCodeItemResponse.builder()
                    .index(index)
                    .rawPayload(trimmed)
                    .safeDisplayPayload(safeDisplay)
                    .payloadType(QRPayloadType.URL_UNSUPPORTED_SCHEME)
                    .scannableUrl(false)
                    .validationError("Unsupported URI scheme (" + scheme + "). Prahar scanner supports HTTP and HTTPS web destinations.")
                    .scanStatus("UNSUPPORTED_SCHEME")
                    .build();
        }

        // 6. Bare domain without protocol (e.g. "www.example.com/login" or "secure-bank.tk")
        if (trimmed.matches("^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/[^\\s]*)?$")) {
            String normalizedUrl = "https://" + trimmed;
            return validateWebUrl(normalizedUrl, safeDisplay, index);
        }

        // 7. General Plain Text (Coupon code, arbitrary alphanumeric message, crypto address)
        return QRCodeItemResponse.builder()
                .index(index)
                .rawPayload(trimmed)
                .safeDisplayPayload(safeDisplay)
                .payloadType(QRPayloadType.PLAIN_TEXT)
                .scannableUrl(false)
                .scanStatus("INFORMATIONAL_TEXT")
                .build();
    }

    private QRCodeItemResponse validateWebUrl(String urlString, String safeDisplay, int index) {
        // Check for deceptive URL characteristics before strict URI parsing
        if (urlString.contains("\\")) {
            return QRCodeItemResponse.builder()
                    .index(index)
                    .rawPayload(urlString)
                    .safeDisplayPayload(safeDisplay)
                    .payloadType(QRPayloadType.MALFORMED_URL)
                    .scannableUrl(false)
                    .validationError("Deceptive backslash character detected in URL.")
                    .scanStatus("REJECTED_DECEPTIVE")
                    .build();
        }

        try {
            URI uri = new URI(urlString);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return QRCodeItemResponse.builder()
                        .index(index)
                        .rawPayload(urlString)
                        .safeDisplayPayload(safeDisplay)
                        .payloadType(QRPayloadType.URL_UNSUPPORTED_SCHEME)
                        .scannableUrl(false)
                        .validationError("Unsupported scheme: " + scheme)
                        .scanStatus("UNSUPPORTED_SCHEME")
                        .build();
            }

            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                return QRCodeItemResponse.builder()
                        .index(index)
                        .rawPayload(urlString)
                        .safeDisplayPayload(safeDisplay)
                        .payloadType(QRPayloadType.MALFORMED_URL)
                        .scannableUrl(false)
                        .validationError("Invalid or missing hostname in URL.")
                        .scanStatus("REJECTED_MALFORMED")
                        .build();
            }

            return QRCodeItemResponse.builder()
                    .index(index)
                    .rawPayload(urlString)
                    .safeDisplayPayload(safeDisplay)
                    .payloadType(QRPayloadType.URL_HTTP_HTTPS)
                    .extractedUrl(urlString)
                    .scannableUrl(true)
                    .scanStatus("PENDING_SCAN")
                    .build();

        } catch (Exception e) {
            return QRCodeItemResponse.builder()
                    .index(index)
                    .rawPayload(urlString)
                    .safeDisplayPayload(safeDisplay)
                    .payloadType(QRPayloadType.MALFORMED_URL)
                    .scannableUrl(false)
                    .validationError("Malformed URL syntax: " + e.getMessage())
                    .scanStatus("REJECTED_MALFORMED")
                    .build();
        }
    }

    private boolean isDangerousScheme(String lower) {
        return lower.startsWith("javascript:") ||
                lower.startsWith("data:") ||
                lower.startsWith("file:") ||
                lower.startsWith("vbscript:") ||
                lower.startsWith("blob:") ||
                lower.startsWith("about:");
    }

    private String createSafeDisplayString(String text) {
        if (text == null) return "";
        // Strip control characters
        String sanitized = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        if (sanitized.length() > MAX_DISPLAY_LENGTH) {
            return sanitized.substring(0, MAX_DISPLAY_LENGTH - 3) + "...";
        }
        return sanitized;
    }
}
