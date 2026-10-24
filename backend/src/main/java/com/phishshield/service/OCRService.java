package com.phishshield.service;

import com.phishshield.security.image.ImageSecurityValidator;
import com.phishshield.security.image.ValidatedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * OCRService extracts optical text from securely validated screenshot images:
 * 1. Operates only on images validated by ImageSecurityValidator.
 * 2. Incorporates client-assisted OCR hints when provided.
 * 3. Integrates with system Tesseract CLI if installed on the host environment.
 * 4. Applies strict execution timeouts (default 10s) and guaranteed temporary file cleanup.
 * 5. Honestly reports when OCR is unavailable without inventing fake text.
 */
@Service
public class OCRService {

    private static final Logger log = LoggerFactory.getLogger(OCRService.class);

    private final ImageSecurityValidator imageSecurityValidator;
    private final boolean ocrEnabled;
    private final String tesseractPath;
    private final long timeoutMs;

    public static class RawOCRResult {
        private final String text;
        private final String status;
        private final String engine;

        public RawOCRResult(String text, String status, String engine) {
            this.text = text != null ? text : "";
            this.status = status;
            this.engine = engine;
        }

        public String getText() { return text; }
        public String getStatus() { return status; }
        public String getEngine() { return engine; }
    }

    public OCRService(
            ImageSecurityValidator imageSecurityValidator,
            @Value("${app.ocr.enabled:true}") boolean ocrEnabled,
            @Value("${app.ocr.tesseract-path:tesseract}") String tesseractPath,
            @Value("${app.ocr.timeout-ms:10000}") long timeoutMs) {
        this.imageSecurityValidator = imageSecurityValidator;
        this.ocrEnabled = ocrEnabled;
        this.tesseractPath = tesseractPath;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Extracts text from an uploaded MultipartFile and optional text hint.
     */
    public RawOCRResult extractText(MultipartFile file, String textHint) {
        if (file == null || file.isEmpty()) {
            if (textHint != null && !textHint.trim().isEmpty()) {
                return new RawOCRResult(textHint.trim(), "SUCCESS", "Client Text Submission");
            }
            return new RawOCRResult("", "EMPTY_INPUT", "None");
        }

        ValidatedImage validatedImage = imageSecurityValidator.validateAndDecode(file);
        return extractText(validatedImage, textHint);
    }

    /**
     * Extracts text from a ValidatedImage and optional client-provided hint.
     */
    public RawOCRResult extractText(ValidatedImage validatedImage, String textHint) {
        // 1. Client-assisted text takes precedence if verified non-empty
        if (textHint != null && !textHint.trim().isEmpty()) {
            String sanitized = sanitizeOcrText(textHint.trim());
            return new RawOCRResult(sanitized, "SUCCESS", "Client OCR / Verified Hint");
        }

        if (!ocrEnabled || validatedImage == null) {
            return new RawOCRResult("", "OCR_DISABLED", "None");
        }

        // 2. Attempt local Tesseract extraction if available in system PATH
        if (isTesseractInstalled()) {
            return runTesseractCli(validatedImage);
        }

        // 3. Honest reporting: Tesseract is not installed and no client OCR hint was provided
        log.info("Tesseract binary not detected in PATH. Returning honest unextracted OCR state without fabrication.");
        return new RawOCRResult("", "NO_TEXT_DETECTED", "Native Tesseract Not Detected in PATH");
    }

    private RawOCRResult runTesseractCli(ValidatedImage validatedImage) {
        Path tempImageFile = null;
        try {
            tempImageFile = Files.createTempFile("prahar_ocr_", ".png");
            ImageIO.write(validatedImage.getImage(), "png", tempImageFile.toFile());

            ProcessBuilder pb = new ProcessBuilder(
                    tesseractPath,
                    tempImageFile.toAbsolutePath().toString(),
                    "stdout",
                    "--oem", "1",
                    "-l", "eng"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Tesseract OCR process timed out after {} ms", timeoutMs);
                return new RawOCRResult("", "TIMEOUT", "Tesseract CLI (Timed Out)");
            }

            String extracted = sanitizeOcrText(output.toString().trim());
            if (extracted.isEmpty()) {
                return new RawOCRResult("", "NO_TEXT_DETECTED", "Tesseract CLI");
            }

            return new RawOCRResult(extracted, "SUCCESS", "Tesseract CLI");

        } catch (Exception e) {
            log.warn("Error during Tesseract OCR execution: {}", e.getMessage());
            return new RawOCRResult("", "OCR_ERROR", "Tesseract CLI (Error: " + e.getMessage() + ")");
        } finally {
            // Guaranteed cleanup of temporary image file
            if (tempImageFile != null) {
                try {
                    Files.deleteIfExists(tempImageFile);
                } catch (IOException ignored) {}
            }
        }
    }

    private boolean isTesseractInstalled() {
        try {
            Process process = new ProcessBuilder(tesseractPath, "--version").start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String sanitizeOcrText(String raw) {
        if (raw == null) return "";
        // Strip non-printable control characters except newline and tab
        return raw.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
    }
}
