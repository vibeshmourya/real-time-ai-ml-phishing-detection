# PRAHAR SUBMISSION PACKAGE MANIFEST

- **Archive File:** `prahar_academic_submission.zip`
- **SHA-256 Checksum:** `d237a284bdcbba094eb6187e6f6221adbb1789eac71a871718deb7f7156014bc`
- **Uncompressed Size:** 2.25 MB
- **Compressed Size:** 0.77 MB
- **Total Files Included:** 403
- **Total Excluded Entries:** 20

## 1. Verified Mandatory Inclusions

- [x] `prahar/README.md`
- [x] `prahar/docker-compose.yml`
- [x] `prahar/start_services.ps1`
- [x] `prahar/database/schema.sql`
- [x] `prahar/backend/pom.xml`
- [x] `prahar/frontend/package.json`
- [x] `prahar/ml-service/requirements.txt`
- [x] `prahar/ml-service/models/best_model.joblib`
- [x] `prahar/ml-service/models/model_metadata.json`
- [x] `prahar/ml-service/data/raw/phishing_dataset.csv`
- [x] `prahar/browser-extension/manifest.json`
- [x] `prahar/docs/PHASE_32_VIVA_BANK.md`
- [x] `prahar/docs/PHASE_35_RELEASE_SIGNOFF.md`
- [x] `prahar/docs/PHASE_36_PACKAGING_REPORT.md`

## 2. Top-Level Directory Breakdown

| Directory / Component | Included Files Count |
| :--- | :--- |
| `backend/` | 207 |
| `browser-extension/` | 11 |
| `database/` | 5 |
| `docs/` | 56 |
| `evaluation/` | 4 |
| `frontend/` | 57 |
| `media/` | 5 |
| `ml-service/` | 50 |
| `root/` | 6 |
| `scripts/` | 2 |

## 3. Sample Exclusion Reasons

- **Excluded directory tree: .git:**
  - `.git`
  - `backend\.git`
- **Excluded directory tree: .pytest_cache:**
  - `.pytest_cache`
  - `backend\.pytest_cache`
  - `ml-service\.pytest_cache`
- **Excluded directory tree: target:**
  - `backend\target`
- **Excluded directory tree: dist:**
  - `frontend\dist`
- **Excluded directory tree: node_modules:**
  - `frontend\node_modules`
- **Excluded directory tree: __pycache__:**
  - `ml-service\app\__pycache__`
  - `ml-service\app\api\__pycache__`
  - `ml-service\app\ml\__pycache__`
