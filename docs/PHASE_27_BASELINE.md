# Prahar 2.0 — Phase 27 Detection Baseline Audit

**Audit Date:** October 24, 2026  
**Git Baseline Commit:** `bd56a5c80c6b56047261b5df300e25a2f2c695ad`  
**Purpose:** Comprehensive baseline audit of the threat detection pipeline, feature extraction, ML classifiers, heuristic rules, external threat intelligence, and risk scoring prior to Phase 27 enhancements.

---

## 1. Inventory of Existing Detection Pipeline Components

| Pipeline Stage | Component File / Service | Primary Responsibility |
| :--- | :--- | :--- |
| **URL Pre-Validation & SSRF Guard** | [`SSRFProtectionService.java`](../backend/src/main/java/com/phishshield/service/SSRFProtectionService.java) | Intercepts target URLs before network access. Resolves DNS and blocks loopback (`127.0.0.1`), link-local metadata (`169.254.169.254`), and RFC-1918 private subnets. |
| **URL Normalization & Canonicalization** | [`UrlNormalizationUtil.java`](../backend/src/main/java/com/phishshield/util/UrlNormalizationUtil.java) | Strips whitespace, standardizes schemes, removes default ports, decodes hex escapes, and normalizes hostname casing. |
| **30-Feature Lexical / Statistical Extractor** | [`url_features.py`](../ml-service/feature_engineering/url_features.py) | Computes 30 numeric/boolean features per URL: length attributes, symbol counts, IP-in-host, punycode, entropy, and keywords. |
| **Machine Learning Classifiers** | [`train.py`](../ml-service/training/train.py) & [`best_model.joblib`](../ml-service/models/best_model.joblib) | Scikit-learn Logistic Regression / Random Forest and XGBoost classifiers predicting binary phishing probability ($P \in [0.0, 1.0]$). |
| **Deterministic Heuristic Rules** | [`RuleEngineService.java`](../backend/src/main/java/com/phishshield/service/RuleEngineService.java) | 9 structural rules: IP hostname (+35), Suspicious TLD (+25), Punycode (+30), `@` trick (+30), Brand impersonation (+30), Hyphens (+15), Subdomains (+15), Length (+10), Entropy (+15). |
| **External Threat Intelligence** | [`ThreatIntelligenceService.java`](../backend/src/main/java/com/phishshield/threatintel/ThreatIntelligenceService.java) | Outbound reputation lookups against Google SafeBrowsing API v4 and VirusTotal API v3 with SSRF validation, TTL caching, and consent checks. |
| **Explainable Fusion Scoring** | [`MultiSignalScoringEngine.java`](../backend/src/main/java/com/phishshield/service/MultiSignalScoringEngine.java) | Fuses heuristics, ML probability, and threat intelligence into a normalized 0–100 score with explicit contribution and uncertainty tracking. |

---

## 2. Feature Extraction Deep Dive (30 URL Features)

The Python ML service extracts 30 distinct indicators without connecting to or fetching the target webpage:

1. `url_length`: Total character count of the URL.
2. `hostname_length`: Character length of the fully qualified domain name.
3. `path_length`: Length of the URI path segment.
4. `query_length`: Length of query parameters.
5. `fragment_length`: Length of anchor fragment.
6. `dot_count`: Total dots (`.`) in the URL.
7. `hyphen_count`: Hyphen (`-`) count, indicating typosquatting.
8. `underscore_count`: Underscore (`_`) frequency.
9. `slash_count`: Path depth delimiter count.
10. `question_mark_count`: Number of `?` delimiters.
11. `equal_count`: Parameter assignment delimiters.
12. `ampersand_count`: Parameter separators (`&`).
13. `percent_count`: URL percent-encoded characters (`%20`).
14. `at_symbol_count`: `@` character count (authentication bypass trick).
15. `digit_count`: Total digits across the URL string.
16. `special_character_count`: Count of non-alphanumeric symbols.
17. `subdomain_count`: Dot segments preceding the registrable domain.
18. `path_segment_count`: Slashes in URI path.
19. `query_parameter_count`: Parsed query argument count.
20. `has_https`: Boolean flag indicating TLS encryption scheme.
21. `has_ip_address`: Boolean flag indicating raw IPv4 address in hostname.
22. `has_port`: Boolean flag indicating non-standard explicit port (e.g. `:8080`).
23. `has_punycode`: Boolean flag indicating `xn--` IDN homograph encoding.
24. `has_url_shortener`: Boolean flag matching known shortener domains (`bit.ly`, `tinyurl.com`, etc.).
25. `has_encoded_characters`: Presence of percent-encoded byte sequences.
26. `suspicious_keyword_count`: Match count against 24 phishing keywords (`login`, `verify`, `kyc`, `bank`, etc.).
27. `contains_suspicious_keyword`: Binary flag indicating presence of any suspicious keyword.
28. `entropy`: Shannon entropy score of the URL string.
29. `digit_ratio`: Ratio of digits to total URL length.
30. `special_char_ratio`: Ratio of special characters to total URL length.

---

## 3. Prior Scoring Bottlenecks & Deficiencies

Prior to Phase 27, the backend combined detection signals using a naive `Math.max()` calculation:
```java
// Legacy combination:
finalScore = Math.max(ruleScore, mlScore);
if (threatIntel != null && Boolean.TRUE.equals(threatIntel.getReportedMalicious())) {
    finalScore = Math.max(finalScore, 92);
}
```

### Critical Deficiencies Identified:
1. **Double Counting of Correlated Features:**
   - Both the rule engine and the ML model extract brand keywords (`sbi`, `hdfc`, `kyc`) and structural symbols (hyphens, dots). `Math.max()` picked the higher value without explaining the mutual correlation.
2. **Missing Dependencies Treated as Negatives:**
   - When the ML service was offline, the score silently reverted to heuristics without explicitly informing the user that statistical ML classification was missing.
   - When Threat Intelligence was unconfigured or consent was withheld, the user could mistakenly infer that external reputation verified the site as safe.
3. **Lack of Uncertainty Quantification:**
   - Borderline scores (30–45) were presented with the same implied certainty as confirmed malicious sites, omitting the reality that zero-hour phishing domains may not yet be listed in external databases.
4. **Omission of Threat Intel in Frontend Modal:**
   - While `URLScanResponse` contained `threatIntel`, `ScanVerdictModal.jsx` omitted threat intelligence vendor status, cache freshness, and provider details.

---

## 4. Phase 27 Target State & Enhancements

1. **Integration of [`MultiSignalScoringEngine.java`](../backend/src/main/java/com/phishshield/service/MultiSignalScoringEngine.java):**
   - Replaces naive `Math.max()` with a multi-signal fusion algorithm that tracks `signalContributions`, `primaryDrivers`, `unavailableChecks`, and `uncertaintyFactors`.
2. **First-Class Explainability DTO:**
   - Added [`ScanExplainabilityDetails.java`](../backend/src/main/java/com/phishshield/dto/response/ScanExplainabilityDetails.java) directly into `URLScanResponse`.
3. **Frontend Explainability Presentation:**
   - Enhanced [`ScanVerdictModal.jsx`](../frontend/src/components/ScanVerdictModal.jsx) with dedicated Threat Intelligence and Explainable Rationale sections, transparently displaying unavailable checks and uncertainty caveats.
