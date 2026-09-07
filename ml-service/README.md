# PhishShield Machine Learning Microservice

Production-ready Python FastAPI microservice for real-time URL feature extraction, multi-model machine learning inference, heuristic indicator evaluation, and explainable cybersecurity risk assessment.

---

## 1. Architecture Overview

```
Incoming URL (Spring Boot Backend / Client)
       │
       │ HTTP POST /predict
       ▼
FastAPI Request Validation (Pydantic)
       │
       ├── 1. Canonical Feature Extraction (30 Numerical/Lexical Features)
       ├── 2. Heuristic Rule Engine (Punycode, IP Hosts, @-Tricks, Keywords)
       ├── 3. Machine Learning Inference (Serialized Best Model Artifact)
       ├── 4. Transparent Risk Scoring (0–100 Bounded Score, Risk Level & Category)
       └── 5. Explainability Engine (Human-Friendly Plain Language Warnings)
       │
       ▼
Structured Prediction Response (JSON)
```

---

## 2. Directory Structure

```
ml-service/
├── app/
│   ├── __init__.py
│   ├── main.py                     # FastAPI application entrypoint & lifespan hooks
│   ├── config.py                   # Pydantic BaseSettings environment loader
│   ├── schemas/
│   │   ├── __init__.py
│   │   └── prediction.py           # Pydantic request/response validation schemas
│   ├── api/
│   │   ├── __init__.py
│   │   └── routes.py               # REST API endpoints (/health, /predict, /features, /model/info)
│   ├── services/
│   │   ├── __init__.py
│   │   ├── prediction_service.py   # Full pipeline orchestration
│   │   ├── feature_service.py      # Feature extraction inspection service
│   │   └── risk_service.py         # Multi-factor risk calculation & category engine
│   ├── ml/
│   │   ├── __init__.py
│   │   ├── model_loader.py         # Singleton model deserialization loader
│   │   ├── predictor.py            # DataFrame conversion & calibrated inference
│   │   └── explainer.py            # User-facing non-technical explanation generator
│   ├── rules/
│   │   ├── __init__.py
│   │   └── phishing_rules.py       # Heuristic rule-based indicators
│   └── utils/
│       ├── __init__.py
│       └── url_utils.py            # Safe URL parsing, normalization, IP/Punycode/Entropy
├── training/
│   ├── train.py                    # End-to-end training pipeline orchestrator
│   ├── evaluate.py                 # Evaluation metrics & feature importance generator
│   ├── compare_models.py           # 4-model trainer (LR, RF, XGBoost, SVM) & model selector
│   └── save_model.py               # Model & metadata serialization
├── preprocessing/
│   ├── clean_dataset.py            # Dataset cleaning, deduplication & report summary
│   └── preprocess.py               # Feature matrix generation & stratified split
├── feature_engineering/
│   ├── __init__.py
│   └── url_features.py             # Canonical 30-feature extraction function
├── data/
│   ├── raw/                        # Raw CSV dataset files
│   ├── processed/                  # Preprocessed matrices
│   └── README.md
├── models/
│   ├── best_model.joblib           # Active serialized model
│   └── model_metadata.json         # Training metrics and metadata
├── evaluation/                     # Evaluation reports
├── tests/
│   ├── conftest.py
│   ├── test_features.py            # Feature extraction unit tests
│   ├── test_rules.py               # Heuristic rules unit tests
│   ├── test_prediction.py          # ML prediction and risk scoring tests
│   └── test_api.py                 # FastAPI route integration tests
├── requirements.txt
├── .env.example
├── Dockerfile
├── pytest.ini
└── README.md
```

---

## 3. Feature Engineering (30 Canonical Features)

| Category | Features |
|---|---|
| **Lexical Lengths** | `url_length`, `hostname_length`, `path_length`, `query_length`, `fragment_length` |
| **Character Counts** | `dot_count`, `hyphen_count`, `underscore_count`, `slash_count`, `question_mark_count`, `equal_count`, `ampersand_count`, `percent_count`, `at_symbol_count`, `digit_count`, `special_character_count` |
| **Structural Complexity** | `subdomain_count`, `path_segment_count`, `query_parameter_count` |
| **Security Indicators** | `has_https`, `has_ip_address`, `has_port`, `has_punycode`, `has_url_shortener`, `has_encoded_characters` |
| **Phishing Keywords** | `suspicious_keyword_count`, `contains_suspicious_keyword` |
| **Complexity & Ratios** | `entropy` (Shannon Entropy), `digit_ratio`, `special_char_ratio` |

---

## 4. Setup & Installation

### Windows PowerShell / CMD
```powershell
cd ml-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

---

## 5. Model Training & Comparison

To train and benchmark all 4 algorithms (Logistic Regression, Random Forest, XGBoost, SVM) and serialize the best model:

```powershell
python training/train.py
```

### Generated Artifacts:
- `models/best_model.joblib`: Selected model pipeline.
- `models/model_metadata.json`: Feature list, metrics, selection timestamp.
- `evaluation/*_eval.json`: Confusion matrix, classification report, and top features.

---

## 6. Running the Microservice

```powershell
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

---

## 7. API Examples

### Health Check
`GET http://localhost:8001/health`
```json
{
  "status": "UP",
  "service": "phishing-ml-service",
  "model_loaded": true,
  "model_version": "1.0.0",
  "model_name": "Logistic Regression"
}
```

### Predict Phishing URL
`POST http://localhost:8001/predict`
```json
{
  "url": "http://secure-login-hdfc-netbanking.tk/verify"
}
```
**Response:**
```json
{
  "success": true,
  "url": "http://secure-login-hdfc-netbanking.tk/verify",
  "prediction": "PHISHING",
  "risk_level": "HIGH_RISK",
  "risk_score": 99,
  "ml_probability": 0.9906,
  "confidence": 0.9812,
  "threat_category": "BANKING_PHISHING",
  "indicators": [
    {
      "code": "SUSPICIOUS_KEYWORD",
      "severity": "MEDIUM",
      "message": "The URL contains sensitive security or verification keywords (login, verify, secure).",
      "weight": 20.0
    },
    {
      "code": "EXCESSIVE_HYPHENS",
      "severity": "MEDIUM",
      "message": "The domain contains an unusually high number of hyphens (3).",
      "weight": 15.0
    },
    {
      "code": "UNENCRYPTED_AUTH",
      "severity": "HIGH",
      "message": "The URL asks for credentials or verification over unencrypted HTTP protocol.",
      "weight": 25.0
    }
  ],
  "explanation": [
    "The web address contains sensitive action keywords (such as login, verify, or banking) that may be used to lure users into submitting credentials.",
    "The domain contains an unusually large number of hyphens, often used to simulate legitimate brand names.",
    "The page does not use secure HTTPS encryption while requesting sensitive or login actions."
  ],
  "model": {
    "name": "Logistic Regression",
    "version": "1.0.0",
    "selection_metric": "f1"
  },
  "execution_time_ms": 3.62
}
```

---

## 8. Running Automated Tests

Run the full pytest suite (25 tests):
```powershell
python -m pytest tests/ -v
```

---

## 9. Docker Deployment

```bash
docker build -t phishshield-ml-service:1.0.0 .
docker run -p 8001:8001 phishshield-ml-service:1.0.0
```
