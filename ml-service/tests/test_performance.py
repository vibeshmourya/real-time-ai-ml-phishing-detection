import time
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.ml.model_loader import get_model_container
from feature_engineering.url_features import extract_features
from app.rules.phishing_rules import evaluate_phishing_rules

client = TestClient(app)

@pytest.fixture(autouse=True)
def setup_model():
    container = get_model_container()
    if not container.is_loaded:
        container.load_model()

def compute_percentiles(durations_ms: list[float]) -> dict[str, float]:
    sorted_d = sorted(durations_ms)
    n = len(sorted_d)
    return {
        "min": round(sorted_d[0], 2),
        "median": round(sorted_d[int(n * 0.50)], 2),
        "p95": round(sorted_d[int(n * 0.95)], 2),
        "p99": round(sorted_d[int(n * 0.99)], 2),
        "max": round(sorted_d[-1], 2),
    }

def test_feature_extraction_performance():
    test_urls = [
        "https://www.google.com/search?q=cybersecurity",
        "http://secure-login-hdfc-netbanking.tk/verify-account",
        "https://sub.domain.corp.internal.example.org/path/to/resource?auth=token&ref=1",
        "http://192.168.1.1/admin.html",
        "https://paypal.com-verification-service.xyz/account/login.php"
    ]
    
    # Warmup
    for u in test_urls:
        extract_features(u)
        
    durations = []
    for _ in range(50):
        for u in test_urls:
            t0 = time.perf_counter()
            feats = extract_features(u)
            durations.append((time.perf_counter() - t0) * 1000)
            assert len(feats) == 30
            
    stats = compute_percentiles(durations)
    print(f"\n[BENCHMARK] Feature Extraction (250 runs): median={stats['median']}ms, p95={stats['p95']}ms, max={stats['max']}ms")
    assert stats["median"] < 1.0, "Feature extraction median must be sub-millisecond"

def test_rule_evaluation_performance():
    url = "http://login-security-update-sbi-portal.com.attacker.xyz/verify-otp?token=9382"
    
    # Warmup
    for _ in range(10):
        evaluate_phishing_rules(url)
        
    durations = []
    for _ in range(100):
        t0 = time.perf_counter()
        rules = evaluate_phishing_rules(url)
        durations.append((time.perf_counter() - t0) * 1000)
        assert len(rules) > 0
        
    stats = compute_percentiles(durations)
    print(f"\n[BENCHMARK] Rule Evaluation (100 runs): median={stats['median']}ms, p95={stats['p95']}ms")
    assert stats["median"] < 1.0, "Rule evaluation median must be sub-millisecond"

def test_end_to_end_predict_api_latency():
    urls = [
        "https://www.github.com/torvalds/linux",
        "http://secure-login-hdfc-netbanking.tk/verify",
        "https://microsoft.com/en-us/security",
        "http://paypal-account-reactivation-center.xyz/auth"
    ]
    
    # Warmup
    for u in urls:
        client.post("/predict", json={"url": u})
        
    durations = []
    for _ in range(25):
        for u in urls:
            t0 = time.perf_counter()
            resp = client.post("/predict", json={"url": u})
            durations.append((time.perf_counter() - t0) * 1000)
            assert resp.status_code == 200
            assert resp.json()["success"] is True
            
    stats = compute_percentiles(durations)
    total_time_s = sum(durations) / 1000.0
    throughput = len(durations) / total_time_s if total_time_s > 0 else 0
    print(f"\n[BENCHMARK] FastAPI /predict (100 runs): median={stats['median']}ms, p95={stats['p95']}ms, max={stats['max']}ms | Throughput: {throughput:.1f} req/s")
    assert stats["median"] < 20.0, "ML Predict API median should be under 20ms in-process"
