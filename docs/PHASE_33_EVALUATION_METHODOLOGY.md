# PHASE 33 — LEAKAGE-RESISTANT EVALUATION METHODOLOGY

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Subject:** Evaluation Protocol, Leakage Prevention & Statistical Framework  
**Verification Target:** Reproducible Benchmark on `ml-service/`

---

## 1. Evaluation Protocol Architecture

To ensure strict scientific integrity, prevent data snooping, and eliminate data leakage, the Phase 33 evaluation protocol adheres to the following experimental standards:

```
+---------------------------------------------------------------------------------------+
|                       LEAKAGE-RESISTANT BENCHMARK PIPELINE                            |
+---------------------------------------------------------------------------------------+
                                  |
            [ Raw Dataset: phishing_dataset.csv (175 URLs) ]
             SHA-256: 46bb65ee2c691efd96cd89310c9bae68044dd27f...
                                  |
                                  v
+---------------------------------------------------------------------------------------+
| [ STEP 1: PRE-SPLIT VERIFICATION & DEDUPLICATION ]                                    |
|   - Null / empty value inspection: 0 records dropped                                  |
|   - Exact string deduplication: 175 distinct URLs preserved                           |
|   - Canonical label mapping: 0 = Benign (107), 1 = Phishing (68)                      |
+---------------------------------------------------------------------------------------+
                                  |
                                  v
+---------------------------------------------------------------------------------------+
| [ STEP 2: STRATIFIED HELD-OUT SPLIT (Seed = 42) ]                                     |
|   - Train Split: 80% (140 samples: 86 Benign, 54 Phishing)                           |
|   - Held-Out Test Split: 20% (35 samples: 21 Benign, 14 Phishing)                     |
|   - Immutability: Test set is strictly isolated from model fitting                    |
+---------------------------------------------------------------------------------------+
                                  |
                                  v
+---------------------------------------------------------------------------------------+
| [ STEP 3: STATELESS FEATURE EXTRACTION ]                                              |
|   - 30 lexical, structural, and syntactic features computed per URL string            |
|   - No global dataset statistics, corpus frequencies, or target labels used           |
+---------------------------------------------------------------------------------------+
                                  |
                                  v
+---------------------------------------------------------------------------------------+
| [ STEP 4: PREPROCESSING ISOLATION ]                                                   |
|   - StandardScaler fit strictly on Training Matrix X_train                            |
|   - Parameters mu_train and sigma_train applied to X_test without refitting           |
+---------------------------------------------------------------------------------------+
                                  |
                                  v
+---------------------------------------------------------------------------------------+
| [ STEP 5: COMPARATIVE BENCHMARK EVALUATION ]                                          |
|   Evaluate 4 Candidate Paradigms on identical test split:                             |
|     1. Majority Class Baseline (Empirical Zero-Intelligence Reference)                |
|     2. Simple Transparent Rule-Based Baseline (Uncalibrated Heuristics)               |
|     3. Current ML Model (Logistic Regression Pipeline)                                |
|     4. Current Combined Detector (RiskService Signal Fusion)                          |
+---------------------------------------------------------------------------------------+
                                  |
                                  v
+---------------------------------------------------------------------------------------+
| [ STEP 6: STATISTICAL INFERENCE & UNCERTAINTY QUANTIFICATION ]                         |
|   - Confusion Matrix (TN, FP, FN, TP)                                                 |
|   - Accuracy, Precision, Recall, F1, ROC-AUC                                          |
|   - False Positive Rate (FPR) & False Negative Rate (FNR)                             |
|   - 95% Confidence Intervals via Wilson Score Formulation                             |
|   - 5-Fold Stratified & GroupKFold (Domain-Grouped) Cross-Validation                  |
+---------------------------------------------------------------------------------------+
```

---

## 2. Leakage Prevention Controls

Data leakage occurs when information outside the training dataset influences model training or preprocessing, resulting in overly optimistic evaluation metrics that fail to generalize. Prahar enforces four explicit leakage prevention controls:

### Control 1: Isolation of Feature Transformation Parameters
- Feature scaling relies on `StandardScaler`:
  $$z = \frac{x - \mu}{\sigma}$$
- The mean ($\mu$) and standard deviation ($\sigma$) are fitted **strictly on the 140 training samples**.
- The test set is transformed using these frozen training statistics. At no point is `StandardScaler.fit()` or `fit_transform()` invoked on the combined or test data.

### Control 2: Stateless Feature Extraction
- The 30 features extracted by `feature_engineering/url_features.py` are **stateless and deterministic**.
- Features depend exclusively on the individual URL string (character counts, Shannon entropy, regex matches). No cross-URL corpus frequencies (such as IDF weights) or dataset-wide n-gram distributions are used.

### Control 3: Domain Grouping Analysis (GroupKFold)
- In addition to standard stratified splitting, the evaluation protocol executes **5-fold GroupKFold cross-validation** grouped by apex netloc (`urlparse(url).netloc`).
- This guarantees that URLs belonging to the same root domain (e.g., `google.com/search` and `google.com/maps`) reside exclusively in either the training fold or the testing fold, validating that the model does not merely memorize specific domain names.

### Control 4: Fixed Evaluation Artifacts
- The model artifact `models/best_model.joblib` is loaded read-only.
- The evaluation script `training/benchmark_evaluation.py` and test suite `tests/test_benchmark_reproducibility.py` verify via SHA-256 checksums that running the benchmark does not mutate or overwrite the production model.

---

## 3. Mathematical Metric Definitions

The evaluation records the full suite of classification and operational security metrics:

### 3.1 Confusion Matrix Quantities
- **True Positive ($TP$):** Phishing URL correctly classified as Phishing.
- **True Negative ($TN$):** Legitimate URL correctly classified as Benign.
- **False Positive ($FP$):** Legitimate URL mistakenly flagged as Phishing (Type I Error).
- **False Negative ($FN$):** Phishing URL mistakenly classified as Benign (Type II Error).

### 3.2 Performance Metrics
$$\text{Accuracy} = \frac{TP + TN}{TP + TN + FP + FN}$$

$$\text{Precision} = \frac{TP}{TP + FP} \quad (\text{with zero division } = 0)$$

$$\text{Recall (Sensitivity)} = \frac{TP}{TP + FN}$$

$$\text{F1 Score} = 2 \times \frac{\text{Precision} \times \text{Recall}}{\text{Precision} + \text{Recall}} = \frac{2TP}{2TP + FP + FN}$$

### 3.3 Security & Risk Operational Metrics
$$\text{False Positive Rate (FPR)} = \frac{FP}{FP + TN} = 1 - \text{Specificity}$$

$$\text{False Negative Rate (FNR)} = \frac{FN}{FN + TP} = 1 - \text{Recall}$$

- **Operational Significance of FPR:** A high FPR blocks legitimate banking portals and customer utility sites, causing severe user frustration, alert fatigue, and operational support overhead.
- **Operational Significance of FNR:** A high FNR lets credential harvesters and malware distributors pass through undetected, directly compromising user credentials and financial assets.

---

## 4. Confidence Interval Formulation (Wilson Score Interval)

Because the held-out test split contains $N = 35$ samples, point estimates of accuracy must be accompanied by confidence intervals to quantify sample size uncertainty. For binomial proportions with small sample sizes, the standard normal approximation ($\hat{p} \pm z\sqrt{\hat{p}(1-\hat{p})/n}$) produces invalid intervals near boundaries (such as $\hat{p} = 1.0$).

We utilize the **Wilson Score Interval** with 95% confidence ($\alpha = 0.05, z = 1.96$):

$$w = \frac{\hat{p} + \frac{z^2}{2n} \pm z \sqrt{\frac{\hat{p}(1 - \hat{p})}{n} + \frac{z^2}{4n^2}}}{1 + \frac{z^2}{n}}$$

Where:
- $n = 35$ (test samples)
- $\hat{p} = \frac{k}{n}$ (sample accuracy)

For $\hat{p} = 1.0000$ ($k = 35, n = 35$):
- Center adjusted: $p_{\text{adj}} = \frac{1.0 + \frac{1.96^2}{70}}{1 + \frac{1.96^2}{35}} = \frac{1.0549}{1.1098} \approx 0.9505$
- Lower bound: **$0.9011$ ($90.11\%$)**
- Upper bound: **$1.0000$ ($100.00\%$)**

This reveals that while the point accuracy on this test set is $100\%$, the true underlying population accuracy is statistically bounded between **$90.1\%$ and $100.0\%$** at the 95% confidence level.

---

## 5. Baselines for Comparison

| Baseline Identifier | Description & Classification Rule | Rationale for Inclusion |
| :--- | :--- | :--- |
| **Majority Class Baseline** | Always predicts label $0$ (Benign). $\hat{y} = 0, \forall x$. | Essential statistical sanity check; represents zero-intelligence performance. |
| **Simple Rule-Based Baseline** | Predicts Phishing if $\ge 1$ high-risk heuristic fires (`has_ip_address`, `has_punycode`, `has_url_shortener`, `contains_suspicious_keyword`, `hyphen_count >= 3`, or `entropy >= 4.0`). | Tests whether a transparent, non-ML heuristic set achieves comparable detection. |
| **Current ML Model** | Logistic Regression Pipeline (`StandardScaler` + `LogisticRegression`). Predicts Phishing if $P(\text{Phishing}) \ge 0.50$. | Measures the standalone contribution of linear machine learning on 30 features. |
| **Current Combined Detector** | `RiskService.calculate_risk(url, ml_prob, rule_indicators, features)`. Predicts Phishing if Risk Score $\ge 50$. | Measures the actual production hybrid pipeline combining ML and heuristic signals. |
