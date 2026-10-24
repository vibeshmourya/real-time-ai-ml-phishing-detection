import json
import pytest
from pathlib import Path
from training.benchmark_evaluation import run_benchmark, compute_sha256

def test_benchmark_reproducibility_and_artifact_integrity():
    base_dir = Path(__file__).resolve().parent.parent
    model_path = base_dir / "models" / "best_model.joblib"
    initial_hash = compute_sha256(model_path)
    
    # Run the benchmark
    results = run_benchmark()
    
    # Verify model artifact was NOT modified
    after_hash = compute_sha256(model_path)
    assert initial_hash == after_hash, "Benchmark execution must not overwrite or mutate production model artifacts"
    
    # Verify results structure
    assert "baseline_comparison" in results
    assert "Majority Class Baseline" in results["baseline_comparison"]
    assert "Simple Rule-Based Baseline" in results["baseline_comparison"]
    assert "Current ML Model (Logistic Regression)" in results["baseline_comparison"]
    assert "Current Combined Detector (ML + Rules)" in results["baseline_comparison"]
    
    # Verify metric bounds
    for name, metrics in results["baseline_comparison"].items():
        assert 0.0 <= metrics["accuracy"] <= 1.0
        assert 0.0 <= metrics["precision"] <= 1.0
        assert 0.0 <= metrics["recall"] <= 1.0
        assert 0.0 <= metrics["f1"] <= 1.0
        assert 0.0 <= metrics["false_positive_rate"] <= 1.0
        assert 0.0 <= metrics["false_negative_rate"] <= 1.0
        assert len(metrics["accuracy_95ci"]) == 2
        assert metrics["accuracy_95ci"][0] <= metrics["accuracy_95ci"][1]
        
    # Verify dataset protocol artifact finding is recorded
    confound = results.get("dataset_confounding_analysis", {}).get("protocol_artifact", {})
    assert confound.get("benign_https_count") == 107
    assert confound.get("phishing_http_count") == 68
