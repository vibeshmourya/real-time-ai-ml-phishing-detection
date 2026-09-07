package com.phishshield.service;

import com.phishshield.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class RuleEngineService {

    private static final Pattern IP_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    private static final Set<String> SUSPICIOUS_TLDS = new HashSet<>(Arrays.asList(
            "tk", "ml", "ga", "cf", "gq", "top", "xyz", "work", "click", "buzz", "rest", "fit", "surf", "casa"
    ));

    private static final Map<String, ThreatCategory> PRIORITIZED_BRAND_KEYWORDS = new LinkedHashMap<>();
    static {
        // 1. Banking Brands (Highest Specificity)
        PRIORITIZED_BRAND_KEYWORDS.put("hdfc", ThreatCategory.BANKING_PHISHING);
        PRIORITIZED_BRAND_KEYWORDS.put("sbi", ThreatCategory.BANKING_PHISHING);
        PRIORITIZED_BRAND_KEYWORDS.put("icici", ThreatCategory.BANKING_PHISHING);
        PRIORITIZED_BRAND_KEYWORDS.put("paypal", ThreatCategory.BANKING_PHISHING);
        PRIORITIZED_BRAND_KEYWORDS.put("chase", ThreatCategory.BANKING_PHISHING);
        PRIORITIZED_BRAND_KEYWORDS.put("wellsfargo", ThreatCategory.BANKING_PHISHING);
        PRIORITIZED_BRAND_KEYWORDS.put("bankofamerica", ThreatCategory.BANKING_PHISHING);
        PRIORITIZED_BRAND_KEYWORDS.put("netbanking", ThreatCategory.BANKING_PHISHING);

        // 2. Government & Tax Impersonation
        PRIORITIZED_BRAND_KEYWORDS.put("incometax", ThreatCategory.GOVERNMENT_IMPERSONATION);
        PRIORITIZED_BRAND_KEYWORDS.put("tax-refund", ThreatCategory.GOVERNMENT_IMPERSONATION);
        PRIORITIZED_BRAND_KEYWORDS.put("epfindia", ThreatCategory.GOVERNMENT_IMPERSONATION);

        // 3. KYC Traps
        PRIORITIZED_BRAND_KEYWORDS.put("kyc", ThreatCategory.FAKE_KYC);
        PRIORITIZED_BRAND_KEYWORDS.put("pan-card", ThreatCategory.FAKE_KYC);
        PRIORITIZED_BRAND_KEYWORDS.put("aadhaar", ThreatCategory.FAKE_KYC);
        PRIORITIZED_BRAND_KEYWORDS.put("sim-block", ThreatCategory.FAKE_KYC);

        // 4. Delivery Traps
        PRIORITIZED_BRAND_KEYWORDS.put("indiapost", ThreatCategory.DELIVERY_SCAM);
        PRIORITIZED_BRAND_KEYWORDS.put("fedex-delivery", ThreatCategory.DELIVERY_SCAM);
        PRIORITIZED_BRAND_KEYWORDS.put("dhl-parcel", ThreatCategory.DELIVERY_SCAM);
        PRIORITIZED_BRAND_KEYWORDS.put("package-tracking", ThreatCategory.DELIVERY_SCAM);

        // 5. Employment & Job Traps
        PRIORITIZED_BRAND_KEYWORDS.put("parttime-job", ThreatCategory.JOB_SCAM);
        PRIORITIZED_BRAND_KEYWORDS.put("daily-earning", ThreatCategory.JOB_SCAM);
        PRIORITIZED_BRAND_KEYWORDS.put("telegram-task", ThreatCategory.JOB_SCAM);

        // 6. Shopping / Discount Traps
        PRIORITIZED_BRAND_KEYWORDS.put("discount-store", ThreatCategory.SHOPPING_SCAM);
        PRIORITIZED_BRAND_KEYWORDS.put("iphone-free", ThreatCategory.SHOPPING_SCAM);
        PRIORITIZED_BRAND_KEYWORDS.put("giftcard-win", ThreatCategory.SHOPPING_SCAM);

        // 7. General Credential / Auth / Login Traps
        PRIORITIZED_BRAND_KEYWORDS.put("secure-login", ThreatCategory.CREDENTIAL_THEFT);
        PRIORITIZED_BRAND_KEYWORDS.put("verify-account", ThreatCategory.CREDENTIAL_THEFT);
        PRIORITIZED_BRAND_KEYWORDS.put("password-reset", ThreatCategory.CREDENTIAL_THEFT);
        PRIORITIZED_BRAND_KEYWORDS.put("account-update", ThreatCategory.CREDENTIAL_THEFT);
        PRIORITIZED_BRAND_KEYWORDS.put("webmail", ThreatCategory.CREDENTIAL_THEFT);
        PRIORITIZED_BRAND_KEYWORDS.put("office365", ThreatCategory.CREDENTIAL_THEFT);
    }

    public static class RuleEvaluationResult {
        private final int riskScore;
        private final RiskLevel riskLevel;
        private final ThreatCategory threatCategory;
        private final double confidenceScore;
        private final String explanationSummary;
        private final String recommendedActions;
        private final List<ThreatIndicator> indicators;

        public RuleEvaluationResult(int riskScore, RiskLevel riskLevel, ThreatCategory threatCategory,
                                    double confidenceScore, String explanationSummary,
                                    String recommendedActions, List<ThreatIndicator> indicators) {
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
            this.threatCategory = threatCategory;
            this.confidenceScore = confidenceScore;
            this.explanationSummary = explanationSummary;
            this.recommendedActions = recommendedActions;
            this.indicators = indicators;
        }

        public int getRiskScore() { return riskScore; }
        public RiskLevel getRiskLevel() { return riskLevel; }
        public ThreatCategory getThreatCategory() { return threatCategory; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getExplanationSummary() { return explanationSummary; }
        public String getRecommendedActions() { return recommendedActions; }
        public List<ThreatIndicator> getIndicators() { return indicators; }
    }

    public RuleEvaluationResult evaluateUrl(String rawUrl, String domain) {
        List<ThreatIndicator> indicators = new ArrayList<>();
        double accumulatedScore = 0.0;
        String lowerUrl = rawUrl.toLowerCase(Locale.ROOT);
        String lowerDomain = (domain != null ? domain : "").toLowerCase(Locale.ROOT);

        ThreatCategory detectedCategory = ThreatCategory.BENIGN;

        // 1. Check IP Hostname
        if (IP_PATTERN.matcher(lowerDomain).matches()) {
            accumulatedScore += 35.0;
            indicators.add(ThreatIndicator.builder()
                    .indicatorType(IndicatorType.DOMAIN)
                    .indicatorName("Direct IP Address Hostname")
                    .description("The URL uses a raw numeric IP address instead of a recognized domain name, a hallmark of evasive malicious servers.")
                    .severity(IndicatorSeverity.HIGH)
                    .ruleTriggered("RULE_IP_HOST")
                    .weight(35.0)
                    .build());
        }

        // 2. Check Suspicious TLD
        String tld = extractTld(lowerDomain);
        if (SUSPICIOUS_TLDS.contains(tld)) {
            accumulatedScore += 25.0;
            indicators.add(ThreatIndicator.builder()
                    .indicatorType(IndicatorType.DOMAIN)
                    .indicatorName("Suspicious Free / High-Abuse TLD")
                    .description("The domain ends with '." + tld + "', a top-level domain frequently associated with automated phishing campaigns.")
                    .severity(IndicatorSeverity.HIGH)
                    .ruleTriggered("RULE_SUSPICIOUS_TLD")
                    .weight(25.0)
                    .build());
        }

        // 3. Check Punycode / Homoglyph
        if (lowerDomain.contains("xn--")) {
            accumulatedScore += 30.0;
            indicators.add(ThreatIndicator.builder()
                    .indicatorType(IndicatorType.SECURITY)
                    .indicatorName("Punycode Character Impersonation")
                    .description("The domain uses Punycode encoding ('xn--') to disguise visual look-alike foreign characters.")
                    .severity(IndicatorSeverity.CRITICAL)
                    .ruleTriggered("RULE_PUNYCODE")
                    .weight(30.0)
                    .build());
        }

        // 4. Check @ Symbol
        if (lowerUrl.contains("@")) {
            accumulatedScore += 30.0;
            indicators.add(ThreatIndicator.builder()
                    .indicatorType(IndicatorType.LEXICAL)
                    .indicatorName("Authentication Trick (@ Symbol in URL)")
                    .description("The URL contains an '@' character, which causes browsers to ignore all preceding text and navigate to the attacker destination.")
                    .severity(IndicatorSeverity.HIGH)
                    .ruleTriggered("RULE_AT_SYMBOL")
                    .weight(30.0)
                    .build());
        }

        // 5. Check Prioritized Brand Impersonation Keywords
        for (Map.Entry<String, ThreatCategory> entry : PRIORITIZED_BRAND_KEYWORDS.entrySet()) {
            String keyword = entry.getKey();
            if (lowerUrl.contains(keyword)) {
                if (!isOfficialDomain(lowerDomain, keyword)) {
                    accumulatedScore += 30.0;
                    if (detectedCategory == ThreatCategory.BENIGN) {
                        detectedCategory = entry.getValue();
                    }
                    indicators.add(ThreatIndicator.builder()
                            .indicatorType(IndicatorType.CONTENT)
                            .indicatorName("Brand Impersonation Indicator (" + keyword + ")")
                            .description("The URL contains sensitive brand or security keyword '" + keyword + "' hosted on an unofficial domain name.")
                            .severity(IndicatorSeverity.CRITICAL)
                            .ruleTriggered("RULE_BRAND_IMPERSONATION_" + keyword.toUpperCase())
                            .weight(30.0)
                            .build());
                    break;
                }
            }
        }

        // 6. Check Hyphen Count
        long hyphenCount = lowerDomain.chars().filter(ch -> ch == '-').count();
        if (hyphenCount >= 3) {
            accumulatedScore += 15.0;
            indicators.add(ThreatIndicator.builder()
                    .indicatorType(IndicatorType.LEXICAL)
                    .indicatorName("Excessive Hyphens in Domain")
                    .description("The domain name contains an unusually high number of hyphens (" + hyphenCount + "), commonly used in typo-squatted phishing names.")
                    .severity(IndicatorSeverity.MEDIUM)
                    .ruleTriggered("RULE_EXCESSIVE_HYPHENS")
                    .weight(15.0)
                    .build());
        }

        // 7. Check Subdomain Depth (Dots count in domain)
        long dotCount = lowerDomain.chars().filter(ch -> ch == '.').count();
        if (dotCount >= 3) {
            accumulatedScore += 15.0;
            indicators.add(ThreatIndicator.builder()
                    .indicatorType(IndicatorType.DOMAIN)
                    .indicatorName("Excessive Subdomain Hierarchy")
                    .description("The hostname contains " + dotCount + " dot segments, suggesting a deeply nested subdomain structure.")
                    .severity(IndicatorSeverity.MEDIUM)
                    .ruleTriggered("RULE_DEEP_SUBDOMAINS")
                    .weight(15.0)
                    .build());
        }

        // 8. Check URL Length
        if (lowerUrl.length() > 85) {
            accumulatedScore += 10.0;
            indicators.add(ThreatIndicator.builder()
                    .indicatorType(IndicatorType.LEXICAL)
                    .indicatorName("Abnormally Long URL")
                    .description("The overall URL length is unusually long (" + lowerUrl.length() + " characters), which can be used to hide malicious tokens.")
                    .severity(IndicatorSeverity.LOW)
                    .ruleTriggered("RULE_URL_LENGTH")
                    .weight(10.0)
                    .build());
        }

        // 9. Shannon Entropy
        double entropy = calculateEntropy(lowerDomain);
        if (entropy > 4.1 && lowerDomain.length() > 12) {
            accumulatedScore += 15.0;
            indicators.add(ThreatIndicator.builder()
                    .indicatorType(IndicatorType.LEXICAL)
                    .indicatorName("High Character Randomness (Entropy)")
                    .description("The domain exhibits high character randomness (entropy score: " + String.format(Locale.ROOT, "%.2f", entropy) + "), characteristic of algorithmically generated domains.")
                    .severity(IndicatorSeverity.MEDIUM)
                    .ruleTriggered("RULE_HIGH_ENTROPY")
                    .weight(15.0)
                    .build());
        }

        // Calculate Final Risk Score (0-100 bounded)
        int finalScore = (int) Math.min(100.0, Math.round(accumulatedScore));
        RiskLevel riskLevel;
        if (finalScore >= 70) {
            riskLevel = RiskLevel.HIGH_RISK;
            if (detectedCategory == ThreatCategory.BENIGN) {
                detectedCategory = ThreatCategory.CREDENTIAL_THEFT;
            }
        } else if (finalScore >= 40) {
            riskLevel = RiskLevel.SUSPICIOUS;
            if (detectedCategory == ThreatCategory.BENIGN) {
                detectedCategory = ThreatCategory.UNKNOWN;
            }
        } else {
            riskLevel = RiskLevel.SAFE;
            detectedCategory = ThreatCategory.BENIGN;
        }

        // Calculate Confidence
        double confidence = Math.min(0.98, Math.max(0.65, 0.50 + (indicators.size() * 0.12)));

        // Synthesize Explanation & Recommended Actions
        String explanationSummary;
        String recommendedActions;

        if (riskLevel == RiskLevel.HIGH_RISK) {
            explanationSummary = "Multiple critical phishing indicators were identified. The domain structure or keywords indicate deceptive impersonation.";
            recommendedActions = "DO NOT enter passwords, OTPs, PINs, or bank details. Close this web page immediately.";
        } else if (riskLevel == RiskLevel.SUSPICIOUS) {
            explanationSummary = "Potential threat indicators were observed. The link exhibits atypical domain or structural patterns.";
            recommendedActions = "Proceed with extreme caution. Verify the sender's identity through an official external channel before interacting.";
        } else {
            explanationSummary = "No typical phishing anomalies or high-risk deception patterns were detected for this URL.";
            recommendedActions = "Always maintain standard cyber hygiene. Confirm HTTPS lock and genuine address bar domain before logging in.";
        }

        return new RuleEvaluationResult(
                finalScore, riskLevel, detectedCategory, confidence,
                explanationSummary, recommendedActions, indicators
        );
    }

    private String extractTld(String domain) {
        if (domain == null || !domain.contains(".")) return "";
        String[] parts = domain.split("\\.");
        return parts[parts.length - 1];
    }

    private boolean isOfficialDomain(String domain, String brand) {
        if ("hdfc".equals(brand) && (domain.equals("hdfcbank.com") || domain.endsWith(".hdfcbank.com"))) return true;
        if ("sbi".equals(brand) && (domain.equals("onlinesbi.sbi") || domain.equals("sbi.co.in") || domain.endsWith(".sbi.co.in"))) return true;
        if ("icici".equals(brand) && (domain.equals("icicibank.com") || domain.endsWith(".icicibank.com"))) return true;
        if ("paypal".equals(brand) && (domain.equals("paypal.com") || domain.endsWith(".paypal.com"))) return true;
        if ("chase".equals(brand) && (domain.equals("chase.com") || domain.endsWith(".chase.com"))) return true;
        if ("wellsfargo".equals(brand) && (domain.equals("wellsfargo.com") || domain.endsWith(".wellsfargo.com"))) return true;
        if ("bankofamerica".equals(brand) && (domain.equals("bankofamerica.com") || domain.endsWith(".bankofamerica.com"))) return true;
        if ("indiapost".equals(brand) && (domain.equals("indiapost.gov.in") || domain.endsWith(".indiapost.gov.in"))) return true;
        return false;
    }

    private double calculateEntropy(String input) {
        if (input == null || input.isEmpty()) return 0.0;
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        double entropy = 0.0;
        int len = input.length();
        for (int count : freq.values()) {
            double p = (double) count / len;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}
