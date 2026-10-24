# PRAHAR — COMPLETE PROJECT UI CONTENT AUDIT
**Document ID:** `docs/CONTENT_CLEANUP_AUDIT.md`  
**Date:** 2026-10-24  
**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection & Prevention System  
**Audit Scope:** Full repository frontend pages, components, layouts, data metrics, and localization resources.

---

## 1. Executive Summary & Audit Objectives

The objective of this comprehensive audit is to systematically review every user-facing page, component, modal, metric, and text block across the Prahar application to identify and eliminate:
- Redundant and repetitive headings, subtitles, and badges.
- Development notes, milestone labels, and debugging artifacts (e.g., `"Phase 20"` badge).
- Fabricated, hardcoded, or misleading statistics (e.g., fallback counters like `'14,829'` and `'4,120'`).
- Verbose disclaimer banners that dilute key warnings.
- Repetitive safety strips and marketing slogans.
- Translation discrepancies or untranslated developer placeholders.

### Non-Negotiable Retention Rules (Step 3 Compliance)
The cleanup strictly preserves:
1. Primary navigation paths, brand identity, and clear page titles.
2. Form controls, buttons, instructions, and input validations.
3. Phishing risk scores (`0-100`), classifications (`SAFE`, `SUSPICIOUS`, `HIGH_RISK`), and explainable forensic detection indicators.
4. Emergency helplines (National Cyber Crime Helpline `1930`, `cybercrime.gov.in`, `ceir.gov.in`).
5. Real-time scanning state indicators (loading, checking, success, error, warning).
6. Multilingual accessibility across English (`en`), Hindi (`hi`), and Kannada (`kn`).

---

## 2. Page-by-Page Audit & Identified Content Issues

### 2.1 Scanner Page (`frontend/src/pages/ScannerPage.jsx`)
- **Identified Issues:**
  1. *Development Artifact:* The Visual & QR Lab tab button contains an obsolete milestone badge: `<span className="... uppercase bg-blue-500/40 text-white">Phase 20</span>`. This is an internal project phase label that clutters the UI.
  2. *Redundant Feature Cards:* The bottom of the page features a 3-card strip ("Privacy Guarded", "Explainable AI", "Multi-Channel Inspection") that duplicates information already conveyed in the page subtitle, the mode switcher, and the scanner privacy note.
- **Proposed Changes:**
  - Remove the `"Phase 20"` badge from the Visual & QR Lab tab.
  - Remove the redundant bottom 3-card strip to streamline user focus entirely on the scanning tools and verdicts.

### 2.2 About Page (`frontend/src/pages/AboutPage.jsx`)
- **Identified Issues:**
  1. *Redundant Pill Badge:* Directly above the primary `<h1>About Prahar</h1>`, an extra `<div className="..."><Shield /> About the System</div>` badge is displayed, repeating the title words.
  2. *Inaccurate/Outdated Architecture Text:* In the technical architecture preformatted block, it references `Java 17/25` and claims `Ensemble Model Inference (XGBoost Classifier)`. In truth, the benchmarked, production-trained artifact in `ml_service/model/` is Calibrated Logistic Regression alongside lexical heuristics.
- **Proposed Changes:**
  - Remove the redundant `"About the System"` badge above `h1`.
  - Update the technical architecture preformatted diagram to accurately state `Java 17 LTS` and `Calibrated ML Classifier (Logistic Regression & Heuristics)`.

### 2.3 Admin Portal / Security Center (`frontend/src/pages/AdminPortalPage.jsx`)
- **Identified Issues:**
  1. *Fabricated Fallback Statistics:*
     - Line 199: `{stats?.totalGlobalScans?.toLocaleString() || '14,829'}`
     - Line 207: `{stats?.totalThreatsBlocked?.toLocaleString() || '4,120'}`
     - Line 209: Hardcoded `"27.8% malicious strike rate"`
     - Line 223: Hardcoded fallback `42 ms`
     These placeholder numbers violate the strict rule against inventing scan history, threat counts, or telemetry data.
- **Proposed Changes:**
  - Replace fabricated fallbacks with dynamic computations:
    - If `stats` is null/loading, display `0` or `'—'` rather than inventing large numbers.
    - If `totalGlobalScans > 0`, calculate `(totalThreatsBlocked / totalGlobalScans * 100).toFixed(1)%`; otherwise display `"No threat telemetry"`.
    - If `avgDetectionTimeMs` is null, display `— ms`.

### 2.4 My Money Page (`frontend/src/pages/MyMoneyPage.jsx`)
- **Identified Issues:**
  1. *Verbose Disclaimer Banner:* The `"Transparent Data Source Notice"` is a wordy paragraph repeating what is already known: `"Prahar strictly uses data you enter, CSV files you upload, or notification texts you paste. Prahar does not connect directly to bank servers or read private SMS without consent."`
- **Proposed Changes:**
  - Streamline to a concise 1-sentence transparency note: `"Data source: User-entered inputs and imported files only. Prahar does not connect to bank servers or read private messages."`

### 2.5 Device Safety Page (`frontend/src/pages/DeviceSafetyPage.jsx`)
- **Identified Issues:**
  1. *Overly Wordy Boundary Notice:* The `"Prahar Safety Policy"` banner has repetitive explanation about what Prahar does not do.
- **Proposed Changes:**
  - Tighten the notice to: `"Step-by-step containment guides with direct links to official portals (CEIR, Find My Device, 1930 Helpline). Prahar does not access private device records."`

### 2.6 Cyber Awareness Hub (`frontend/src/components/CyberAwarenessHub.jsx`)
- **Identified Issues:**
  1. *Redundant Sub-Badge:* The header contains `<span className="...">Cyber Safety Education</span>` stacked immediately on top of `<h2>Prahar Learn</h2>`.
- **Proposed Changes:**
  - Remove the redundant badge so the heading and concise subtitle stand clearly.

### 2.7 Quick Scanner (`frontend/src/components/QuickScanner.jsx`)
- **Identified Issues:**
  1. Duplicate or overly long example labels.
  2. Input labels and buttons checked for concise phrasing.
- **Proposed Changes:**
  - Maintain clean, minimal sample chips (`Fake Bank`, `Urgent KYC SMS`, `Electricity Bill`, `Google Safe`).
  - Keep primary CTA crisp: `"Check for threats"`.

### 2.8 Shared Navigation & Modals
- **Navbar (`frontend/src/components/Navbar.jsx`):** Clean and concise; ensure all nav items have matching translations.
- **Scan Verdict Modal (`frontend/src/components/ScanVerdictModal.jsx`):** Excellent clarity; preserve the 0-100 risk score, explainable forensic tags, and recommended next steps.
- **Emergency Wizard (`frontend/src/components/EmergencyWizard.jsx`):** Clear, actionable step-by-step checklists and direct `1930` dialing.

---

## 3. Multilingual Parity & Localization Review
All UI text modifications must maintain 100% parity across:
- `frontend/src/locales/en.json` (English)
- `frontend/src/locales/hi.json` (Hindi)
- `frontend/src/locales/kn.json` (Kannada)

No obsolete translation keys will be left orphaned, and any updated UI phrases will be mirrored across all three languages without introducing raw English strings in non-English locales.

---

## 4. Verification Checklist & Testing Strategy
1. **Automated Unit Tests:** Execute `npm test` in `frontend` (verifying 20/20 tests passing).
2. **Production Bundle Verification:** Run `npm run build` in `frontend` to guarantee error-free compilation and clean asset emission.
3. **Packaging Validation:** Re-run `python scripts/package_submission.py` to ensure submission integrity.
4. **Post-Cleanup Report:** Produce `docs/CONTENT_CLEANUP_REPORT.md` documenting before/after status.
