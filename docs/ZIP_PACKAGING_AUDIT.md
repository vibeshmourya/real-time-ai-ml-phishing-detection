# ZIP ARCHIVE PACKAGING & SUBMISSION AUDIT

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Audit Scope:** Cleanroom ZIP Archive Creation, Sanitization, Inclusion/Exclusion Rules, and Archive Validation  
**Target Milestone:** MCA Final Year Academic Submission & Evaluator Offline Package  
**Date of Audit:** October 24, 2026  
**Auditor:** Antigravity System Auditor  

---

## 1. Executive Summary

A reproducible, standalone packaging process was designed and verified to generate the official academic submission ZIP archive (`prahar_academic_submission.zip`). The packaging script operates independently of `.gitignore`, enforcing explicit whitelist and blacklist filters to ensure zero development residue, zero credentials, and 100% reproducible execution for examiners.

### Overall Packaging Readiness Verdict: **VERIFIED — COMPLETE & SANITIZED**

- **Archive File:** `prahar_academic_submission.zip`
- **Archive Size (Compressed):** **0.75 MB** (791,155 bytes)
- **Uncompressed Payload:** **2.20 MB** (2,311,026 bytes)
- **Total Files Bundled:** **399 files**
- **Archive Integrity:** **100% Valid** (`zipfile.testzip()` returned 0 corrupt entries)
- **SHA-256 Checksum:** `17b08a4f03cdd78accaef1e11eada6d87540d570cfc8f7d01d05d21f8e983490`

---

## 2. Package Architecture: GitHub vs. Academic Submission ZIP

Two distinct distribution targets are supported:

| Dimension | Package 1: GitHub Repository | Package 2: Academic Submission ZIP |
| :--- | :--- | :--- |
| **Primary Audience** | Open-source reviewers & version control tracking | Academic evaluators, university portal, & viva defense |
| **VCS Metadata (`.git/`)** | Included (git history, branches, tags) | **Strictly Excluded** (reduces size and removes git clutter) |
| **Vendor Dependencies** | Excluded via `.gitignore` (`node_modules/`, `target/`) | **Strictly Excluded** (reproducibly built via `npm` and `mvn`) |
| **Model Weights** | Tracked via Git LFS / whitelisted `best_model.joblib` | **Included** (`ml-service/models/best_model.joblib`, 3.0 KB) |
| **Research Dataset** | Tracked in Git (`data/raw/phishing_dataset.csv`) | **Included** (`ml-service/data/raw/phishing_dataset.csv`, 7.97 KB) |
| **Documentation** | GitHub Markdown viewable in repo | **Complete Docs Suite** (`docs/`, `PACKAGE_MANIFEST.md`, `README.md`) |
| **Secrets & `.env`** | Strictly Excluded (`.env.example` templates only) | Strictly Excluded (`.env.example` templates only) |

---

## 3. Explicit Exclusion & Inclusion Rules

The packaging script (`scripts/package_submission.py`) enforces strict procedural exclusions:

### 3.1 Explicit Exclusions (Blacklist)
- **VCS & Metadata:** `.git/`, `.idea/`, `.vscode/`, `.agents/`, `.gemini/`, `.settings/`, `*.iml`, `.project`, `.classpath`
- **Dependencies & Environments:** `node_modules/`, `venv/`, `.venv/`, `env/`
- **Build Artifacts & Bytecode:** `backend/target/`, `frontend/dist/`, `frontend/build/`, `*.class`, `*.jar`, `*.war`
- **Caches & Compilations:** `__pycache__/`, `*.pyc`, `*.pyo`, `.pytest_cache/`
- **Secrets & Keys:** `.env`, `.env.local`, `*.pem`, `*.key`, `*.p12`, `*.pfx`, `*.jks`, `*.keystore`
- **Local Databases & Locks:** `*.sqlite3`, `*.db`, `database/data/`
- **OS Clutter:** `.DS_Store`, `Thumbs.db`, `desktop.ini`, `*.dmp`, `*.dump`
- **Volatile Outputs:** `*.log`, `temp_uploads/`, `coverage/`, `htmlcov/`
- **Recursive Archives:** `*.zip`, `*.tar`, `*.tar.gz`, `prahar_academic_submission/`

### 3.2 Explicit Inclusions (Preserved Assets)
- **Complete Source Code:**
  - `backend/src/main/java/**` (Controllers, Services, DTOs, Security, NLP, Threat Intel)
  - `frontend/src/**` (React components, pages, context providers, API clients)
  - `ml-service/app/**` (FastAPI routes, ML predictors, feature extraction, OCR)
  - `ml-service/data_pipeline/**` & `ml-service/training/**`
  - `browser-extension/**` (Manifest V3 background, content, popup scripts, icons)
- **Complete Automated Test Suites (186 Tests):**
  - `backend/src/test/java/**` (130 JUnit 5 Spring Boot tests)
  - `ml-service/tests/**` (36 Pytest unit, feature, rule, performance, and benchmark tests)
  - `frontend/src/tests/**` (10 Vitest React tests)
  - `browser-extension/test_extension.js` (10 Chrome Extension contract tests)
- **Manifests & Configs:**
  - `backend/pom.xml`
  - `frontend/package.json` & `frontend/package-lock.json`
  - `ml-service/requirements.txt`
  - `browser-extension/manifest.json`
  - `backend/src/main/resources/application*.yml`
- **Relational Database Baseline:**
  - `database/schema.sql`, `database/sample_data.sql`, `database/migration_v2.sql`, `database/migration_v3.sql`, `database/production_hardening.sql`
- **Machine Learning Artifacts:**
  - `ml-service/models/best_model.joblib` (3.0 KB)
  - `ml-service/models/model_metadata.json`
  - `ml-service/data/raw/phishing_dataset.csv` (7.97 KB)
  - `evaluation/*.json` & `ml-service/evaluation/*.json`
- **Safe Test Fixtures:**
  - `media/demo_fixtures/*.png` (Benign portal, benign QR, phishing QR, phishing screenshot)
- **Orchestration & Startup:**
  - `start_services.ps1`, `docker-compose.yml`, `docker-compose.prod.yml`, `frontend/Dockerfile`, `ml-service/Dockerfile`
- **Consolidated Documentation:**
  - `README.md`, `docs/*.md` (All viva questions, defense notes, presentation scripts, architecture diagrams)

---

## 4. Archive Verification & Integrity Checks

### 4.1 Check 1: Mandatory Critical Files Presence
All 14 mission-critical files required for compilation and evaluation were verified present in the archive:

| Mandatory Path in Archive | Verification Status |
| :--- | :---: |
| `prahar/README.md` | `VERIFIED` |
| `prahar/docker-compose.yml` | `VERIFIED` |
| `prahar/start_services.ps1` | `VERIFIED` |
| `prahar/database/schema.sql` | `VERIFIED` |
| `prahar/backend/pom.xml` | `VERIFIED` |
| `prahar/frontend/package.json` | `VERIFIED` |
| `prahar/ml-service/requirements.txt` | `VERIFIED` |
| `prahar/ml-service/models/best_model.joblib` | `VERIFIED` |
| `prahar/ml-service/models/model_metadata.json` | `VERIFIED` |
| `prahar/ml-service/data/raw/phishing_dataset.csv` | `VERIFIED` |
| `prahar/browser-extension/manifest.json` | `VERIFIED` |
| `prahar/docs/PHASE_32_VIVA_BANK.md` | `VERIFIED` |
| `prahar/docs/PHASE_35_RELEASE_SIGNOFF.md` | `VERIFIED` |
| `prahar/docs/PHASE_36_PACKAGING_REPORT.md` | `VERIFIED` |

### 4.2 Check 2: Zip Deflation & Extraction Integrity
```python
import zipfile
z = zipfile.ZipFile('prahar_academic_submission.zip')
corrupt = z.testzip()
assert corrupt is None
```
**Result:** `VERIFIED`. Zero corrupt entries across 399 items.

### 4.3 Check 3: Pre-Bundling Secret Scan
All 399 candidate files were parsed with regex scanners detecting private keys, Google API keys, AWS credentials, Slack tokens, and JWT tokens before insertion into the ZIP.  
**Result:** `VERIFIED`. Zero leaked credentials or secrets detected.

---

## 5. Summary Checklist of Packaging Verification

| Check Description | Taxonomy Status | Evidence / Result |
| :--- | :---: | :--- |
| Standalone packaging script | `VERIFIED` | `scripts/package_submission.py` & `.ps1` |
| Explicit exclusion of `.git/` | `VERIFIED` | `.git/` directory tree excluded |
| Exclusion of `node_modules/` | `VERIFIED` | `frontend/node_modules/` pruned |
| Exclusion of Python venv | `VERIFIED` | `venv/` and `.venv/` pruned |
| Exclusion of compiled `.class` & `.jar` | `VERIFIED` | `backend/target/` and `.class` pruned |
| Exclusion of `.pyc` and `__pycache__` | `VERIFIED` | All bytecode and pytest caches pruned |
| Exclusion of `.env` and real keys | `VERIFIED` | Zero `.env` files present or bundled |
| Mandatory files verified present | `VERIFIED` | All 14 critical files confirmed |
| Pre-trained model weights included | `VERIFIED` | `best_model.joblib` (3.0 KB) included |
| Research dataset included | `VERIFIED` | `phishing_dataset.csv` (7.97 KB) included |
| Test fixtures included | `VERIFIED` | 4 safe PNG fixtures in `media/demo_fixtures/` |
| Zip integrity validation | `VERIFIED` | `testzip()` passed with 0 corrupt entries |
| Uncompressed payload size | `VERIFIED` | 2.20 MB total payload |
| Compressed archive size | `VERIFIED` | 0.75 MB (791 KB) total archive |
| Manifest documentation | `VERIFIED` | Generated `docs/PACKAGE_MANIFEST.md` |

---

## 6. Examiner Quick-Start from Unzipped Package

When an evaluator or examiner unzips `prahar_academic_submission.zip`:

```powershell
# 1. Unzip the package
Expand-Archive prahar_academic_submission.zip -DestinationPath .
cd prahar

# 2. Automated One-Command Startup
.\start_services.ps1

# 3. Access Services
# Frontend Web App : http://localhost:5173
# Spring Boot API  : http://localhost:8080/swagger-ui/index.html
# Python ML Service: http://localhost:8000/docs
```
