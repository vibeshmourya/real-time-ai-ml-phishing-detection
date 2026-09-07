import logging
from typing import Any, Tuple
import numpy as np
import pandas as pd

from app.ml.model_loader import get_model_container
from feature_engineering.url_features import get_feature_names

logger = logging.getLogger("phishshield.ml.predictor")

class ModelPredictor:
    def __init__(self):
        self.container = get_model_container()

    def predict(self, feature_dict: dict[str, Any]) -> Tuple[str, float, float]:
        """
        Runs ML model inference on the extracted feature dictionary.
        Returns:
            (prediction_label, phishing_probability, confidence_score)
        """
        if not self.container.is_loaded or self.container.model is None:
            raise RuntimeError(f"ML Model is not loaded. {self.container.load_error or 'Please train or place model artifact.'}")
        
        feature_names = self.container.feature_names or get_feature_names()
        
        # Build 1-row DataFrame aligned with training feature names
        ordered_values = {k: [feature_dict.get(k, 0)] for k in feature_names}
        df_input = pd.DataFrame(ordered_values)
        
        model = self.container.model
        
        # 1. Probability Calculation
        if hasattr(model, "predict_proba"):
            probs = model.predict_proba(df_input)[0]
            # Assumes class 1 is phishing, class 0 is legitimate
            phishing_prob = float(probs[1]) if len(probs) > 1 else float(probs[0])
        elif hasattr(model, "decision_function"):
            decision = float(model.decision_function(df_input)[0])
            # Sigmoid scaling
            phishing_prob = 1.0 / (1.0 + np.exp(-decision))
        else:
            raw_pred = model.predict(df_input)[0]
            phishing_prob = 1.0 if raw_pred == 1 else 0.0
            
        phishing_prob = round(float(np.clip(phishing_prob, 0.0, 1.0)), 4)
        
        # 2. Prediction Label (0.5 threshold)
        prediction_label = "PHISHING" if phishing_prob >= 0.50 else "SAFE"
        
        # 3. Confidence Score: Distance from undecided 0.5 decision boundary
        confidence = round(float(2.0 * abs(phishing_prob - 0.50)), 4)
        confidence = max(0.50, min(0.99, confidence))
        
        return prediction_label, phishing_prob, confidence
