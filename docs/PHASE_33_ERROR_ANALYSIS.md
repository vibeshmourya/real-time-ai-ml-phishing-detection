# PHASE 33 — QUALITATIVE ERROR ANALYSIS & RESILIENCE AUDIT

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Subject:** Failure Mode Categorization, Adversarial Edge Cases & Evidence-Based Recommendations  
**Test Baseline:** Empirical Experiments on `ml-service/` Model & Heuristic Rules

---

## 1. Executive Summary

While the standalone ML model and combined detector achieve $1.0000$ ($100\%$) accuracy on the current 35-sample test split, qualitative failure mode analysis reveals distinct operational edge cases. 

This error analysis examines representative authorized, non-sensitive fixtures across four primary risk categories to understand where static lexical models degrade and how Prahar's multi-signal architecture mitigates those degradation paths.

---

## 2. Failure Mode Categorization & Empirical Evidence

---

### CATEGORY 1: The "HTTPS Phishing" Blind Spot (Phishing URLs Resembling Legitimate Sites)

- **Mechanics:**  
  Historically, phishing websites operated over plaintext HTTP because obtaining SSL/TLS certificates required identity verification or payment. Modern certificate authorities (Let's Encrypt, Cloudflare Universal SSL) issue free, automated certificates in seconds. Consequently, over $80\%$ of active phishing campaigns operate over HTTPS.
- **Empirical Test:**  
  We evaluated the existing serialized model (`models/best_model.joblib`) on identical lure strings with and without HTTPS:
  ```text
  Test URL A: http://secure-login-hdfc-netbanking.tk/verify
  Result:     P(Phishing) = 0.9906 (99.06% Confidence)

  Test URL B: https://secure-login-hdfc-netbanking.tk/verify
  Result:     P(Phishing) = 0.6799 (67.99% Confidence)  [Drop: -31.07%]
  ```
- **Root Cause:**  
  The feature coefficient for `has_https` in the logistic regression model is **$-1.909081$** because the raw training dataset contained 100% HTTPS benign URLs and 100% HTTP phishing URLs. The model treats HTTPS as a powerful signal of legitimacy.
- **System Defense in Prahar:**  
  The standalone ML probability drops from $0.99$ to $0.68$, but **does not flip to false negative** because lexical length and keyword indicators compensate. Furthermore, in Prahar's `MultiSignalScoringEngine.java` and `RiskService.java`, the presence of brand tokens in unauthorized subdomains triggers a **heuristic floor of $75/100$**, preventing the link from being presented as Safe to the user.

---

### CATEGORY 2: Complex Legitimate URLs with Suspicious Structural Signals (False Positive Risk)

- **Mechanics:**  
  Modern web development, documentation sites, and code repositories feature URLs with deep path segments, version strings, hexadecimal git commit hashes, hyphens, and security keywords.
- **Representative Examples from Dataset:**
  1. `https://raw.githubusercontent.com/user/repo/main/README.md` (Multiple subdomains, slashes, raw token)
  2. `https://developer.mozilla.org/en-US/docs/Web/Security` (Contains keyword "security", hyphens)
  3. `https://fastapi.tiangolo.com/tutorial/first-steps` (Hyphenated, tutorial keyword)
- **Empirical Observation:**  
  In our baseline comparison, the **Simple Rule-Based Baseline misclassified 11 out of 21 legitimate URLs** ($FPR = 52.38\%$) because its uncalibrated rules flagged entropy and hyphens.
- **How the Current ML Model Resolves This:**  
  The logistic regression model correctly assigns probabilities $< 0.15$ to these URLs because its negative weights on path structure and HTTPS counterbalance the raw character counts.
- **System Defense in Prahar:**  
  Prahar implements **Fast-Path Whitelisting** (`PhishingRuleEngine.java`) for verified domains (e.g., `github.com`, `mozilla.org`), completely bypassing risk score accumulation on authentic infrastructure.

---

### CATEGORY 3: Legacy Plaintext HTTP Legitimate Sites

- **Mechanics:**  
  While rare today, certain educational archives, legacy government portals, and internal intranet appliances operate over plaintext `http://`.
- **Empirical Test:**  
  We evaluated the historical CERN website:
  ```text
  Test URL: http://info.cern.ch/hypertext/WWW/TheProject.html
  Result:   Prediction = 0 (SAFE) | P(Phishing) = 0.0954
  ```
- **Finding:**  
  Despite the lack of HTTPS, the model correctly predicts `SAFE` ($9.54\%$ risk) because the URL lacks brand spoofing keywords, high entropy, Punycode characters, and suspicious TLD suffixes. This demonstrates that the model does not blindly classify all HTTP URLs as phishing.

---

### CATEGORY 4: URL Shorteners & Indirection Services (Missing Context)

- **Mechanics:**  
  Attackers frequently use URL shortening services (`bit.ly`, `tinyurl.com`, `is.gd`, `cutt.ly`) in SMS and social media lures to conceal the destination domain.
- **Representative Examples from Dataset:**
  - `http://bit.ly/3xXBankVerifyKYCNow`
  - `http://tinyurl.com/sbi-urgent-account-unblock`
- **Current Model Behavior:**  
  The feature `has_url_shortener` has a strong positive coefficient ($+0.5857$), correctly flagging these shorteners as suspicious ($P \ge 0.90$).
- **Inherent Limitation:**  
  Because Prahar operates **statically without active outbound HTTP expansion**, it cannot inspect the post-redirect destination server without making an outbound HTTP request. If a benign user shares a shortened link to a legitimate article, the static system cannot distinguish it from a shortened phishing lure based on lexical cues alone.

---

## 5. Evidence-Based Research Recommendations

Based strictly on the observed empirical findings, we recommend the following enhancements for future research phases:

### Recommendation 1: Dataset Rebalancing & Protocol Neutralization
- **Action:** Expand the training dataset by ingesting modern active phishing feeds from OpenPhish and PhishTank that utilize HTTPS certificates.
- **Goal:** Equalize the protocol distribution ($50\%$ HTTPS in benign, $50\%$ HTTPS in phishing) so the model learns that HTTPS indicates cryptographic transport encryption, not domain trustworthiness.

### Recommendation 2: Dataset Scale Expansion ($N \ge 50,000$)
- **Action:** Scale the dataset from 175 samples to an industry-standard corpus of $\ge 50,000$ verified URLs.
- **Goal:** Enable the training of non-linear gradient-boosted tree ensembles (XGBoost / LightGBM) without risk of small-sample overfitting, narrowing the 95% confidence interval width from $\pm 10\%$ to $\pm 0.3\%$.

### Recommendation 3: Safe Server-Side HTTP Head Pre-Flight
- **Action:** For verified URL shorteners (`bit.ly`, `tinyurl.com`), implement an isolated pre-flight HTTP HEAD request with redirects disabled (`follow_redirects=False`) and SSRF guards active to extract the `Location` header.
- **Goal:** Recursively feed the unmasked destination URL back into the static feature extraction pipeline.

### Recommendation 4: Model Governance Promotion Gate
- **Action:** Preserve the governance gate implemented in `data_pipeline/governance.py` (`DEFAULT_PROMOTION_THRESHOLDS`), requiring candidate models to demonstrate:
  - $\text{F1} \ge 0.85$
  - $\text{FPR} \le 0.03$ (Maximum 3% false alarm rate on benign links)
  - $\text{Recall} \ge 0.85$
  - Model promotion requires human authorization rather than automatic overwrites.
