package com.phishshield.security.image;

import com.phishshield.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ImageSecurityValidator enforces strict security controls on uploaded image assets:
 * 1. File size enforcement (default 10 MB limit).
 * 2. Magic byte / file header signature verification (rejects MIME-spoofed files and scripts).
 * 3. Actual image decoding integrity check (rejects corrupted, truncated, or malformed binaries).
 * 4. Image dimension and pixel-bomb / decompression-bomb bounds (default max 4096x4096px / 16 MP).
 * 5. Safe in-memory stream processing without lingering temporary files or disk leaks.
 */
@Component
public class ImageSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(ImageSecurityValidator.class);

    // PNG signature: 89 50 4E 47 0D 0A 1A 0A
    private static final byte[] PNG_SIGNATURE = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    // JPEG signature: FF D8 FF
    private static final byte[] JPEG_SIGNATURE = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    // GIF signature: 47 49 46 38 ("GIF8")
    private static final byte[] GIF_SIGNATURE = new byte[]{0x47, 0x49, 0x46, 0x38};

    // BMP signature: 42 4D ("BM")
    private static final byte[] BMP_SIGNATURE = new byte[]{0x42, 0x4D};

    // WebP signature: RIFF at byte 0..3 and WEBP at byte 8..11
    private static final byte[] WEBP_RIFF = new byte[]{0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] WEBP_TAG = new byte[]{0x57, 0x45, 0x42, 0x50};  // "WEBP"

    private final long maxFileSizeBytes;
    private final int maxWidth;
    private final int maxHeight;
    private final long maxPixelCount;
    private final Set<String> allowedFormats;

    public ImageSecurityValidator(
            @Value("${app.upload.max-file-size-bytes:10485760}") long maxFileSizeBytes, // 10 MB
            @Value("${app.upload.max-width:4096}") int maxWidth,
            @Value("${app.upload.max-height:4096}") int maxHeight,
            @Value("${app.upload.max-pixels:16777216}") long maxPixelCount, // 16 Megapixels
            @Value("${app.upload.allowed-formats:PNG,JPEG,JPG,WEBP,GIF,BMP}") String allowedFormatsConfig) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxPixelCount = maxPixelCount;

        this.allowedFormats = new HashSet<>();
        for (String fmt : allowedFormatsConfig.split(",")) {
            String trimmed = fmt.trim().toUpperCase();
            if (!trimmed.isEmpty()) {
                this.allowedFormats.add(trimmed);
            }
        }
    }

    /**
     * Validates an uploaded MultipartFile and safely decodes it into a ValidatedImage.
     *
     * @param file uploaded multipart file
     * @return ValidatedImage containing the decoded BufferedImage and verified metadata
     * @throws BadRequestException if the file is empty, oversized, spoofed, malformed, or exceeds dimension limits
     */
    public ValidatedImage validateAndDecode(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new BadRequestException("No image file was provided or uploaded file is empty.");
        }

        // 1. File Size Verification
        if (file.getSize() > maxFileSizeBytes) {
            long maxMb = maxFileSizeBytes / (1024 * 1024);
            throw new BadRequestException("Image file size (" + file.getSize() + " bytes) exceeds maximum permitted limit of " + maxMb + "MB.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.warn("Failed to read image upload bytes: {}", e.getMessage());
            throw new BadRequestException("Failed to read uploaded image data.");
        }

        return validateAndDecode(bytes, file.getOriginalFilename());
    }

    /**
     * Validates raw bytes and decodes into a ValidatedImage.
     *
     * @param bytes raw file bytes
     * @param originalFilename original name for reference
     * @return ValidatedImage
     */
    public ValidatedImage validateAndDecode(byte[] bytes, String originalFilename) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Uploaded file contains zero bytes.");
        }

        if (bytes.length > maxFileSizeBytes) {
            long maxMb = maxFileSizeBytes / (1024 * 1024);
            throw new BadRequestException("Image file size (" + bytes.length + " bytes) exceeds maximum permitted limit of " + maxMb + "MB.");
        }

        // 2. Magic Byte Signature Verification (Do not trust extensions or Content-Type)
        ImageFormat detectedFormat = detectFormatFromMagicBytes(bytes);
        if (detectedFormat == ImageFormat.UNKNOWN) {
            log.warn("Rejected uploaded file: unrecognized magic byte signature for file '{}'", sanitizeFilename(originalFilename));
            throw new BadRequestException("Unsupported or unrecognized file format. Uploaded file signature does not match supported image types (PNG, JPEG, WebP, GIF, BMP).");
        }

        if (!isFormatAllowed(detectedFormat)) {
            log.warn("Rejected uploaded file with format {}: not in allowed formats list", detectedFormat);
            throw new BadRequestException("Image format " + detectedFormat + " is not permitted by system upload policy.");
        }

        // 3. Decoding and Integrity Check
        BufferedImage image;
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            image = ImageIO.read(is);
        } catch (Exception e) {
            log.warn("Exception decoding image data: {}", e.getMessage());
            throw new BadRequestException("Corrupted or malformed image data. The file could not be parsed as a valid image.");
        }

        if (image == null) {
            // Check if format is WebP without an installed SPI plugin
            if (detectedFormat == ImageFormat.WEBP && !ImageIO.getImageReadersByFormatName("webp").hasNext()) {
                throw new BadRequestException("WebP image format is recognized but not supported by the current JVM ImageIO runtime without an imageio-webp SPI plugin. Please upload as PNG or JPEG.");
            }
            throw new BadRequestException("Corrupted or malformed image data. The file could not be parsed as a valid image.");
        }

        // 4. Dimension & Decompression Bomb (Pixel Bomb) Verification
        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= 0 || height <= 0) {
            throw new BadRequestException("Invalid image dimensions detected: " + width + "x" + height);
        }

        if (width > maxWidth || height > maxHeight) {
            throw new BadRequestException("Image dimensions (" + width + "x" + height + ") exceed the maximum allowed limits (" + maxWidth + "x" + maxHeight + " pixels).");
        }

        long totalPixels = (long) width * height;
        if (totalPixels > maxPixelCount) {
            throw new BadRequestException("Total pixel count (" + totalPixels + ") exceeds safe processing threshold (" + maxPixelCount + " pixels). Potential decompression bomb detected.");
        }

        log.debug("Successfully validated image: format={}, dimensions={}x{}, size={} bytes", detectedFormat, width, height, bytes.length);
        return new ValidatedImage(image, detectedFormat, width, height, bytes.length, sanitizeFilename(originalFilename));
    }

    /**
     * Inspects magic byte signatures at the head of the byte array.
     */
    public ImageFormat detectFormatFromMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return ImageFormat.UNKNOWN;
        }

        // Check PNG (8 bytes)
        if (bytes.length >= PNG_SIGNATURE.length && matchesSignature(bytes, 0, PNG_SIGNATURE)) {
            return ImageFormat.PNG;
        }

        // Check JPEG (3 bytes)
        if (matchesSignature(bytes, 0, JPEG_SIGNATURE)) {
            return ImageFormat.JPEG;
        }

        // Check GIF (4 bytes)
        if (matchesSignature(bytes, 0, GIF_SIGNATURE)) {
            return ImageFormat.GIF;
        }

        // Check BMP (2 bytes)
        if (matchesSignature(bytes, 0, BMP_SIGNATURE)) {
            return ImageFormat.BMP;
        }

        // Check WebP (RIFF....WEBP)
        if (bytes.length >= 12 && matchesSignature(bytes, 0, WEBP_RIFF) && matchesSignature(bytes, 8, WEBP_TAG)) {
            return ImageFormat.WEBP;
        }

        return ImageFormat.UNKNOWN;
    }

    private boolean matchesSignature(byte[] data, int offset, byte[] signature) {
        if (data.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isFormatAllowed(ImageFormat format) {
        if (allowedFormats.contains(format.name())) {
            return true;
        }
        // Normalize JPG/JPEG
        if (format == ImageFormat.JPEG && (allowedFormats.contains("JPG") || allowedFormats.contains("JPEG"))) {
            return true;
        }
        return false;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown";
        // Strip path traversal sequences and special control chars
        String cleaned = filename.replace("\\", "/");
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash != -1) {
            cleaned = cleaned.substring(lastSlash + 1);
        }
        return cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public long getMaxPixelCount() {
        return maxPixelCount;
    }

    public Set<String> getAllowedFormats() {
        return allowedFormats;
    }
}
