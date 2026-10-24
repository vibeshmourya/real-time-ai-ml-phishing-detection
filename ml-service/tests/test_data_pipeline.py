import os
import tempfile
from pathlib import Path
import numpy as np
import pandas as pd
import pytest
from sklearn.linear_model import LogisticRegression

from data_pipeline.ingest_feeds import PhishingFeedIngester, normalize_url, CURATED_HARD_NEGATIVES
from data_pipeline.governance import ModelGovernanceAuditor, DEFAULT_PROMOTION_THRESHOLDS

def test_normalize_url():
    # Fragment stripping
    assert normalize_url("https://example.com/login#section1") == "https://example.com/login"
    # Port normalization
    assert normalize_url("http://example.com:80/path") == "http://example.com/path"
    assert normalize_url("https://example.com:443/path") == "https://example.com/path"
    # Scheme injection
    assert normalize_url("cybercrime.gov.in") == "http://cybercrime.gov.in/"
    # Trailing whitespace
    assert normalize_url("  https://bank.com/portal/  ") == "https://bank.com/portal/"
    # Invalid inputs
    assert normalize_url("") is None
    assert normalize_url("abc") is None
    assert normalize_url(None) is None

def test_feed_ingester_deduplication():
    with tempfile.TemporaryDirectory() as tmp_dir:
        ingester = PhishingFeedIngester(output_dir=tmp_dir)

        phish_urls = [
            "http://phish-login.top/verify",
            "http://phish-login.top/verify#frag", # Duplicate after norm
            "http://update-kyc-alert.xyz"
        ]
        benign_urls = [
            "https://cybercrime.gov.in/portal",
            "https://cybercrime.gov.in/portal"   # Duplicate
        ]

        df, prov = ingester.build_curated_dataset(
            phishing_sources=[(phish_urls, "test_phish_feed")],
            benign_sources=[(benign_urls, "test_benign_feed")],
            include_hard_negatives=False
        )

        # Expected unique URLs: 2 phish + 1 benign = 3 total
        assert len(df) == 3
        assert prov["deduplicated_samples"] == 3
        assert prov["duplicates_removed"] == 2
        assert prov["phishing_count"] == 2
        assert prov["legitimate_count"] == 1

        # Test export
        out_file = ingester.export_dataset(df, prefix="test_export")
        assert out_file.exists()
        assert out_file.suffix == ".csv"

def test_governance_metrics_calculation():
    with tempfile.TemporaryDirectory() as tmp_dir:
        auditor = ModelGovernanceAuditor(evaluation_dir=tmp_dir)

        # Ground truth: 3 Benign (0), 3 Phishing (1)
        y_true = pd.Series([0, 0, 0, 1, 1, 1])
        # Prediction: 1 False Positive, 0 False Negatives
        # Preds: 0, 0, 1 (FP), 1, 1, 1
        y_pred = np.array([0, 0, 1, 1, 1, 1])

        metrics = auditor.calculate_governance_metrics(y_true, y_pred)

        # FPR = FP / (FP + TN) = 1 / (1 + 2) = 1/3 ~ 0.3333
        assert metrics["false_positive_rate"] == pytest.approx(0.3333, abs=0.01)
        # Recall = TP / (TP + FN) = 3 / (3 + 0) = 1.0
        assert metrics["recall"] == 1.0
        # Precision = TP / (TP + FP) = 3 / (3 + 1) = 0.75
        assert metrics["precision"] == 0.75
        assert metrics["confusion_matrix"]["false_positive"] == 1
        assert metrics["confusion_matrix"]["false_negative"] == 0

def test_governance_candidate_evaluation_gate():
    with tempfile.TemporaryDirectory() as tmp_dir:
        auditor = ModelGovernanceAuditor(evaluation_dir=tmp_dir)

        # Create dummy logistic model
        X_train = pd.DataFrame({
            "f1": [1.0, 2.0, 3.0, 4.0, 5.0, 6.0],
            "f2": [0.1, 0.2, 0.3, 0.8, 0.9, 1.0]
        })
        y_train = pd.Series([0, 0, 0, 1, 1, 1])

        model = LogisticRegression()
        model.fit(X_train, y_train)

        # Test data matching training distributions
        X_test = pd.DataFrame({
            "f1": [1.1, 2.1, 5.1, 5.9],
            "f2": [0.15, 0.25, 0.85, 0.95]
        })
        y_test = pd.Series([0, 0, 1, 1])

        record = auditor.evaluate_candidate(
            candidate_model=model,
            candidate_name="TestLogisticCandidate",
            X_test=X_test,
            y_test=y_test,
            candidate_version="2.1.0-test"
        )

        assert "governance_status" in record
        assert "candidate_metrics" in record
        assert "threshold_evaluations" in record
        assert Path(tmp_dir).glob("governance_audit_*.json") is not None

def test_governance_rejection_of_unauthorized_promotion():
    with tempfile.TemporaryDirectory() as tmp_dir:
        auditor = ModelGovernanceAuditor(evaluation_dir=tmp_dir)

        # Attempt to promote without human signature
        with pytest.raises(ValueError, match="Valid human operator approval signature is required"):
            auditor.promote_candidate_model(
                candidate_model_path="dummy.joblib",
                human_approval_signature="",
                audit_report_path="dummy_report.json"
            )
