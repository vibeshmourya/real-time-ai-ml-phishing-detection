import io
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.ml.model_loader import get_model_container

client = TestClient(app)

@pytest.fixture(autouse=True)
def setup_model():
    container = get_model_container()
    if not container.is_loaded:
        container.load_model()

def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] in ("prahar-ml-service", "phishing-ml-service")
    assert data["model_loaded"] is True
    assert "model_version" in data

def test_predict_safe_url():
    response = client.post("/predict", json={"url": "https://www.github.com/torvalds/linux"})
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert "risk_score" in data
    assert "risk_level" in data
    assert "ml_probability" in data
    assert "confidence" in data
    assert "explanation" in data
    assert "model" in data

def test_predict_phishing_url():
    response = client.post("/predict", json={"url": "http://secure-login-hdfc-netbanking.tk/verify"})
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["prediction"] == "PHISHING"
    assert data["risk_level"] == "HIGH_RISK"
    assert data["risk_score"] >= 70
    assert len(data["indicators"]) > 0

def test_predict_invalid_protocol():
    response = client.post("/predict", json={"url": "ftp://fileserver.net/data"})
    assert response.status_code == 422
    data = response.json()
    assert data["success"] is False
    assert data["error"]["code"] == "VALIDATION_ERROR"

def test_predict_empty_url():
    response = client.post("/predict", json={"url": ""})
    assert response.status_code == 422
    data = response.json()
    assert data["success"] is False

def test_model_info_endpoint():
    response = client.get("/model/info")
    assert response.status_code == 200
    data = response.json()
    assert data["model_loaded"] is True
    assert data["feature_count"] == 30

def test_model_evaluations_endpoint():
    response = client.get("/model/evaluations")
    assert response.status_code == 200
    data = response.json()
    assert "evaluations" in data
    assert len(data["evaluations"]) > 0

def test_features_endpoint():
    response = client.post("/features", json={"url": "https://example.com/login?id=123"})
    assert response.status_code == 200
    data = response.json()
    assert data["feature_count"] == 30
    assert "has_https" in data["features"]
    assert data["features"]["has_https"] == 1

def test_ocr_endpoint_with_text_hint():
    fake_img = io.BytesIO(b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15c4\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82")
    response = client.post(
        "/ocr",
        files={"file": ("test.png", fake_img, "image/png")},
        data={"text_hint": "Urgent: Your SBI bank account KYC has expired. Visit http://sbi-kyc.xyz"}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert "extracted_urls" in data
    assert len(data["extracted_urls"]) > 0
