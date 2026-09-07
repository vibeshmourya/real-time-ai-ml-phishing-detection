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
    assert data["service"] == "phishing-ml-service"
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

def test_features_endpoint():
    response = client.post("/features", json={"url": "https://example.com/login?id=123"})
    assert response.status_code == 200
    data = response.json()
    assert data["feature_count"] == 30
    assert "has_https" in data["features"]
    assert data["features"]["has_https"] == 1
