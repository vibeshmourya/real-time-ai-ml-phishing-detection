package com.phishshield.service;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ScanType;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.dto.response.*;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.SSRFSecurityException;
import com.phishshield.security.image.ImageSecurityValidator;
import com.phishshield.security.image.ValidatedImage;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UnifiedImageAnalysisService orchestrates the multi-vector Prahar analysis pipeline:
 * 1. Image security validation (file signature, magic bytes, dimensions, pixel bounds, decompression bombs).
 * 2. Optical QR code decoding, payload classification, and dangerous scheme interception.
 * 3. Optical Character Recognition (OCR) text extraction, engine diagnosis, and heuristic indicator scoring.
 * 4. URL candidate extraction across QR and OCR with source attribution and provenance tracking.
 * 5. Deduplication, scheme enforcement, SSRF protection, and resource bounding.
 * 6. Scanning of candidate URLs via Prahar's real detection pipeline (heuristics, ML, threat intelligence).
 * 7. Failure isolation across all components with honest reporting of partial degradation.
 */
@Service
public class UnifiedImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedImageAnalysisService.class);

    private static final int MAX_CANDIDATE_URLS = 5;
    private static final Pattern URL_REGEX = Pattern.compile("(?i)\\bhttps?://[^\\s<>\"'{}|\\\\^`]+");

    private final ImageSecurityValidator imageSecurityValidator;
    private final QRCodeService qrCodeService;
    private final OCRService ocrService;
    private final ScreenshotTextAnalyzer screenshotTextAnalyzer;
    private final URLScanService urlScanService;
    private final SSRFProtectionService ssrfProtectionService;
    private final MLServiceClient mlServiceClient;

    public UnifiedImageAnalysisService(ImageSecurityValidator imageSecurityValidator,
                                      QRCodeService qrCodeService,
                                      OCRService ocrService,
                                      ScreenshotTextAnalyzer screenshotTextAnalyzer,
                                      URLScanService urlScanService,
                                      SSRFProtectionService ssrfProtectionService,
                                      MLServiceClient mlServiceClient) {
        this.imageSecurityValidator = imageSecurityValidator;
        this.qrCodeService = qrCodeService;
        this.ocrService = ocrService;
        this.screenshotTextAnalyzer = screenshotTextAnalyzer;
        this.urlScanService = urlScanService;
        this.ssrfProtectionService = ssrfProtectionService;
        this.mlServiceClient = mlServiceClient;
    }

    /**
     * Executes the unified multi-vector image analysis pipeline.
     *
     * @param file uploaded screenshot or image file
     * @param textHint optional client-assisted OCR text hint
     * @param httpRequest HTTP request for client IP and session tracking
     * @return UnifiedImageAnalysisResponse containing comprehensive evidence and verdicts
     */
    @Transactional
    public UnifiedImageAnalysisResponse analyzeImage(MultipartFile file, String textHint, HttpServletRequest httpRequest) {
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = textHint != null && !textHint.trim().isEmpty();

        if (!hasFile && !hasText) {
            throw new BadRequestException("Neither image file nor text hint was provided for analysis.");
        }

        Map<String, String> componentErrors = new HashMap<>();
        List<String> limitations = new ArrayList<>();
        boolean isPartialFailure = false;

        // -------------------------------------------------------------
        // Step 1: Image Security Validation
        // -------------------------------------------------------------
        ValidatedImage validatedImage = null;
        ImageValidationMetadata imageValidation;

        if (hasFile) {
            try {
                validatedImage = imageSecurityValidator.validateAndDecode(file);
                imageValidation = ImageValidationMetadata.valid(
                        validatedImage.getFormat().name(),
                        validatedImage.getWidth(),
                        validatedImage.getHeight(),
                        validatedImage.getSizeBytes(),
                        validatedImage.getOriginalFilename()
                );
            } catch (Exception e) {
                log.warn("Image security validation failed: {}", e.getMessage());
                throw e; // Rethrow bad request or validation exception for client rejection
            }
        } else {
            imageValidation = new ImageValidationMetadata(true, "TEXT_ONLY", 0, 0, 0L, "text_input", null);
        }

        // -------------------------------------------------------------
        // Step 2: Optical QR Code Decoding (Failure-Isolated)
        // -------------------------------------------------------------
        QRCodeAnalysisResponse qrAnalysis = null;
        List<QRCodeItemResponse> qrItems = new ArrayList<>();

        if (validatedImage != null) {
            try {
                List<String> rawQrs = qrCodeService.decodeAllQRCodes(validatedImage.getImage());
                if (rawQrs.isEmpty()) {
                    qrAnalysis = QRCodeAnalysisResponse.builder()
                            .totalQrCodesFound(0)
                            .qrCodes(new ArrayList<>())
                            .status("NO_QR_DETECTED")
                            .message("No optical QR codes detected in the image canvas.")
                            .build();
                } else {
                    for (int i = 0; i < rawQrs.size(); i++) {
                        QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(rawQrs.get(i), i + 1);
                        qrItems.add(item);
                    }
                    qrAnalysis = QRCodeAnalysisResponse.builder()
                            .totalQrCodesFound(qrItems.size())
                            .qrCodes(qrItems)
                            .status("SUCCESS")
                            .message("Successfully detected and decoded " + qrItems.size() + " QR code(s).")
                            .build();
                }
            } catch (Exception qrEx) {
                log.warn("QR code decoding failure on canvas: {}", qrEx.getMessage());
                componentErrors.put("qrDecoder", "QR decoding encountered an error: " + qrEx.getMessage());
                qrAnalysis = QRCodeAnalysisResponse.builder()
                        .totalQrCodesFound(0)
                        .qrCodes(new ArrayList<>())
                        .status("ERROR")
                        .message("QR decoding failure: " + qrEx.getMessage())
                        .build();
                isPartialFailure = true;
            }
        }

        // -------------------------------------------------------------
        // Step 3: Optical Character Recognition (OCR) (Failure-Isolated)
        // -------------------------------------------------------------
        OCRTextAnalysisResult ocrAnalysis = null;
        String ocrExtractedText = "";

        try {
            OCRService.RawOCRResult ocrRaw = ocrService.extractText(validatedImage, textHint);
            ocrExtractedText = ocrRaw.getText() != null ? ocrRaw.getText() : "";
            ocrAnalysis = screenshotTextAnalyzer.analyzeText(ocrExtractedText, ocrRaw.getStatus(), ocrRaw.getEngine());

            if ("ERROR".equalsIgnoreCase(ocrRaw.getStatus())) {
                componentErrors.put("ocrEngine", "OCR extraction error: " + ocrRaw.getStatus());
                isPartialFailure = true;
            } else if ("Tesseract Not Detected in PATH".equalsIgnoreCase(ocrRaw.getStatus())) {
                limitations.add("Local Tesseract OCR binary was not detected on host PATH; visual analysis relied on client hints.");
            }
        } catch (Exception ocrEx) {
            log.warn("OCR service processing failure: {}", ocrEx.getMessage());
            componentErrors.put("ocrEngine", "OCR text extraction failed: " + ocrEx.getMessage());
            ocrAnalysis = screenshotTextAnalyzer.analyzeText("", "ERROR", "None");
            isPartialFailure = true;
        }

        // -------------------------------------------------------------
        // Step 4: Extract URL Candidates from Decoded Payloads and OCR Text
        // -------------------------------------------------------------
        Map<String, UnifiedUrlCandidateResponse> candidateMap = new LinkedHashMap<>();

        // 4a. Candidates from QR codes
        for (QRCodeItemResponse qrItem : qrItems) {
            if (qrItem.isScannableUrl() && qrItem.getExtractedUrl() != null) {
                String candidateUrl = qrItem.getExtractedUrl().trim();
                String key = normalizeCandidateKey(candidateUrl);
                UnifiedUrlCandidateResponse candidate = UnifiedUrlCandidateResponse.builder()
                        .url(candidateUrl)
                        .source(CandidateUrlSource.QR)
                        .scannable(true)
                        .scanStatus("PENDING")
                        .build();
                candidateMap.put(key, candidate);
            } else if (qrItem.getPayloadType() == QRPayloadType.URL_DANGEROUS_SCHEME) {
                String candidateUrl = qrItem.getRawPayload();
                String key = normalizeCandidateKey(candidateUrl);
                UnifiedUrlCandidateResponse candidate = UnifiedUrlCandidateResponse.builder()
                        .url(candidateUrl)
                        .source(CandidateUrlSource.QR)
                        .scannable(false)
                        .scanStatus("BLOCKED_DANGEROUS_SCHEME")
                        .validationError(qrItem.getValidationError())
                        .build();
                candidateMap.put(key, candidate);
            }
        }

        // 4b. Candidates from OCR text
        List<String> ocrUrls = extractUrlsFromText(ocrExtractedText);
        for (String ocrUrl : ocrUrls) {
            String candidateUrl = ocrUrl.trim();
            String key = normalizeCandidateKey(candidateUrl);
            if (candidateMap.containsKey(key)) {
                // Duplicate discovered through BOTH QR and OCR
                UnifiedUrlCandidateResponse existing = candidateMap.get(key);
                existing.setSource(CandidateUrlSource.BOTH);
            } else {
                UnifiedUrlCandidateResponse candidate = UnifiedUrlCandidateResponse.builder()
                        .url(candidateUrl)
                        .source(CandidateUrlSource.OCR)
                        .scannable(true)
                        .scanStatus("PENDING")
                        .build();
                candidateMap.put(key, candidate);
            }
        }

        // -------------------------------------------------------------
        // Step 5: Validate, Scheme Check, SSRF Check, and Bound Candidates
        // -------------------------------------------------------------
        List<UnifiedUrlCandidateResponse> candidateList = new ArrayList<>(candidateMap.values());
        int eligibleScansCount = 0;

        for (UnifiedUrlCandidateResponse candidate : candidateList) {
            if (!candidate.isScannable()) {
                continue;
            }

            String url = candidate.getUrl();
            String lower = url.toLowerCase();

            // Scheme enforcement: HTTP/HTTPS only
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                candidate.setScannable(false);
                candidate.setScanStatus("INVALID_SCHEME");
                candidate.setValidationError("Unsupported URL scheme. Only HTTP and HTTPS destinations are permitted.");
                continue;
            }

            // Resource limit bounding
            if (eligibleScansCount >= MAX_CANDIDATE_URLS) {
                candidate.setScannable(false);
                candidate.setScanStatus("RESOURCE_LIMIT_EXCEEDED");
                candidate.setValidationError("Exceeded maximum candidate scan limit of " + MAX_CANDIDATE_URLS + " per request.");
                limitations.add("Truncated URL scanning at limit of " + MAX_CANDIDATE_URLS + " candidates.");
                continue;
            }

            // SSRF Protection validation
            try {
                ssrfProtectionService.validateUrl(url);
                eligibleScansCount++;
            } catch (SSRFSecurityException ssrfEx) {
                candidate.setScannable(false);
                candidate.setScanStatus("BLOCKED_SSRF");
                candidate.setValidationError("SSRF security policy blocked destination: " + ssrfEx.getMessage());
                componentErrors.put("ssrf_" + url, ssrfEx.getMessage());
                isPartialFailure = true;
            }
        }

        // -------------------------------------------------------------
        // Step 6: Execute URL Scanner for Eligible Candidates (Failure-Isolated)
        // -------------------------------------------------------------
        List<URLScanResponse> successfulScans = new ArrayList<>();

        for (UnifiedUrlCandidateResponse candidate : candidateList) {
            if (!candidate.isScannable() || !"PENDING".equals(candidate.getScanStatus())) {
                continue;
            }

            try {
                URLScanResponse scanResult = urlScanService.processAndPersistScan(candidate.getUrl(), ScanType.SCREENSHOT, httpRequest);
                candidate.setScanResult(scanResult);
                candidate.setScanStatus("SCANNED");
                successfulScans.add(scanResult);

                // Update corresponding QR item scan status if applicable
                for (QRCodeItemResponse qrItem : qrItems) {
                    if (candidate.getUrl().equalsIgnoreCase(qrItem.getExtractedUrl())) {
                        qrItem.setScanResult(scanResult);
                        qrItem.setScanStatus("SCANNED");
                    }
                }
            } catch (Exception scanEx) {
                log.warn("Candidate URL '{}' scanning error: {}", candidate.getUrl(), scanEx.getMessage());
                candidate.setScanStatus("SCAN_FAILED");
                candidate.setValidationError("URL scan execution failed: " + scanEx.getMessage());
                componentErrors.put("scan_" + candidate.getUrl(), scanEx.getMessage());
                isPartialFailure = true;

                for (QRCodeItemResponse qrItem : qrItems) {
                    if (candidate.getUrl().equalsIgnoreCase(qrItem.getExtractedUrl())) {
                        qrItem.setScanStatus("SCAN_FAILED");
                        qrItem.setValidationError(scanEx.getMessage());
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Step 7: Check ML Service Availability (Zero Fabrication)
        // -------------------------------------------------------------
        try {
            MLServiceHealthDto mlHealth = mlServiceClient.checkHealth();
            if (!mlHealth.isModelLoaded() || "DOWN".equalsIgnoreCase(mlHealth.getStatus())) {
                componentErrors.put("mlService", "FastAPI ML service is unavailable or model is unloaded. Analysis relied strictly on heuristic rules without fabricated ML inference.");
                limitations.add("FastAPI ML service offline; threat verdict is based exclusively on deterministic rule engine analysis.");
                isPartialFailure = true;
            }
        } catch (Exception mlEx) {
            componentErrors.put("mlService", "ML health check failed: " + mlEx.getMessage());
            isPartialFailure = true;
        }

        // -------------------------------------------------------------
        // Step 8: Determine Primary Scan & Qualitative Risk Verdict
        // -------------------------------------------------------------
        URLScanResponse primaryScan;

        if (!successfulScans.isEmpty()) {
            primaryScan = successfulScans.stream()
                    .max(Comparator.comparingInt(URLScanResponse::getRiskScore))
                    .orElse(successfulScans.get(0));

            // Merge text heuristic indicators into primary scan detection report
            if (primaryScan.getDetection() != null && ocrAnalysis != null && !ocrAnalysis.getTextIndicators().isEmpty()) {
                List<ThreatIndicatorResponse> combined = new ArrayList<>(primaryScan.getDetection().getIndicators());
                for (ThreatIndicatorResponse ti : ocrAnalysis.getTextIndicators()) {
                    if (combined.stream().noneMatch(c -> c.getRuleTriggered().equals(ti.getRuleTriggered()))) {
                        combined.add(ti);
                    }
                }
                primaryScan.getDetection().setIndicators(combined);
            }
        } else {
            // No scanned external URLs; create internal screenshot scan representation
            int textScore = ocrAnalysis != null ? ocrAnalysis.getHeuristicRiskScore() : 0;
            RiskLevel textLevel = ocrAnalysis != null ? RiskLevel.valueOf(ocrAnalysis.getHeuristicRiskLevel()) : RiskLevel.SAFE;
            ThreatCategory textCategory = textScore >= 35 ? ThreatCategory.CREDENTIAL_THEFT : ThreatCategory.BENIGN;

            if (!candidateList.isEmpty() && successfulScans.isEmpty()) {
                componentErrors.put("urlScanner", "All candidate URL scans encountered errors or security policy blocks.");
            }

            primaryScan = urlScanService.processAndPersistScan("http://screenshot-analysis.internal", ScanType.SCREENSHOT, httpRequest);
            primaryScan.setRiskScore(textScore);
            primaryScan.setRiskLevel(textLevel);
            primaryScan.setThreatCategory(textCategory);
            primaryScan.setUrl(ocrExtractedText.length() > 60 ? ocrExtractedText.substring(0, 57) + "..." : (ocrExtractedText.isEmpty() ? "Visual Image Content" : ocrExtractedText));
            primaryScan.setNormalizedUrl(primaryScan.getUrl());

            if (primaryScan.getDetection() != null && ocrAnalysis != null) {
                primaryScan.getDetection().setIndicators(ocrAnalysis.getTextIndicators());
                primaryScan.getDetection().setExplanationSummary(ocrAnalysis.getSummary());
            }
        }

        // Compute overall risk score and qualitative level based on explicit maximum observed risk
        int textHeuristicScore = ocrAnalysis != null ? ocrAnalysis.getHeuristicRiskScore() : 0;
        int overallScore = Math.max(primaryScan.getRiskScore(), textHeuristicScore);

        // Dangerous scheme in QR or text automatically triggers HIGH_RISK
        boolean hasDangerousScheme = candidateList.stream()
                .anyMatch(c -> "BLOCKED_DANGEROUS_SCHEME".equals(c.getScanStatus()));
        if (hasDangerousScheme) {
            overallScore = Math.max(overallScore, 85);
        }

        RiskLevel overallLevel = overallScore >= 70 ? RiskLevel.HIGH_RISK : (overallScore >= 35 ? RiskLevel.SUSPICIOUS : RiskLevel.SAFE);
        ThreatCategory overallCategory = primaryScan.getThreatCategory();

        // Standard limitations
        limitations.add("Visual and OCR heuristic analysis detects surface phishing cues and does not guarantee underlying server-side safety.");
        limitations.add("Low resolution, compression artifacts, or blurred text may degrade optical yield.");

        // Recommended Next Steps
        List<String> nextSteps = new ArrayList<>();
        if (overallLevel == RiskLevel.HIGH_RISK) {
            nextSteps.add("Do not visit or enter credentials into any URL extracted from this image.");
            nextSteps.add("Immediately report this communication to your organization's security team or incident response.");
        } else if (overallLevel == RiskLevel.SUSPICIOUS) {
            nextSteps.add("Exercise caution: Verify the domain identity directly through official channels before proceeding.");
        } else {
            nextSteps.add("No immediate phishing vectors identified. Always ensure HTTPS lock and correct domain spelling.");
        }

        // -------------------------------------------------------------
        // Step 9: Assemble Final Unified Response
        // -------------------------------------------------------------
        String overallStatus = (isPartialFailure || !componentErrors.isEmpty()) ? "PARTIAL_SUCCESS" : "SUCCESS";
        String overallMessage = "PARTIAL_SUCCESS".equals(overallStatus)
                ? "Unified image analysis completed with partial component degradation."
                : "Unified image analysis completed successfully.";

        return UnifiedImageAnalysisResponse.builder()
                .status(overallStatus)
                .message(overallMessage)
                .analyzedAt(LocalDateTime.now())
                .imageValidation(imageValidation)
                .qrAnalysis(qrAnalysis)
                .ocrAnalysis(ocrAnalysis)
                .urlCandidates(candidateList)
                .successfulScans(successfulScans)
                .primaryScan(primaryScan)
                .overallRiskScore(overallScore)
                .overallRiskLevel(overallLevel)
                .threatCategory(overallCategory)
                .componentErrors(componentErrors)
                .limitations(limitations)
                .recommendedNextSteps(nextSteps)
                .build();
    }

    private List<String> extractUrlsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_REGEX.matcher(text);
        while (matcher.find()) {
            String match = matcher.group();
            // Clean trailing sentence punctuation
            match = match.replaceAll("[.,;)\\]]+$", "");
            if (!match.isEmpty() && !urls.contains(match)) {
                urls.add(match);
            }
        }
        return urls;
    }

    private String normalizeCandidateKey(String url) {
        if (url == null) return "";
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            String path = uri.getPath() != null ? uri.getPath() : "";
            String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
            return host + path + query;
        } catch (Exception e) {
            return url.trim().toLowerCase();
        }
    }
}
