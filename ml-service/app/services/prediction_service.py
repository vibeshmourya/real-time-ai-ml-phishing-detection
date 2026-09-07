import time
import logging
from typing import Any

from feature_engineering.url_features import extract_features
from app.rules.phishing_rules import evaluate_phishing_rules
from app.ml.predictor import ModelPredictor
from app.ml.explainer import generate_user_explanations
from app.services.risk_service import RiskService
from app.ml.model_loader import get_model_container
from app.config import settings

logger = logging.getLogger("phishshield.ml.service")

class PredictionService:
    def __init__(self):
        self.predictor = ModelPredictor()
        self.container = get_model_container()

    def process_url(self, url: str) -> dict[str, Any]:
        """
        Executes end-to-end detection pipeline:
        URL -> Features -> Rules -> ML Inference -> Risk Assessment -> Explanation
        """
        start_time = time.perf_counter()
        
        # 1. Feature Extraction
        features = extract_features(url)
        
        # 2. Rule-Based Indicator Evaluation
        rule_indicators = evaluate_phishing_rules(url)
        
        # 3. ML Model Prediction
        prediction_label, ml_prob, confidence = self.predictor.predict(features)
        
        # 4. Risk Calculation & Threat Categorization
        risk_result = RiskService.calculate_risk(url, ml_prob, rule_indicators, features)
        
        # 5. User-Friendly Explanation Synthesis
        explanations = generate_user_explanations(features, rule_indicators, ml_prob, prediction_label)
        
        execution_time_ms = round((time.perf_counter() - start_time) * 1000, 2)
        
        # Safe logging (avoid logging full URL if LOG_URLS=False)
        log_target = url if settings.LOG_URLS else f"URL (length {len(url)})"
        logger.info(
            "Analyzed %s -> %s (Risk: %s, Score: %d) in %.2fms",
            log_target, prediction_label, risk_result["risk_level"], risk_result["risk_score"], execution_time_ms
        )
        
        model_meta = self.container.metadata or {}
        
        return {
            "success": True,
            "url": url,
            "prediction": prediction_label,
            "risk_level": risk_result["risk_level"],
            "risk_score": risk_result["risk_score"],
            "ml_probability": ml_prob,
            "confidence": confidence,
            "threat_category": risk_result["threat_category"],
            "indicators": rule_indicators,
            "explanation": explanations,
            "model": {
                "name": model_meta.get("model_name", "PhishShield-ML"),
                "version": model_meta.get("version", "1.0.0"),
                "selection_metric": model_meta.get("selection_metric", "f1")
            },
            "execution_time_ms": execution_time_ms
        }
