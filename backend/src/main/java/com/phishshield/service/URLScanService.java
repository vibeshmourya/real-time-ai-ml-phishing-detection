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

    public URLScanService(SSRFProtectionService ssrfProtectionService,
                          RuleEngineService ruleEngineService,
                          PreventionContentService preventionContentService,
                          URLScanRepository urlScanRepository,
                          UserRepository userRepository,
                          AuditLogService auditLogService,
                          QRCodeService qrCodeService,
                          MLServiceClient mlServiceClient) {
        this.ssrfProtectionService = ssrfProtectionService;
        this.ruleEngineService = ruleEngineService;
        this.preventionContentService = preventionContentService;
        this.urlScanRepository = urlScanRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.qrCodeService = qrCodeService;
        this.mlServiceClient = mlServiceClient;
    }

    @Transactional
    public URLScanResponse scanUrl(URLScanRequest request, HttpServletRequest httpRequest) {
        String rawUrl = request.getUrl().trim();
        return processAndPersistScan(rawUrl, ScanType.URL, httpRequest);
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
    public URLScanResponse scanScreenshot(MultipartFile file, String textHint, HttpServletRequest httpRequest) {
        String contentText = textHint != null ? textHint.trim() : "";
        List<String> extractedUrls = extractUrlsFromText(contentText);

        String targetUrl;
        if (!extractedUrls.isEmpty()) {
            targetUrl = extractedUrls.get(0);
        } else {
            targetUrl = "http://screenshot-ocr-analysis.internal";
        }

        URLScanResponse response = processAndPersistScan(targetUrl, ScanType.SCREENSHOT, httpRequest);

        if (contentText.length() > 0 && response.getDetection() != null) {
            boolean hasUrgency = checkUrgencyLanguage(contentText);
            if (hasUrgency) {
                ThreatIndicatorResponse ocrIndicator = ThreatIndicatorResponse.builder()
                        .indicatorType(IndicatorType.CONTENT)
                        .indicatorName("Screenshot OCR Threat Language")
                        .description("Screenshot text contains urgency cues or credential solicitation.")
                        .severity(IndicatorSeverity.HIGH)
                        .ruleTriggered("RULE_OCR_URGENCY")
                        .weight(25.0)
                        .build();

                List<ThreatIndicatorResponse> indicators = new ArrayList<>(response.getDetection().getIndicators());
                indicators.add(ocrIndicator);
                response.getDetection().setIndicators(indicators);
            }
        }

        return response;
    }

    @Transactional
    public URLScanResponse scanQRCode(MultipartFile file, String decodedUrlHint, HttpServletRequest httpRequest) {
        String decodedUrl = null;

        // 1. In-process ZXing decode if image file provided
        if (file != null && !file.isEmpty()) {
            decodedUrl = qrCodeService.decodeQRCode(file);
        }

        // 2. Fallback to client decoded hint
        if (decodedUrl == null || decodedUrl.isEmpty()) {
            decodedUrl = decodedUrlHint != null ? decodedUrlHint.trim() : null;
        }

        if (decodedUrl == null || decodedUrl.isEmpty()) {
            throw new BadRequestException("No QR code or valid destination link was detected in the submitted content.");
        }

        return processAndPersistScan(decodedUrl, ScanType.QR, httpRequest);
    }

    private URLScanResponse processAndPersistScan(String rawUrl, ScanType scanType, HttpServletRequest httpRequest) {
        // 1. SSRF Protection validation
        if (!rawUrl.endsWith(".internal")) {
            ssrfProtectionService.validateUrl(rawUrl);
        }

        // 2. Normalization & Domain Extraction
        String normalizedUrl = UrlNormalizationUtil.normalizeUrl(rawUrl);
        String domain = UrlNormalizationUtil.extractDomain(rawUrl);

        // 3. Rule Engine Heuristic Evaluation
        RuleEngineService.RuleEvaluationResult ruleResult = ruleEngineService.evaluateUrl(rawUrl, domain);

        // 4. ML Service prediction integration (if available)
        Map<String, Object> mlResult = null;
        if (!rawUrl.endsWith(".internal")) {
            mlResult = mlServiceClient.predictUrl(rawUrl);
        }

        int finalScore = ruleResult.getRiskScore();
        RiskLevel finalRiskLevel = ruleResult.getRiskLevel();
        ThreatCategory finalCategory = ruleResult.getThreatCategory();
        double finalConfidence = ruleResult.getConfidenceScore();

        if (mlResult != null && Boolean.TRUE.equals(mlResult.get("success"))) {
            Object mlScoreObj = mlResult.get("risk_score");
            if (mlScoreObj instanceof Number) {
                finalScore = Math.max(finalScore, ((Number) mlScoreObj).intValue());
                if (finalScore >= 70) {
                    finalRiskLevel = RiskLevel.HIGH_RISK;
                } else if (finalScore >= 40) {
                    finalRiskLevel = RiskLevel.SUSPICIOUS;
                } else {
                    finalRiskLevel = RiskLevel.SAFE;
                }
            }
        }

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
        ModelPrediction prediction = ModelPrediction.builder()
                .scan(scan)
                .modelName(mlResult != null ? "Prahar-ML-XGBoost" : "Prahar-Heuristic-Rule-Engine-v1.0")
                .modelVersion("1.0.0")
                .phishingProbability(finalScore / 100.0)
                .lexicalScore(finalScore * 0.4)
                .domainScore(finalScore * 0.4)
                .ruleScore(finalScore * 0.2)
                .explanationJson("{\"indicatorsCount\":" + ruleResult.getIndicators().size() + "}")
                .build();
        scan.setPrediction(prediction);

        URLScan savedScan = urlScanRepository.save(scan);

        // 9. Fetch Contextual Prevention Guide
        PreventionContentResponse preventionGuide = preventionContentService
                .getPreventionGuide(finalCategory, "en");

        auditLogService.logAction(currentUser, "URL_SCAN", "URL_SCAN", savedScan.getId().toString(),
                "Scanned " + domain + " (" + scanType + ") - Risk: " + finalRiskLevel + " (" + finalScore + "/100)");

        return mapToResponse(savedScan, preventionGuide);
    }

    @Transactional(readOnly = true)
    public URLScanResponse getScanById(Long id) {
        URLScan scan = urlScanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scan report not found for id: " + id));

        PreventionContentResponse guide = preventionContentService
                .getPreventionGuide(scan.getThreatCategory(), "en");

        return mapToResponse(scan, guide);
    }

    @Transactional(readOnly = true)
    public Page<URLScanResponse> getUserScans(Pageable pageable) {
        User user = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new BadRequestException("Authentication required to view scan history"));

        return urlScanRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(scan -> mapToResponse(scan, null));
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
