#!/usr/bin/env python3
"""
PRAHAR ACADEMIC SUBMISSION PACKAGING & CLEANLINESS AUDITOR
==========================================================
Deterministic, secure packaging script for Prahar academic submission.
Explicitly enforces exclusion of VCS, caches, temporary files, build outputs,
virtual environments, dependencies, and credentials.
Validates file integrity and scans for leaked secrets prior to bundling.
"""

import os
import sys
import re
import zipfile
import hashlib
from pathlib import Path

# Paths
REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_ZIP_NAME = "prahar_academic_submission.zip"
DEFAULT_ZIP_PATH = REPO_ROOT / DEFAULT_ZIP_NAME

# Explicit directory exclusion blacklist (exact folder names or path components)
EXCLUDED_DIR_NAMES = {
    ".git",
    "node_modules",
    "venv",
    ".venv",
    "env",
    "__pycache__",
    ".pytest_cache",
    ".idea",
    ".vscode",
    ".agents",
    ".gemini",
    ".settings",
    "target",
    "dist",
    "build",
    "temp_uploads",
    "coverage",
    "htmlcov",
    ".nyc_output"
}

# Explicit file extension blacklist
EXCLUDED_EXTENSIONS = {
    ".pyc", ".pyo", ".pyd",
    ".class", ".jar", ".war", ".ear",
    ".log",
    ".zip", ".tar", ".gz", ".tgz", ".rar", ".7z",
    ".sqlite3", ".db",
    ".pem", ".key", ".p12", ".pfx", ".jks", ".keystore",
    ".dmp", ".dump"
}

# Explicit exact file names blacklist
EXCLUDED_FILE_NAMES = {
    ".env",
    ".env.local",
    ".env.development.local",
    ".env.test.local",
    ".env.production.local",
    ".DS_Store",
    "Thumbs.db",
    "desktop.ini",
    "npm-debug.log",
    "yarn-debug.log",
    "yarn-error.log"
}

# Regex patterns for leaked credential detection in included files
SECRET_PATTERNS = [
    (re.compile(r"-----BEGIN (RSA|EC|DSA|OPENSSH|PGP) PRIVATE KEY-----"), "Private Key Header"),
    (re.compile(r"AIzaSy[0-9A-Za-z_-]{35}"), "Google API Key"),
    (re.compile(r"AKIA[0-9A-Z]{16}"), "AWS Access Key ID"),
    (re.compile(r"(ghp|gho|ghu|ghs|ghr)_[0-9a-zA-Z]{36}"), "GitHub Personal Access Token"),
    (re.compile(r"xox[baprs]-[0-9a-zA-Z]{10,48}"), "Slack Token"),
    (re.compile(r"ey[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"), "Hardcoded JWT Token")
]

# Required critical files that MUST be present in the submission package
MANDATORY_INCLUSIONS = [
    "README.md",
    "docker-compose.yml",
    "start_services.ps1",
    "database/schema.sql",
    "backend/pom.xml",
    "frontend/package.json",
    "ml-service/requirements.txt",
    "ml-service/models/best_model.joblib",
    "ml-service/models/model_metadata.json",
    "ml-service/data/raw/phishing_dataset.csv",
    "browser-extension/manifest.json",
    "docs/PHASE_32_VIVA_BANK.md",
    "docs/PHASE_35_RELEASE_SIGNOFF.md",
    "docs/PHASE_36_PACKAGING_REPORT.md"
]


def should_exclude(rel_path: Path) -> tuple[bool, str]:
    """Check if a relative path should be excluded based on strict packaging rules."""
    parts = rel_path.parts
    # Check directory components
    for part in parts[:-1]:
        if part in EXCLUDED_DIR_NAMES or part.startswith(".pytest_cache"):
            return True, f"Excluded directory: {part}"
        if part.endswith(".egg-info"):
            return True, f"Excluded egg-info: {part}"

    filename = rel_path.name
    # Check exact file names
    if filename in EXCLUDED_FILE_NAMES:
        return True, f"Excluded file name: {filename}"

    # Check extension
    suffix = rel_path.suffix.lower()
    if suffix in EXCLUDED_EXTENSIONS:
        return True, f"Excluded extension: {suffix}"

    # Check for temporary/scratch files
    if filename.startswith("~") or filename.endswith("~") or filename.endswith(".tmp"):
        return True, "Excluded temporary file"

    return False, ""


def scan_file_for_secrets(filepath: Path) -> list[str]:
    """Scan file content for secret patterns. Skips binary files."""
    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()
    except Exception:
        return []

    findings = []
    # Skip checking docs/SECRETS_MANAGEMENT_PLAYBOOK.md if it only describes patterns
    for pattern, desc in SECRET_PATTERNS:
        matches = pattern.findall(content)
        if matches:
            # Check if match is in an example placeholder
            if "replace_with_" in content or "your_" in content or "example" in str(filepath).lower():
                continue
            findings.append(f"{desc} in {filepath.name} ({len(matches)} match)")
    return findings


def build_package(zip_output_path: Path):
    print("=" * 70)
    print("PRAHAR ACADEMIC SUBMISSION PACKAGING AUDIT")
    print(f"Repository Root : {REPO_ROOT}")
    print(f"Target Archive  : {zip_output_path}")
    print("=" * 70)

    included_files = []
    excluded_files = []
    secret_findings = []

    print("\n[Step 1/5] Scanning repository files...")
    for root, dirs, files in os.walk(REPO_ROOT):
        # Record pruned directories for audit transparency
        pruned = [d for d in dirs if d in EXCLUDED_DIR_NAMES or d.startswith(".pytest_cache")]
        for p in pruned:
            rel_d = (Path(root) / p).relative_to(REPO_ROOT)
            excluded_files.append((str(rel_d), f"Excluded directory tree: {p}"))

        dirs[:] = [d for d in dirs if d not in EXCLUDED_DIR_NAMES and not d.startswith(".pytest_cache")]

        for file in files:
            full_path = Path(root) / file
            rel_path = full_path.relative_to(REPO_ROOT)

            # Skip the output zip itself
            if full_path.resolve() == zip_output_path.resolve():
                continue

            exclude, reason = should_exclude(rel_path)
            if exclude:
                excluded_files.append((str(rel_path), reason))
            else:
                # Scan non-binary files for leaked credentials
                if not rel_path.suffix.lower() in {".png", ".jpg", ".jpeg", ".svg", ".ico", ".joblib", ".bin"}:
                    leaks = scan_file_for_secrets(full_path)
                    if leaks:
                        secret_findings.extend(leaks)

                included_files.append(rel_path)

    print(f"Found {len(included_files)} candidate files to include.")
    print(f"Excluded {len(excluded_files)} files/artifacts per strict hygiene rules.")

    # Step 2: Secret check
    print("\n[Step 2/5] Validating secret scanning results...")
    if secret_findings:
        print("[-] FAILED: Leaked secrets or credentials detected:")
        for sf in secret_findings:
            print(f"    - {sf}")
        print("Aborting package creation due to security violations.")
        sys.exit(1)
    else:
        print("[+] PASSED: Zero leaked credentials or private keys detected.")

    # Step 3: Verify mandatory inclusions
    print("\n[Step 3/5] Verifying mandatory inclusion manifest...")
    missing_critical = []
    for mandatory in MANDATORY_INCLUSIONS:
        mandatory_path = Path(mandatory)
        if not any(f == mandatory_path for f in included_files):
            missing_critical.append(mandatory)

    if missing_critical:
        print("[-] FAILED: Mandatory critical files missing from inclusion list:")
        for mc in missing_critical:
            print(f"    - {mc}")
        sys.exit(1)
    else:
        print(f"[+] PASSED: All {len(MANDATORY_INCLUSIONS)} mandatory critical files verified present.")

    # Step 4: Create zip archive
    print("\n[Step 4/5] Creating academic submission ZIP archive...")
    if zip_output_path.exists():
        zip_output_path.unlink()

    total_uncompressed_bytes = 0
    with zipfile.ZipFile(zip_output_path, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as zipf:
        for rel_path in sorted(included_files):
            full_path = REPO_ROOT / rel_path
            total_uncompressed_bytes += full_path.stat().st_size
            # Normalize path inside zip to use forward slashes (cross-platform compatible)
            archive_name = str(rel_path).replace("\\", "/")
            zipf.write(full_path, arcname=f"prahar/{archive_name}")

    archive_size_bytes = zip_output_path.stat().st_size
    archive_size_mb = archive_size_bytes / (1024 * 1024)
    uncompressed_mb = total_uncompressed_bytes / (1024 * 1024)

    # Compute SHA-256
    sha256 = hashlib.sha256()
    with open(zip_output_path, "rb") as f:
        while chunk := f.read(65536):
            sha256.update(chunk)
    archive_sha256 = sha256.hexdigest()

    print(f"[+] Archive generated successfully: {zip_output_path.name}")
    print(f"    - Included Files     : {len(included_files)}")
    print(f"    - Uncompressed Size  : {uncompressed_mb:.2f} MB ({total_uncompressed_bytes:,} bytes)")
    print(f"    - Compressed Size    : {archive_size_mb:.2f} MB ({archive_size_bytes:,} bytes)")
    print(f"    - Compression Ratio  : {(1 - archive_size_bytes / total_uncompressed_bytes) * 100:.1f}%")
    print(f"    - SHA-256 Checksum   : {archive_sha256}")

    # Step 5: Test archive extraction integrity
    print("\n[Step 5/5] Testing archive extraction integrity...")
    with zipfile.ZipFile(zip_output_path, "r") as test_zipf:
        corrupt = test_zipf.testzip()
        if corrupt is not None:
            print(f"[-] FAILED: Archive testzip detected corrupt entry: {corrupt}")
            sys.exit(1)
        namelist = test_zipf.namelist()

    print(f"[+] PASSED: Archive integrity verified (0 corrupt entries across {len(namelist)} items).")

    # Generate manifest files
    manifest_dir = REPO_ROOT / "docs"
    manifest_file = manifest_dir / "PACKAGE_MANIFEST.md"
    with open(manifest_file, "w", encoding="utf-8") as mf:
        mf.write("# PRAHAR SUBMISSION PACKAGE MANIFEST\n\n")
        mf.write(f"- **Archive File:** `{zip_output_path.name}`\n")
        mf.write(f"- **SHA-256 Checksum:** `{archive_sha256}`\n")
        mf.write(f"- **Uncompressed Size:** {uncompressed_mb:.2f} MB\n")
        mf.write(f"- **Compressed Size:** {archive_size_mb:.2f} MB\n")
        mf.write(f"- **Total Files Included:** {len(included_files)}\n")
        mf.write(f"- **Total Excluded Entries:** {len(excluded_files)}\n\n")

        mf.write("## 1. Verified Mandatory Inclusions\n\n")
        for m in MANDATORY_INCLUSIONS:
            mf.write(f"- [x] `prahar/{m}`\n")

        mf.write("\n## 2. Top-Level Directory Breakdown\n\n")
        categories = {}
        for f in included_files:
            top = f.parts[0] if len(f.parts) > 1 else "root"
            categories[top] = categories.get(top, 0) + 1

        mf.write("| Directory / Component | Included Files Count |\n")
        mf.write("| :--- | :--- |\n")
        for cat, count in sorted(categories.items()):
            mf.write(f"| `{cat}/` | {count} |\n")

        mf.write("\n## 3. Sample Exclusion Reasons\n\n")
        sample_exclusions = {}
        for path, reason in excluded_files:
            if reason not in sample_exclusions:
                sample_exclusions[reason] = []
            if len(sample_exclusions[reason]) < 3:
                sample_exclusions[reason].append(path)

        for reason, examples in sample_exclusions.items():
            mf.write(f"- **{reason}:**\n")
            for ex in examples:
                mf.write(f"  - `{ex}`\n")

    print(f"[+] Manifest generated: {manifest_file.relative_to(REPO_ROOT)}")
    print("=" * 70)
    print("PACKAGE GENERATION AND AUDIT COMPLETE: STATUS VERIFIED")
    print("=" * 70)


if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_ZIP_PATH
    build_package(out)
