package com.phishshield.service;

import com.phishshield.domain.*;
import com.phishshield.dto.request.EmailScanRequest;
import com.phishshield.dto.request.MessageScanRequest;
import com.phishshield.dto.request.URLScanRequest;
import com.phishshield.dto.response.*;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.ForbiddenException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.URLScanRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.util.SecurityUtils;
import com.phishshield.util.UrlNormalizationUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class URLScanService {

    private static final Logger log = LoggerFactory.getLogger(URLScanService.class);
    private static final Pattern URL_REGEX = Pattern.compile("(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\))+(?:\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))");

    private final SSRFProtectionService ssrfProtectionService;
    private final RuleEngineService ruleEngineService;
    private final PreventionContentService preventionContentService;
    private final URLScanRepository urlScanRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final QRCodeService qrCodeService;
    private final MLServiceClient mlServiceClient;
    private final com.phishshield.threatintel.ThreatIntelligenceService threatIntelligenceService;
    private final com.phishshield.security.image.ImageSecurityValidator imageSecurityValidator;
    private final OCRService ocrService;
    private final ScreenshotTextAnalyzer screenshotTextAnalyzer;
    private final NotificationService notificationService;
    private final MultiSignalScoringEngine multiSignalScoringEngine;

    public URLScanService(SSRFProtectionService ssrfProtectionService,
                          RuleEngineService ruleEngineService,
                          PreventionContentService preventionContentService,
                          URLScanRepository urlScanRepository,
                          UserRepository userRepository,
                          AuditLogService auditLogService,
                          QRCodeService qrCodeService,
                          MLServiceClient mlServiceClient,
                          com.phishshield.threatintel.ThreatIntelligenceService threatIntelligenceService,
                          com.phishshield.security.image.ImageSecurityValidator imageSecurityValidator,
                          OCRService ocrService,
                          ScreenshotTextAnalyzer screenshotTextAnalyzer,
                          NotificationService notificationService,
                          MultiSignalScoringEngine multiSignalScoringEngine) {
        this.ssrfProtectionService = ssrfProtectionService;
        this.ruleEngineService = ruleEngineService;
        this.preventionContentService = preventionContentService;
        this.urlScanRepository = urlScanRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.qrCodeService = qrCodeService;
        this.mlServiceClient = mlServiceClient;
        this.threatIntelligenceService = threatIntelligenceService;
        this.imageSecurityValidator = imageSecurityValidator;
        this.ocrService = ocrService;
        this.screenshotTextAnalyzer = screenshotTextAnalyzer;
        this.notificationService = notificationService;
        this.multiSignalScoringEngine = multiSignalScoringEngine;
    }

    @Transactional
    public URLScanResponse scanUrl(URLScanRequest request, HttpServletRequest httpRequest) {
        String rawUrl = request.getUrl().trim();
        return processAndPersistScan(rawUrl, ScanType.URL, httpRequest, request.isIncludeThreatIntelConsent());
    }

    @Transactional
    public URLScanResponse scanEmailContent(EmailScanRequest request, HttpServletRequest httpRequest) {
        String content = request.getEmailContent().trim();
        List<String> extractedUrls = extractUrlsFromText(content);

        String targetUrl;
        if (!extractedUrls.isEmpty()) {
            targetUrl = extractedUrls.get(0);
        } else {
            targetUrl = "http://email-content-analysis.internal";
        }

        URLScanResponse response = processAndPersistScan(targetUrl, ScanType.EMAIL, httpRequest);

        boolean hasUrgency = checkUrgencyLanguage(content);
        if (hasUrgency && response.getDetection() != null) {
            ThreatIndicatorResponse emailUrgencyIndicator = ThreatIndicatorResponse.builder()
                    .indicatorType(IndicatorType.CONTENT)
                    .indicatorName("Artificial Urgency / Threat Language")
                    .description("The email message contains psychological urgency cues ('immediate suspension', 'within 24 hours', 'verify now').")
                    .severity(IndicatorSeverity.MEDIUM)
                    .ruleTriggered("RULE_EMAIL_URGENCY")
                    .weight(15.0)
                    .build();

            List<ThreatIndicatorResponse> indicators = new ArrayList<>(response.getDetection().getIndicators());
            indicators.add(emailUrgencyIndicator);
            response.getDetection().setIndicators(indicators);
        }

        return response;
    }

    @Transactional
    public URLScanResponse scanMessageContent(MessageScanRequest request, HttpServletRequest httpRequest) {
        String content = request.getMessageContent().trim();
        List<String> extractedUrls = extractUrlsFromText(content);

        String targetUrl;
        if (!extractedUrls.isEmpty()) {
            targetUrl = extractedUrls.get(0);
        } else {
            targetUrl = "http://sms-content-analysis.internal";
        }

        URLScanResponse response = processAndPersistScan(targetUrl, ScanType.MESSAGE, httpRequest);

        boolean hasRewardOrTrap = checkRewardLanguage(content);
        if (hasRewardOrTrap && response.getDetection() != null) {
            ThreatIndicatorResponse msgIndicator = ThreatIndicatorResponse.builder()
                    .indicatorType(IndicatorType.CONTENT)
                    .indicatorName("Lure / Unsolicited Prize Bait")
                    .description("The message employs deceptive reward or gift baiting ('won $500', 'lottery winner', 'free voucher').")
                    .severity(IndicatorSeverity.HIGH)
                    .ruleTriggered("RULE_MSG_BAIT")
                    .weight(25.0)
                    .build();

            List<ThreatIndicatorResponse> indicators = new ArrayList<>(response.getDetection().getIndicators());
            indicators.add(msgIndicator);
            response.getDetection().setIndicators(indicators);
        }

        return response;
    }

    @Transactional
    public ScreenshotAnalysisResponse analyzeScreenshot(MultipartFile file, String textHint, HttpServletRequest httpRequest) {
        // 1. Validate image if file is provided
        com.phishshield.security.image.ValidatedImage validatedImage = null;
        if (file != null && !file.isEmpty()) {
            validatedImage = imageSecurityValidator.validateAndDecode(file);
        }

        if (validatedImage == null && (textHint == null || textHint.trim().isEmpty())) {
            throw new BadRequestException("No screenshot image file or text hint was provided.");
        }

        // 2. Optical QR code decoding on screenshot image (to find embedded QRs)
        QRCodeAnalysisResponse qrAnalysis = null;
        if (validatedImage != null) {
            List<String> rawQrs = qrCodeService.decodeAllQRCodes(validatedImage.getImage());
            if (!rawQrs.isEmpty()) {
                List<QRCodeItemResponse> qrItems = new ArrayList<>();
                for (int i = 0; i < rawQrs.size(); i++) {
                    QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(rawQrs.get(i), i + 1);
                    if (item.isScannableUrl() && item.getExtractedUrl() != null) {
                        try {
                            ssrfProtectionService.validateUrl(item.getExtractedUrl());
                            URLScanResponse scanResult = processAndPersistScan(item.getExtractedUrl(), ScanType.QR, httpRequest);
                            item.setScanResult(scanResult);
                            item.setScanStatus("SCANNED");
                        } catch (Exception e) {
                            item.setScanStatus("SCAN_FAILED");
                            item.setValidationError(e.getMessage());
                        }
                    }
                    qrItems.add(item);
                }
                qrAnalysis = QRCodeAnalysisResponse.builder()
                        .totalQrCodesFound(qrItems.size())
                        .qrCodes(qrItems)
                        .status("SUCCESS")
                        .message("Detected " + qrItems.size() + " QR code(s) inside screenshot.")
                        .build();
            }
        }

        // 3. OCR Text Extraction (Client-assisted or local engine)
        OCRService.RawOCRResult ocrRaw = ocrService.extractText(validatedImage, textHint);
        String extractedText = ocrRaw.getText();

        // 4. Explainable Heuristic Analysis on Text
        OCRTextAnalysisResult ocrAnalysis = screenshotTextAnalyzer.analyzeText(extractedText, ocrRaw.getStatus(), ocrRaw.getEngine());

        // 5. Extract and Validate URLs from OCR Text
        List<String> rawExtractedUrls = extractUrlsFromText(extractedText);
        List<URLScanResponse> scannedUrls = new ArrayList<>();

        for (String urlCand : rawExtractedUrls) {
            try {
                if (urlCand.startsWith("http://") || urlCand.startsWith("https://")) {
                    ssrfProtectionService.validateUrl(urlCand);
                    URLScanResponse urlScan = processAndPersistScan(urlCand, ScanType.SCREENSHOT, httpRequest);
                    scannedUrls.add(urlScan);
                }
            } catch (Exception e) {
                log.warn("Extracted URL from screenshot '{}' failed security scan: {}", urlCand, e.getMessage());
            }
        }

        // 6. Canonical Primary Scan determination
        URLScanResponse primaryScan = null;
        if (!scannedUrls.isEmpty()) {
            primaryScan = scannedUrls.stream()
                    .max(Comparator.comparingInt(URLScanResponse::getRiskScore))
                    .orElse(scannedUrls.get(0));

            // Merge text heuristic indicators into the primary scan's detection response
            if (primaryScan.getDetection() != null && !ocrAnalysis.getTextIndicators().isEmpty()) {
                List<ThreatIndicatorResponse> combinedIndicators = new ArrayList<>(primaryScan.getDetection().getIndicators());
                combinedIndicators.addAll(ocrAnalysis.getTextIndicators());
                primaryScan.getDetection().setIndicators(combinedIndicators);
            }
        } else {
            // No valid external URLs found; evaluate based on text heuristic threat signals
            int score = ocrAnalysis.getHeuristicRiskScore();
            RiskLevel level = RiskLevel.valueOf(ocrAnalysis.getHeuristicRiskLevel());
            ThreatCategory category = score >= 35 ? ThreatCategory.CREDENTIAL_THEFT : ThreatCategory.BENIGN;
            String internalUrl = "http://screenshot-analysis.internal";

            primaryScan = processAndPersistScan(internalUrl, ScanType.SCREENSHOT, httpRequest);
            primaryScan.setRiskScore(score);
            primaryScan.setRiskLevel(level);
            primaryScan.setThreatCategory(category);
            primaryScan.setUrl(extractedText.length() > 60 ? extractedText.substring(0, 57) + "..." : (extractedText.isEmpty() ? "Screenshot Image" : extractedText));
            primaryScan.setNormalizedUrl(primaryScan.getUrl());

            if (primaryScan.getDetection() != null) {
                primaryScan.getDetection().setIndicators(ocrAnalysis.getTextIndicators());
                primaryScan.getDetection().setExplanationSummary(ocrAnalysis.getSummary());
            }
        }

        int overallScore = Math.max(primaryScan.getRiskScore(), ocrAnalysis.getHeuristicRiskScore());
        RiskLevel overallLevel = overallScore >= 70 ? RiskLevel.HIGH_RISK : (overallScore >= 35 ? RiskLevel.SUSPICIOUS : RiskLevel.SAFE);

        List<String> limitations = Arrays.asList(
                "Visual and OCR text analysis provides evidence-based heuristic indicators and does not confirm underlying server-side malware.",
                "Low-resolution, blurry, or distorted screenshots may affect optical text recognition yield.",
                "Always verify sender identity and domain legitimacy independently before submitting credentials."
        );

        return ScreenshotAnalysisResponse.builder()
                .qrAnalysis(qrAnalysis)
                .ocrAnalysis(ocrAnalysis)
                .extractedUrlScans(scannedUrls)
                .primaryScan(primaryScan)
                .overallRiskScore(overallScore)
                .overallRiskLevel(overallLevel)
                .threatCategory(primaryScan.getThreatCategory())
                .limitations(limitations)
                .status("SUCCESS")
                .message("Screenshot visual and text analysis completed successfully.")
                .build();
    }

    @Transactional
    public URLScanResponse scanScreenshot(MultipartFile file, String textHint, HttpServletRequest httpRequest) {
        ScreenshotAnalysisResponse analysis = analyzeScreenshot(file, textHint, httpRequest);
        return analysis.getPrimaryScan();
    }

    @Transactional
    public QRCodeAnalysisResponse analyzeQRCode(MultipartFile file, String decodedUrlHint, HttpServletRequest httpRequest) {
        List<String> rawPayloads = new ArrayList<>();

        // 1. In-process ZXing multi-decode if image file provided
        if (file != null && !file.isEmpty()) {
            rawPayloads.addAll(qrCodeService.decodeAllQRCodes(file));
        }

        // 2. Fallback to client decoded hint if no payload from image
        if (rawPayloads.isEmpty() && decodedUrlHint != null && !decodedUrlHint.trim().isEmpty()) {
            rawPayloads.add(decodedUrlHint.trim());
        }

        if (rawPayloads.isEmpty()) {
            if (file != null && !file.isEmpty()) {
                return QRCodeAnalysisResponse.builder()
                        .totalQrCodesFound(0)
                        .status("NO_QR_DETECTED")
                        .message("No readable QR code was detected in the submitted image.")
                        .build();
            }
            throw new BadRequestException("No QR image file or decoded URL payload was submitted.");
        }

        List<QRCodeItemResponse> qrItems = new ArrayList<>();
        URLScanResponse primaryScan = null;

        for (int i = 0; i < rawPayloads.size(); i++) {
            String raw = rawPayloads.get(i);
            QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(raw, i + 1);

            if (item.isScannableUrl() && item.getExtractedUrl() != null) {
                try {
                    // Pre-scan SSRF validation
                    ssrfProtectionService.validateUrl(item.getExtractedUrl());

                    // Execute real Prahar scan pipeline
                    URLScanResponse scanResult = processAndPersistScan(item.getExtractedUrl(), ScanType.QR, httpRequest);
                    item.setScanResult(scanResult);
                    item.setScanStatus("SCANNED");

                    if (primaryScan == null) {
                        primaryScan = scanResult;
                    }
                } catch (com.phishshield.exception.SSRFSecurityException ssrfEx) {
                    log.warn("QR Code destination rejected by SSRF protection: {}", ssrfEx.getMessage());
                    item.setScanStatus("BLOCKED_SSRF");
                    item.setValidationError("Destination URL rejected by security policy: " + ssrfEx.getMessage());
                } catch (Exception scanEx) {
                    log.warn("Failed scanning QR destination URL '{}': {}", item.getExtractedUrl(), scanEx.getMessage());
                    item.setScanStatus("SCAN_FAILED");
                    item.setValidationError("Scanner error: " + scanEx.getMessage());
                }
            }

            qrItems.add(item);
        }

        return QRCodeAnalysisResponse.builder()
                .totalQrCodesFound(qrItems.size())
                .qrCodes(qrItems)
                .primaryScan(primaryScan)
                .status("SUCCESS")
                .message("Successfully analyzed " + qrItems.size() + " QR code payload(s).")
                .build();
    }

    @Transactional
    public URLScanResponse scanQRCode(MultipartFile file, String decodedUrlHint, HttpServletRequest httpRequest) {
        QRCodeAnalysisResponse analysis = analyzeQRCode(file, decodedUrlHint, httpRequest);
        if (analysis.getPrimaryScan() != null) {
            return analysis.getPrimaryScan();
        }
        if (analysis.getTotalQrCodesFound() == 0) {
            throw new BadRequestException("No QR code or valid destination link was detected in the submitted content.");
        }
        QRCodeItemResponse firstItem = analysis.getQrCodes().get(0);
        return URLScanResponse.builder()
                .url(firstItem.getRawPayload())
                .normalizedUrl(firstItem.getSafeDisplayPayload())
                .domain("qr-content.internal")
                .scanType(ScanType.QR)
                .riskScore(0)
                .riskLevel(RiskLevel.SAFE)
                .threatCategory(ThreatCategory.BENIGN)
                .status("COMPLETED")
                .build();
    }

    public URLScanResponse processAndPersistScan(String rawUrl, ScanType scanType, HttpServletRequest httpRequest) {
        return processAndPersistScan(rawUrl, scanType, httpRequest, false);
    }

    public URLScanResponse processAndPersistScan(String rawUrl, ScanType scanType, HttpServletRequest httpRequest, boolean userConsent) {
        // 1. SSRF Protection validation
        if (!rawUrl.endsWith(".internal")) {
            ssrfProtectionService.validateUrl(rawUrl);
        }

        // 2. Normalization & Domain Extraction
        String normalizedUrl = UrlNormalizationUtil.normalizeUrl(rawUrl);
        if (normalizedUrl.length() > 1024) {
            normalizedUrl = normalizedUrl.substring(0, 1024);
        }
        String domain = UrlNormalizationUtil.extractDomain(rawUrl);
        if (domain.length() > 255) {
            domain = domain.substring(0, 255);
        }

        // 3. Rule Engine Heuristic Evaluation
        RuleEngineService.RuleEvaluationResult ruleResult = ruleEngineService.evaluateUrl(rawUrl, domain);
        int ruleScore = ruleResult.getRiskScore();

        // 4. ML Service prediction integration (if available)
        MLPredictionResult mlResult = null;
        if (!rawUrl.endsWith(".internal")) {
            mlResult = mlServiceClient.predictUrlTyped(rawUrl);
        }

        // 4b. Legitimate External Threat Intelligence Check (Optional, respects user consent)
        com.phishshield.threatintel.ThreatIntelReport threatIntel = null;
        if (!rawUrl.endsWith(".internal")) {
            threatIntel = threatIntelligenceService.checkReputation(rawUrl, domain, userConsent);
        }

        // 4c. Intelligent Explainable Multi-Signal Fusion Scoring
        MultiSignalScoringEngine.CompositeScoringResult scoringResult = multiSignalScoringEngine.computeCompositeScore(
                rawUrl, domain, ruleResult, mlResult, threatIntel
        );

        int finalScore = scoringResult.getNormalizedScore();
        RiskLevel finalRiskLevel = scoringResult.getRiskLevel();
        ThreatCategory finalCategory = scoringResult.getThreatCategory();
        double finalConfidence = scoringResult.getConfidenceScore();
        ScanExplainabilityDetails explainability = scoringResult.getExplainability();

        boolean mlApplied = mlResult != null && mlResult.isAvailable();
        Double mlProbability = mlApplied ? mlResult.getMlProbability() : null;
        String modelName = mlApplied && mlResult.getModelName() != null ? mlResult.getModelName() : "PRAHAR_HEURISTIC_RULES_ONLY";
        String modelVersion = mlApplied && mlResult.getModelVersion() != null ? mlResult.getModelVersion() : "1.0.0";
        String mlFailureReason = mlResult != null ? mlResult.getFailureReason() : null;

        // 5. Resolve authenticated user if present
        User currentUser = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);

        // 6. Build and Persist Scan Entity
        URLScan scan = URLScan.builder()
                .user(currentUser)
                .url(rawUrl)
                .normalizedUrl(normalizedUrl)
                .domain(domain)
                .scanType(scanType)
                .riskScore(finalScore)
                .riskLevel(finalRiskLevel)
                .threatCategory(finalCategory)
                .status("COMPLETED")
                .clientIp(SecurityUtils.getClientIpAddress(httpRequest))
                .userAgent(SecurityUtils.getUserAgent(httpRequest))
                .build();

        // 7. Threat Detection Entity
        ThreatDetection detection = ThreatDetection.builder()
                .scan(scan)
                .threatCategory(finalCategory)
                .confidenceScore(finalConfidence)
                .explanationSummary(ruleResult.getExplanationSummary())
                .recommendedActions(ruleResult.getRecommendedActions())
                .build();

        for (ThreatIndicator indicator : ruleResult.getIndicators()) {
            detection.addIndicator(indicator);
        }
        scan.setDetection(detection);

        // 8. Baseline Model Prediction record
        String explanationJson;
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("isMlAvailable", mlApplied);
            details.put("ruleScore", ruleScore);
            if (mlApplied) {
                details.put("mlProbability", mlProbability);
                details.put("explanations", mlResult.getExplanations());
                details.put("indicators", mlResult.getIndicators());
            } else {
                details.put("failureReason", mlFailureReason != null ? mlFailureReason : "ML service was offline/unreachable");
                details.put("note", "Assessed exclusively via Prahar 4-layer heuristic rule engine.");
            }
            explanationJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details);
        } catch (Exception e) {
            explanationJson = "{\"indicatorsCount\":" + ruleResult.getIndicators().size() + "}";
        }

        ModelPrediction prediction = ModelPrediction.builder()
                .scan(scan)
                .modelName(modelName)
                .modelVersion(modelVersion)
                .phishingProbability(mlProbability != null ? mlProbability : (ruleScore / 100.0))
                .lexicalScore(ruleScore * 0.4)
                .domainScore(ruleScore * 0.4)
                .ruleScore(ruleScore / 100.0)
                .explanationJson(explanationJson)
                .build();
        scan.setPrediction(prediction);

        URLScan savedScan = urlScanRepository.save(scan);

        // 9. Fetch Contextual Prevention Guide
        PreventionContentResponse preventionGuide = preventionContentService
                .getPreventionGuide(finalCategory, "en");

        auditLogService.logAction(currentUser, "URL_SCAN", "URL_SCAN", savedScan.getId().toString(),
                "Scanned " + domain + " (" + scanType + ") - Risk: " + finalRiskLevel + " (" + finalScore + "/100)");

        URLScanResponse response = mapToResponse(savedScan, preventionGuide, threatIntel, explainability);
        if (currentUser != null && notificationService != null) {
            try {
                notificationService.createScanNotification(currentUser, response);
            } catch (Exception e) {
                log.warn("Failed creating security alert notification: {}", e.getMessage());
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public URLScanResponse getScanById(Long id) {
        URLScan scan = urlScanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scan report not found for id: " + id));

        PreventionContentResponse guide = preventionContentService
                .getPreventionGuide(scan.getThreatCategory(), "en");

        return mapToResponse(scan, guide, null);
    }

    @Transactional(readOnly = true)
    public Page<URLScanResponse> getUserScans(Pageable pageable) {
        User user = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new BadRequestException("Authentication required to view scan history"));

        return urlScanRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(scan -> mapToResponse(scan, null, null));
    }

    @Transactional
    public void deleteScan(Long id) {
        User user = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new BadRequestException("Authentication required"));

        URLScan scan = urlScanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scan not found with id: " + id));

        // Privacy protection: users can only delete their own scans
        if (scan.getUser() == null || !scan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to delete this scan record.");
        }

        urlScanRepository.delete(scan);
        auditLogService.logAction(user, "DELETE_SCAN", "URL_SCAN", id.toString(), "User deleted scan record");
    }

    public URLScanResponse mapToResponse(URLScan scan, PreventionContentResponse guide) {
        return mapToResponse(scan, guide, null);
    }

    public URLScanResponse mapToResponse(URLScan scan, PreventionContentResponse guide, com.phishshield.threatintel.ThreatIntelReport threatIntel) {
        return mapToResponse(scan, guide, threatIntel, null);
    }

    public URLScanResponse mapToResponse(URLScan scan, PreventionContentResponse guide, com.phishshield.threatintel.ThreatIntelReport threatIntel, ScanExplainabilityDetails explainability) {
        ThreatDetectionResponse detectionResponse = null;
        if (scan.getDetection() != null) {
            List<ThreatIndicatorResponse> indicatorResponses = scan.getDetection().getIndicators().stream()
                    .map(ind -> ThreatIndicatorResponse.builder()
                            .id(ind.getId())
                            .indicatorType(ind.getIndicatorType())
                            .indicatorName(ind.getIndicatorName())
                            .description(ind.getDescription())
                            .severity(ind.getSeverity())
                            .ruleTriggered(ind.getRuleTriggered())
                            .weight(ind.getWeight())
                            .build())
                    .collect(Collectors.toList());

            detectionResponse = ThreatDetectionResponse.builder()
                    .id(scan.getDetection().getId())
                    .threatCategory(scan.getDetection().getThreatCategory())
                    .confidenceScore(scan.getDetection().getConfidenceScore())
                    .explanationSummary(scan.getDetection().getExplanationSummary())
                    .recommendedActions(scan.getDetection().getRecommendedActions())
                    .indicators(indicatorResponses)
                    .build();
        }

        ModelPredictionResponse modelPredictionResponse = null;
        if (scan.getPrediction() != null) {
            ModelPrediction pred = scan.getPrediction();
            List<String> modelExplanations = new ArrayList<>();
            List<Map<String, Object>> mlIndicators = new ArrayList<>();
            boolean isMlAvailable = !"PRAHAR_HEURISTIC_RULES_ONLY".equalsIgnoreCase(pred.getModelName());
            String failureReason = null;

            if (pred.getExplanationJson() != null && !pred.getExplanationJson().isBlank()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(pred.getExplanationJson());
                    if (node.has("isMlAvailable")) {
                        isMlAvailable = node.path("isMlAvailable").asBoolean(true);
                    }
                    if (node.has("failureReason")) {
                        failureReason = node.path("failureReason").asText();
                    }
                    if (node.has("explanations") && node.path("explanations").isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode exp : node.path("explanations")) {
                            modelExplanations.add(exp.asText());
                        }
                    }
                } catch (Exception ignored) {}
            }

            modelPredictionResponse = ModelPredictionResponse.builder()
                    .modelName(pred.getModelName())
                    .modelVersion(pred.getModelVersion())
                    .phishingProbability(pred.getPhishingProbability())
                    .isMlAvailable(isMlAvailable)
                    .mlFailureReason(failureReason)
                    .lexicalScore(pred.getLexicalScore())
                    .domainScore(pred.getDomainScore())
                    .ruleScore(pred.getRuleScore())
                    .explanations(modelExplanations)
                    .timestamp(pred.getCreatedAt())
                    .build();
        }

        return URLScanResponse.builder()
                .scanId(scan.getId())
                .url(scan.getUrl())
                .normalizedUrl(scan.getNormalizedUrl())
                .domain(scan.getDomain())
                .scanType(scan.getScanType())
                .riskScore(scan.getRiskScore())
                .riskLevel(scan.getRiskLevel())
                .threatCategory(scan.getThreatCategory())
                .status(scan.getStatus())
                .detection(detectionResponse)
                .preventionGuide(guide)
                .modelPrediction(modelPredictionResponse)
                .threatIntel(threatIntel)
                .explainability(explainability)
                .createdAt(scan.getCreatedAt())
                .build();
    }

    private List<String> extractUrlsFromText(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null) return urls;

        Matcher matcher = URL_REGEX.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

    private boolean checkUrgencyLanguage(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("urgent") || lower.contains("24 hours") || lower.contains("suspended")
                || lower.contains("immediately") || lower.contains("blocked") || lower.contains("action required");
    }

    private boolean checkRewardLanguage(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("won") || lower.contains("lottery") || lower.contains("gift card")
                || lower.contains("free prize") || lower.contains("claimed");
    }
}
