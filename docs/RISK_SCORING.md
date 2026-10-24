# Prahar 2.0 — Intelligent Explainable Risk Scoring Methodology

## 1. Mathematical Scoring Formulation & Normalization

Prahar normalizes all threat analysis results to a bounded, integer risk score:
$$\text{Score} \in [0, 100]$$

Mapped directly to three risk level bands:
- **`0 – 29` (SAFE / LOW RISK):** No significant deceptive patterns observed; standard hygiene advised.
- **`30 – 69` (SUSPICIOUS):** Anomalous lexical patterns or moderate statistical risk detected; caution required.
- **`70 – 100` (HIGH RISK):** High-confidence phishing, credential theft, or verified threat intelligence match; avoid opening.

> **Important Disclosure:** Prahar's risk score is an **evidence-based composite heuristic score**, not a calibrated statistical probability. No automated scanner can guarantee 100% mathematical certainty.

---

## 2. Signal Inventory & Weighting Mechanics

Prahar evaluates four concrete, verified signals available in the application runtime:

```text
                               MULTI-SIGNAL FUSION ENGINE
    ┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐
    │  Heuristic Rules     │   │  FastAPI ML Model    │   │ Threat Intelligence  │
    │  (9 Structural Rules)│   │  (30 Feature Vectors)│   │ (External Vendors)   │
    └──────────┬───────────┘   └──────────┬───────────┘   └──────────┬───────────┘
               │                          │                          │
               │ (Subtotal: 0 - 85)       │ (Probability: 0.0 - 1.0) │ (Confirmed / Clean)
               ▼                          ▼                          ▼
    ┌────────────────────────────────────────────────────────────────────────────┐
    │                       MultiSignalScoringEngine                             │
    │  - Threat Intel Dominance Rule (Sets Score >= 90 if Verified Malicious)    │
    │  - Correlation De-duplication (Prevents Keyword Double-Counting)           │
    │  - Heuristic Safety Override (Low ML probability cannot mask homoglyphs)   │
    │  - Missing Data Isolation (Tracks Unavailable Checks vs. Negative Finding) │
    └─────────────────────────────────────┬──────────────────────────────────────┘
                                          │
                                          ▼
                         Composite Result & Explainability DTO
```

### Signal 1: External Threat Intelligence ($S_{TI}$)
- **When Positive (`status == "MALICIOUS"`):**
  Verified malicious listing by an external threat intelligence vendor (Google SafeBrowsing or VirusTotal). Overrides baseline scoring:
  $$\text{Score} = \max(90, \min(99, \text{Confidence}))$$
- **When Clean (`status == "CLEAN"`):**
  Contributes 0 points. **Does not force the score to 0**, as zero-hour phishing campaigns are rarely indexed immediately.
- **When Unavailable / Disabled (`status == "NOT_CONFIGURED" | "DISABLED"`):**
  Added explicitly to `unavailableChecks`. 0 point contribution.

### Signal 2: Deterministic Heuristic Indicators ($S_{H}$)
Computed in [`RuleEngineService.java`](../backend/src/main/java/com/phishshield/service/RuleEngineService.java):

| Rule Name | Severity | Weight | Rationale |
| :--- | :--- | :--- | :--- |
| `RULE_IP_HOST` | HIGH | +35 | Numeric IP hostname bypasses domain reputation filters. |
| `RULE_SUSPICIOUS_TLD` | HIGH | +25 | High-abuse free TLDs (`.top`, `.xyz`, `.tk`) heavily used in disposable phishing. |
| `RULE_PUNYCODE` | CRITICAL | +30 | `xn--` encoding visually mimics authentic brand characters (homoglyphs). |
| `RULE_AT_SYMBOL` | HIGH | +30 | `@` character tricks browsers into ignoring preceding brand text. |
| `RULE_BRAND_IMPERSONATION` | CRITICAL | +30 | Sensitive brand (`sbi`, `hdfc`, `kyc`) hosted on an unofficial root domain. |
| `RULE_EXCESSIVE_HYPHENS` | MEDIUM | +15 | $\ge 3$ hyphens frequently indicates typo-squatting. |
| `RULE_DEEP_SUBDOMAINS` | MEDIUM | +15 | $\ge 3$ dot segments used to hide the true registrable domain. |
| `RULE_URL_LENGTH` | LOW | +10 | $> 85$ characters used to push malicious parameters off-screen. |
| `RULE_HIGH_ENTROPY` | MEDIUM | +15 | Shannon entropy $> 4.1$ characteristic of algorithmically generated domains. |

*Bounding Rule:* Heuristic subtotal is capped at **85 points** to reserve maximum scores for corroborated or threat-intel confirmed threats.

### Signal 3: Machine Learning Model Probability ($S_{ML}$)
- Extracted across 30 URL features in FastAPI microservice: $P_{phish} \in [0.0, 1.0]$.
- Standard score conversion: $S_{ML} = \text{round}(P_{phish} \times 100)$.

---

## 3. Anti-Double-Counting & Fusion Logic

Because both the heuristic engine and the ML model extract correlated features (e.g., brand keywords, hyphens, and dot counts), simply summing them would lead to severe score inflation and false positives.

[`MultiSignalScoringEngine.java`](../backend/src/main/java/com/phishshield/service/MultiSignalScoringEngine.java) resolves this through four fusion rules:

1. **Reinforcing Consensus ($S_{H} \ge 50$ and $S_{ML} \ge 50$):**
   Both engines independently identify high threat:
   $$\text{Score} = \max(S_H, S_{ML})$$
   *Rationale:* Both engines confirm deceptive signals; score reflects the highest confirmed evidence.
2. **Heuristic Safety Override ($S_{H} \ge 40$ and $S_{ML} < 40$):**
   Heuristic rules detect clear deception (e.g. brand impersonation or punycode), but ML model is conservative:
   $$\text{Score} = S_H$$
   *Rationale:* A statistical model trained on general data must never suppress a deterministic structural deception.
3. **ML-Driven Detection ($S_{H} < 30$ and $S_{ML} \ge 60$):**
   ML model detects anomalous feature distributions not captured by static keyword rules:
   $$\text{Score} = \text{round}(0.3 \times S_H + 0.7 \times S_{ML})$$
   *Rationale:* Statistical patterns flag zero-hour anomalies.
4. **Offline Fallback ($S_{ML}$ unavailable):**
   $$\text{Score} = S_H$$
   Recorded in `unavailableChecks`: *"Machine Learning Inference (ML Service Offline)"*.

---

## 4. Uncertainty & Inconclusiveness

To maintain scientific honesty, Prahar explicitly flags uncertain or inconclusive assessments:

- **Borderline Inconclusive Condition:**
  If the composite score falls between **30 and 45**, and **both ML and Threat Intelligence are unavailable**, the engine sets:
  ```json
  "inconclusive": true,
  "uncertaintyFactors": [
    "Score is borderline (30-45) and both ML and Threat Intelligence checks were unavailable."
  ]
  ```
- **Zero-Hour Caveat:**
  If threat intelligence returns `CLEAN`, Prahar automatically records:
  *"Threat intelligence found no known listings; zero-hour phishing domains may not yet be indexed."*
