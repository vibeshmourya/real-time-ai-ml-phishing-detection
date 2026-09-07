# PhishShield Machine Learning Methodology & Architecture

This document outlines the machine learning architecture, feature engineering framework, mathematical foundations, candidate models, evaluation methodology, and explainability mechanisms employed in the **Real-Time AI/ML-Based Phishing Detection and Prevention System**.

---

## 1. Threat Detection Strategy

PhishShield employs a **Hybrid Defense-in-Depth Architecture**:
1. **Deterministic SSRF Shield & Normalization**: Sanitizes input, blocks private networks/metadata endpoints, and standardizes canonical URL representations.
2. **Canonical Feature Extraction**: Computes a 30-dimensional numerical vector capturing lexical lengths, character distributions, structural depths, security markers, and linguistic keywords.
3. **Multi-Model Machine Learning Classification**: Evaluates statistical combinations of features using calibrated probabilities from supervised classifiers.
4. **Transparent Rule-Based Heuristics**: Applies security domain rules (e.g., Punycode homoglyphs, IP addresses, `@` symbol evasion, unencrypted authentication).
5. **Calibrated Risk Engine & Explainability**: Blends ML probabilities and rule indicators into an actionable score (0–100) and produces accessible, non-technical explanations.

```
Incoming URL
    │
    ▼
[ URL Normalization & SSRF Defense ]
    │
    ├──▶ [ 30-Feature Extraction Vector ] ──▶ [ Trained ML Classifier ] ──▶ (ML Probability)
    │                                                                               │
    └──▶ [ Heuristic Phishing Rules ]     ──▶ [ Rule Indicators List ] ──────────────┼──▶ [ Risk Scoring & Threat Category ]
                                                                                    │
                                                                                    ▼
                                                                     [ Explainability Engine ]
                                                                                    │
                                                                                    ▼
                                                                     Structured JSON Prediction
```

---

## 2. Mathematical Definition: Shannon Character Entropy

Shannon Entropy quantifies the uncertainty and randomness in a character sequence. Attackers commonly use Domain Generation Algorithms (DGA) or random token stuffing to generate disposable phishing hostnames (e.g., `qx7z9k2m1p4w8v3y6t5r0j.xyz`), resulting in significantly higher entropy than natural language domains.

### Formula
For a given string $S$ of length $N$, the Shannon Entropy $H(S)$ is defined as:

$$H(S) = -\sum_{i=1}^{k} P(c_i) \log_2 P(c_i)$$

Where:
- $k$ is the number of distinct characters in $S$.
- $c_i$ is the $i$-th distinct character.
- $P(c_i) = \frac{\text{count}(c_i)}{N}$ represents the empirical probability (frequency ratio) of character $c_i$ in string $S$.

### Implementation in Python
```python
import math
from collections import Counter

def calculate_shannon_entropy(text: str) -> float:
    if not text:
        return 0.0
    length = len(text)
    counts = Counter(text)
    entropy = 0.0
    for count in counts.values():
        p = count / length
        entropy -= p * math.log2(p)
    return round(entropy, 4)
```

**Interpretation:**
- Legitimate english domains (e.g. `google.com`, `wikipedia.org`) typically exhibit entropy between **2.0 and 3.5**.
- Algorithmic and obfuscated domains typically exhibit entropy **$> 4.0$**.

---

## 3. Canonical Feature Vector (30 Features)

| Index | Feature Name | Type | Description |
|---|---|---|---|
| 1 | `url_length` | Integer | Total length of the URL string |
| 2 | `hostname_length` | Integer | Length of the domain/hostname |
| 3 | `path_length` | Integer | Length of the URL path |
| 4 | `query_length` | Integer | Length of query string parameters |
| 5 | `fragment_length` | Integer | Length of hash fragment identifier |
| 6 | `dot_count` | Integer | Number of `.` characters |
| 7 | `hyphen_count` | Integer | Number of `-` characters in domain/path |
| 8 | `underscore_count` | Integer | Number of `_` characters |
| 9 | `slash_count` | Integer | Number of `/` separators |
| 10 | `question_mark_count` | Integer | Number of `?` characters |
| 11 | `equal_count` | Integer | Number of `=` characters |
| 12 | `ampersand_count` | Integer | Number of `&` characters |
| 13 | `percent_count` | Integer | Number of `%` encoding characters |
| 14 | `at_symbol_count` | Integer | Number of `@` characters |
| 15 | `digit_count` | Integer | Count of numerical digits (0-9) |
| 16 | `special_character_count` | Integer | Count of non-alphanumeric special symbols |
| 17 | `subdomain_count` | Integer | Number of subdomain hierarchy tiers |
| 18 | `path_segment_count` | Integer | Number of path directory components |
| 19 | `query_parameter_count` | Integer | Number of distinct query key-value pairs |
| 20 | `has_https` | Binary (0/1) | Whether protocol is HTTPS |
| 21 | `has_ip_address` | Binary (0/1) | Whether host is an IPv4 address |
| 22 | `has_port` | Binary (0/1) | Whether a non-standard port is specified |
| 23 | `has_punycode` | Binary (0/1) | Whether domain contains `xn--` encoding |
| 24 | `has_url_shortener` | Binary (0/1) | Whether host belongs to a known shortener |
| 25 | `has_encoded_characters` | Binary (0/1) | Whether URL contains percent-encoding |
| 26 | `suspicious_keyword_count`| Integer | Number of matched security/action keywords |
| 27 | `contains_suspicious_keyword` | Binary (0/1) | Boolean indicator for phishing keywords |
| 28 | `entropy` | Float | Shannon character entropy score |
| 29 | `digit_ratio` | Float | Ratio of digits to total URL length |
| 30 | `special_char_ratio` | Float | Ratio of special characters to URL length |

---

## 4. Machine Learning Algorithms & Training Strategy

The training pipeline evaluates 4 distinct algorithms using 5-fold stratified cross-validation and independent test splits:

1. **Logistic Regression (with StandardScaler)**
   - Linear baseline with calibrated log-odds probability.
   - Provides transparent feature coefficients.
2. **Random Forest Classifier (Ensemble Trees)**
   - 100 estimators with balanced class weights.
   - Captures non-linear feature interactions without requiring normalization.
3. **XGBoost Classifier (Gradient Boosted Trees)**
   - Sequential boosting optimizing cross-entropy log-loss.
   - Handles imbalanced distributions effectively.
4. **Support Vector Machine (RBF Kernel with Scaling)**
   - Non-linear margin classifier with Platt scaling for probability estimates.

### Model Selection Metric
Models are ranked using **F1-Score** and **ROC-AUC** to balance precision (preventing false alarms on benign sites) and recall (catching active phishing lures).

---

## 5. Explainability Architecture

To ensure decisions can be understood by everyday non-technical users, the `explainer.py` engine translates statistical feature activations into intuitive, human-understandable explanations:

- **Technical:** `has_ip_address = 1`  
  **User-Facing:** *"The link uses a raw IP address instead of a recognized domain name, which is commonly seen on evasive scam servers."*
- **Technical:** `has_punycode = 1`  
  **User-Facing:** *"The web address uses look-alike international characters (punycode) that can visually disguise an impersonation domain."*
- **Technical:** `at_symbol_count > 0`  
  **User-Facing:** *"The address includes an '@' symbol, which causes modern web browsers to ignore everything before it and redirect to an unexpected destination."*

---

## 6. Known Limitations

1. **URL-Only Analysis**: Static URL feature analysis operates strictly on the address string. In Phase 3, no live HTML body crawling, DOM inspection, or OCR is performed (safeguarding against remote code execution and SSRF).
2. **Shortened URLs**: Masked URLs (e.g. `bit.ly`) conceal the true landing page. Safe unshortening will be integrated in future pipeline iterations.
3. **Evolving Phishing Infrastructure**: Newly registered legitimate-looking top-level domains may have lower lexical risk until reputation threat feeds are queried.
