import pytest
from app.ml.model_loader import get_model_container
from app.services.prediction_service import PredictionService
from app.services.risk_service import RiskService

@pytest.fixture(autouse=True)
def ensure_model_loaded():
    container = get_model_container()
    if not container.is_loaded:
        container.load_model()

def test_model_loaded():
    container = get_model_container()
    assert container.is_loaded is True
    assert container.model is not None
    assert len(container.feature_names) == 30

def test_prediction_legitimate_url():
    service = PredictionService()
    res = service.process_url("https://www.google.com/search?q=cybersecurity")
    
    assert res["success"] is True
    assert res["prediction"] in ("SAFE", "PHISHING")
    assert "risk_score" in res
    assert "risk_level" in res
    assert "explanation" in res
    assert len(res["explanation"]) > 0
    assert res["execution_time_ms"] > 0

def test_prediction_phishing_url():
    service = PredictionService()
    res = service.process_url("http://secure-login-hdfc-netbanking.tk/verify")
    
    assert res["success"] is True
    assert res["prediction"] == "PHISHING"
    assert res["risk_level"] == "HIGH_RISK"
    assert res["risk_score"] >= 70
    assert res["threat_category"] == "BANKING_PHISHING"
    assert len(res["indicators"]) > 0

def test_risk_service_scoring():
    rule_indicators = [
        {"code": "IP_BASED_HOST", "severity": "HIGH", "weight": 30.0},
        {"code": "SUSPICIOUS_KEYWORD", "severity": "MEDIUM", "weight": 20.0}
    ]
    features = {"has_ip_address": 1}
    
    res = RiskService.calculate_risk("http://192.168.1.1/login", 0.90, rule_indicators, features)
    assert res["risk_score"] >= 70
    assert res["risk_level"] == "HIGH_RISK"
    assert res["threat_category"] in ("CREDENTIAL_THEFT", "PHISHING")
