import json
import logging
from pathlib import Path
from typing import Any
import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    confusion_matrix,
    classification_report
)

logger = logging.getLogger("phishshield.ml.eval")

def evaluate_model(
    model: Any,
    X_test: pd.DataFrame,
    y_test: pd.Series,
    model_name: str,
    output_dir: str = "evaluation"
) -> dict[str, Any]:
    """
    Evaluates a trained model on test data, computing classification metrics,
    confusion matrix, and feature importances.
    """
    y_pred = model.predict(X_test)
    
    # Probability computation for ROC-AUC
    if hasattr(model, "predict_proba"):
        y_prob = model.predict_proba(X_test)[:, 1]
    elif hasattr(model, "decision_function"):
        decision = model.decision_function(X_test)
        y_prob = 1.0 / (1.0 + np.exp(-decision))
    else:
        y_prob = y_pred
        
    acc = float(accuracy_score(y_test, y_pred))
    prec = float(precision_score(y_test, y_pred, zero_division=0))
    rec = float(recall_score(y_test, y_pred, zero_division=0))
    f1 = float(f1_score(y_test, y_pred, zero_division=0))
    
    try:
        roc_auc = float(roc_auc_score(y_test, y_prob))
    except Exception:
        roc_auc = 0.5
        
    cm = confusion_matrix(y_test, y_pred).tolist()
    report = classification_report(y_test, y_pred, output_dict=True, zero_division=0)
    
    # Feature importances (for tree-based models or linear models)
    feature_importances = {}
    underlying_estimator = getattr(model, "named_steps", {}).get("classifier", model)
    
    if hasattr(underlying_estimator, "feature_importances_"):
        imps = underlying_estimator.feature_importances_
        feature_importances = {
            col: round(float(imp), 4) for col, imp in zip(X_test.columns, imps)
        }
    elif hasattr(underlying_estimator, "coef_"):
        coefs = np.abs(underlying_estimator.coef_[0])
        feature_importances = {
            col: round(float(c), 4) for col, c in zip(X_test.columns, coefs)
        }
        
    sorted_importances = sorted(feature_importances.items(), key=lambda x: x[1], reverse=True)
    top_features = [{"feature": k, "importance": v} for k, v in sorted_importances[:10]]
    
    results = {
        "model_name": model_name,
        "metrics": {
            "accuracy": round(acc, 4),
            "precision": round(prec, 4),
            "recall": round(rec, 4),
            "f1": round(f1, 4),
            "roc_auc": round(roc_auc, 4)
        },
        "confusion_matrix": {
            "true_negative": cm[0][0] if len(cm) > 0 else 0,
            "false_positive": cm[0][1] if len(cm) > 0 and len(cm[0]) > 1 else 0,
            "false_negative": cm[1][0] if len(cm) > 1 else 0,
            "true_positive": cm[1][1] if len(cm) > 1 and len(cm[1]) > 1 else 0
        },
        "top_features": top_features,
        "classification_report": report
    }
    
    # Save evaluation report
    out_path = Path(output_dir)
    out_path.mkdir(parents=True, exist_ok=True)
    report_file = out_path / f"{model_name.lower().replace(' ', '_')}_eval.json"
    
    with open(report_file, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)
        
    logger.info(
        "Evaluation [%s]: Acc=%.4f, Prec=%.4f, Rec=%.4f, F1=%.4f, ROC-AUC=%.4f",
        model_name, acc, prec, rec, f1, roc_auc
    )
    return results
