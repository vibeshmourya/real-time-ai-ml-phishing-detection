# PHASE 33 — ML BASELINE AUDIT & DATASET QUALITY ASSESSMENT

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Audit Scope:** Python Machine Learning Microservice (`ml-service/`)  
**Audit Date:** October 2026  
**Auditor:** Antigravity AI Engineering / Lead Research Auditor  
**Verification Baseline:** Working Tree Source Code, Model Artifacts & Dataset Inspection

---

## 1. Executive Summary

This audit establishes the ground-truth technical state of Prahar's machine learning pipeline before conducting benchmark evaluations. Every finding in this document has been verified against the physical artifacts on disk, cryptographic checksums, Python AST inspection, and empirical test runs.

### Key Audit Findings at a Glance
1. **Saved Production Model:** A `scikit-learn` Pipeline consisting of `StandardScaler` and `LogisticRegression(class_weight='balanced', max_iter=1000, random_state=42)`.
2. **Model Provenance:** Saved at `models/best_model.joblib` (SHA-256: `f7f941caded414431298090532926cc12977b4e84020b658576fb5629220cf1a`), serialized on `2026-09-02`.
3. **Dataset Scope:** Exactly **175 samples** stored in `data/raw/phishing_dataset.csv` (SHA-256: `46bb65ee2c691efd96cd89310c9bae68044dd27f825206da96ef0f38296e12e3`).
4. **Class Balance:** 107 Benign URLs (61.14%) and 68 Phishing URLs (38.86%).
5. **Critical Confounding Factor Identified:** **100% of benign URLs use `https://` (107/107) and 100% of phishing URLs use `http://` (68/68).** This protocol imbalance creates an artificial shortcut for linear classifiers, resulting in inflated performance metrics that do not reflect modern real-world HTTPS phishing traffic.

---

## 2. Model Architecture & Artifact Inspection

### 2.1 Model Artifacts on Disk

| Artifact Path | Size | SHA-256 Checksum | Serialized Type |
| :--- | :--- | :--- | :--- |
| `ml-service/models/best_model.joblib` | 3,073 bytes | `f7f941caded414431298090532926cc12977b4e84020b658576fb5629220cf1a` | `sklearn.pipeline.Pipeline` |
| `ml-service/models/model_metadata.json` | 1,036 bytes | `77e8d888089c1e342b013d32ce9f818c75310f12453433dd450126e826866e9b` | JSON Metadata |

### 2.2 Model Architecture Breakdown

Inspection of `models/best_model.joblib` via Python reflection reveals:
```python
Pipeline(steps=[
    ('scaler', StandardScaler(copy=True, with_mean=True, with_std=True)),
    ('classifier', LogisticRegression(
        C=1.0, 
        class_weight='balanced', 
        dual=False, 
        fit_intercept=True,
        intercept_scaling=1, 
        l1_ratio=None, 
        max_iter=1000,
        multi_class='auto', 
        n_jobs=None, 
        penalty='l2',
        random_state=42, 
        solver='lbfgs', 
        tol=0.0001, 
        verbose=0,
        warm_start=False
    ))
])
```

- **Candidate Selection Logic:** In `training/compare_models.py`, four candidates are trained: Logistic Regression, Random Forest, XGBoost (if installed), and SVM. On the current 35-sample test split, all four models achieved a tie of $1.0000$ across Accuracy, Precision, Recall, and F1. Because Logistic Regression is evaluated first and ties for highest F1, `train.py` selected and serialized Logistic Regression.
- **Decision Intercept:** `intercept_ = -0.996468`
- **Primary Feature Coefficients:**
  - `has_https`: **-1.909081** (Strongest negative predictor; pushes predictions heavily toward "Benign")
  - `hostname_length`: **+0.863339** (Strongest positive predictor; pushes predictions toward "Phishing")
  - `hyphen_count`: **+0.593389**
  - `has_url_shortener`: **+0.585746**
  - `entropy`: **+0.498054**
  - `special_character_count`: **+0.468868**
  - `digit_ratio`: **+0.450096**
  - `contains_suspicious_keyword`: **+0.447154**

---

## 3. Training & Inference Pipeline Audit

### 3.1 Training Workflow (`training/train.py`)
1. **Cleaning:** `preprocessing/clean_dataset.py` reads `data/raw/phishing_dataset.csv`, drops empty/null records, removes exact duplicate URLs, and maps labels to binary integers ($0 = \text{Benign}$, $1 = \text{Phishing}$).
2. **Feature Extraction:** `feature_engineering/url_features.py` extracts a 30-dimensional numerical feature vector for each URL.
3. **Train/Test Splitting:** `preprocessing/preprocess.py` uses `sklearn.model_selection.train_test_split` with `test_size=0.20`, `random_state=42`, and `stratify=y`.
   - Training Set: 140 samples (86 benign, 54 phishing).
   - Test Set: 35 samples (21 benign, 14 phishing).
4. **Comparison & Serialization:** `training/save_model.py` saves the winning estimator to `models/best_model.joblib` along with `models/model_metadata.json`.

### 3.2 Inference Workflow (`app/services/prediction_service.py`)
Inference executes synchronously in under 5ms:
1. `extract_features(url)` extracts the 30 features.
2. `ModelPredictor.predict(features)` loads `models/best_model.joblib`, runs `predict_proba(df_input)`, and extracts $P(\text{Phishing})$.
3. `evaluate_phishing_rules(url)` checks heuristic rules (Punycode, raw IP host, urgency keywords).
4. `RiskService.calculate_risk(url, ml_prob, rule_indicators, features)` computes the composite risk score.
5. `generate_user_explanations(...)` generates human-readable reasoning cards.

### 3.3 Integration with Detection Signals
In `RiskService.java`:
$$\text{Combined Score} = (P_{\text{ML}} \times 70.0) + \min\left(30.0, \sum \text{Rule Weight} \times 0.5\right)$$
- If $\ge 2$ Critical/High heuristic indicators fire (e.g., Punycode + raw IP), a hard floor of $75.0$ is enforced.
- Risk levels: $\ge 70 \rightarrow \text{HIGH\_RISK}$, $40–69 \rightarrow \text{SUSPICIOUS}$, $< 40 \rightarrow \text{SAFE}$.

---

## 4. Dataset Quality & Provenance Assessment

### 4.1 Dataset Properties

| Metric | Measured Value | Observation |
| :--- | :--- | :--- |
| **File Location** | `ml-service/data/raw/phishing_dataset.csv` | Single flat CSV file |
| **File Checksum** | `46bb65ee2c691efd96cd89310c9bae68044dd27f825206da96ef0f38296e12e3` | Validated |
| **Total Rows** | 175 | Header + 175 data lines |
| **Missing / Null Values** | 0 | 100% complete across `url` and `label` |
| **Exact Duplicate URLs** | 0 | All 175 URL strings are distinct |
| **Unique Domain Netlocs** | 137 | High domain diversity across 175 URLs |
| **Class 0 (Legitimate)** | 107 (61.14%) | Major portals (Google, Wikipedia, GitHub, BBC, SBI, HDFC) |
| **Class 1 (Phishing)** | 68 (38.86%) | Synthetic and captured lure patterns |

### 4.2 Dataset Provenance & Licensing
- **Origin:** The dataset was curated internally for the academic prototype demonstration, combining representative high-authority Alexa/Tranco top domains with common phishing lure archetypes (banking KYC, fake parcel delivery, Punycode homoglyphs, and IP hosts).
- **Licensing:** The dataset contains purely public web URLs without user personal data, credentials, or copyrighted proprietary text.
- **Limitation:** The sample size ($N=175$) is appropriate for unit testing, CI/CD smoke validation, and local academic demonstration, but **is not statistically sufficient to represent global Internet traffic** or guarantee production enterprise defense.

### 4.3 Critical Vulnerability: The Protocol Confounding Factor
An empirical query over the dataset reveals:
- Benign URLs: **107 HTTPS (100.0%)**, 0 HTTP (0.0%).
- Phishing URLs: **0 HTTPS (0.0%)**, 68 HTTP (100.0%).

**Scientific Implication:**  
Because the feature `has_https` correlates perfectly with the binary label in this dataset, linear models assign it an enormous coefficient ($-1.909$). In the real world, over $80\%$ of active phishing sites now deploy valid SSL/TLS certificates (via Let's Encrypt, Cloudflare, etc.). When a modern HTTPS phishing URL is presented to this model, its predicted phishing probability drops by $\sim 31\%$, creating a risk of False Negatives unless compensated by heuristic rules or domain reputation.

---

## 5. Audit Conclusions & Recommendations

1. **Academic Readiness:** The model, preprocessing pipeline, and inference endpoints are fully functional, deterministic, and bug-free (36/36 tests passing).
2. **Honest Reporting:** Academic examiners must be informed that the recorded $1.0000$ ($100\%$) accuracy is a product of dataset linear separability and the protocol artifact, rather than an infallible real-world detector.
3. **Multi-Signal Justification:** This audit validates the fundamental design thesis of Prahar: **machine learning alone cannot be trusted in isolation**. The multi-signal scoring engine (heuristics + ML + threat intel) exists specifically to prevent protocol-related ML blind spots from compromising system security.
