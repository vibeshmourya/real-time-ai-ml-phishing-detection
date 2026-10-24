import datetime
import json
import logging
from pathlib import Path
from typing import Any, Dict, Optional, Tuple
import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score
)

logger = logging.getLogger("phishshield.ml.governance")

# Production Promotion Gate Thresholds
DEFAULT_PROMOTION_THRESHOLDS = {
    "min_f1": 0.80,
    "min_recall": 0.80,
    "min_precision": 0.75,
    "max_fpr": 0.03,        # Strict: Maximum 3% false positives on benign URLs
    "min_roc_auc": 0.85
}

class ModelGovernanceAuditor:
    """
    Enforces strict ML governance, provenance tracking, and threshold evaluation.
    Prevents automatic, unverified model deployment into production.
    """
    def __init__(
        self,
        baseline_model_path: Optional[str] = None,
        evaluation_dir: Optional[str] = None,
        thresholds: Optional[Dict[str, float]] = None
    ):
        base_dir = Path(__file__).resolve().parent.parent
        self.baseline_model_path = Path(baseline_model_path or base_dir / "models" / "best_model.joblib")
        self.evaluation_dir = Path(evaluation_dir or base_dir / "evaluation")
        self.evaluation_dir.mkdir(parents=True, exist_ok=True)
        self.thresholds = thresholds or DEFAULT_PROMOTION_THRESHOLDS

    def calculate_governance_metrics(
        self,
        y_true: pd.Series,
        y_pred: np.ndarray,
        y_prob: Optional[np.ndarray] = None
    ) -> Dict[str, Any]:
        """
        Calculates standard classification metrics and critical security metrics:
        False Positive Rate (FPR) = FP / (FP + TN)
        False Negative Rate (FNR) = FN / (FN + TP)
        """
        acc = float(accuracy_score(y_true, y_pred))
        prec = float(precision_score(y_true, y_pred, zero_division=0))
        rec = float(recall_score(y_true, y_pred, zero_division=0))
        f1 = float(f1_score(y_true, y_pred, zero_division=0))

        if y_prob is not None:
            try:
                roc_auc = float(roc_auc_score(y_true, y_prob))
            except Exception:
                roc_auc = 0.5
        else:
            roc_auc = 0.5

        cm = confusion_matrix(y_true, y_pred).tolist()
        tn = cm[0][0] if len(cm) > 0 else 0
        fp = cm[0][1] if len(cm) > 0 and len(cm[0]) > 1 else 0
        fn = cm[1][0] if len(cm) > 1 else 0
        tp = cm[1][1] if len(cm) > 1 and len(cm[1]) > 1 else 0

        total_negatives = max(1, tn + fp)
        total_positives = max(1, tp + fn)
        fpr = float(fp / total_negatives)
        fnr = float(fn / total_positives)

        return {
            "accuracy": round(acc, 4),
            "precision": round(prec, 4),
            "recall": round(rec, 4),
            "f1": round(f1, 4),
            "roc_auc": round(roc_auc, 4),
            "false_positive_rate": round(fpr, 4),
            "false_negative_rate": round(fnr, 4),
            "confusion_matrix": {
                "true_negative": tn,
                "false_positive": fp,
                "false_negative": fn,
                "true_positive": tp
            }
        }

    def evaluate_candidate(
        self,
        candidate_model: Any,
        candidate_name: str,
        X_test: pd.DataFrame,
        y_test: pd.Series,
        dataset_provenance: Optional[Dict[str, Any]] = None,
        candidate_version: str = "2.0.0-candidate"
    ) -> Dict[str, Any]:
        """
        Evaluates a candidate model against governance thresholds and baseline model.
        Outputs an auditable review artifact.
        """
        # Predict with candidate
        y_pred = candidate_model.predict(X_test)
        if hasattr(candidate_model, "predict_proba"):
            y_prob = candidate_model.predict_proba(X_test)[:, 1]
        else:
            y_prob = None

        candidate_metrics = self.calculate_governance_metrics(y_test, y_pred, y_prob)

        # Baseline model comparison if available
        baseline_metrics = None
        if self.baseline_model_path.exists():
            try:
                baseline_model = joblib.load(self.baseline_model_path)
                base_pred = baseline_model.predict(X_test)
                base_prob = baseline_model.predict_proba(X_test)[:, 1] if hasattr(baseline_model, "predict_proba") else None
                baseline_metrics = self.calculate_governance_metrics(y_test, base_pred, base_prob)
            except Exception as e:
                logger.warning("Could not evaluate baseline model for comparison: %s", e)

        # Check Gate Thresholds
        threshold_results = {
            "f1_passed": candidate_metrics["f1"] >= self.thresholds["min_f1"],
            "recall_passed": candidate_metrics["recall"] >= self.thresholds["min_recall"],
            "precision_passed": candidate_metrics["precision"] >= self.thresholds["min_precision"],
            "fpr_passed": candidate_metrics["false_positive_rate"] <= self.thresholds["max_fpr"],
            "roc_auc_passed": candidate_metrics["roc_auc"] >= self.thresholds["min_roc_auc"]
        }

        all_passed = all(threshold_results.values())
        governance_status = "PENDING_HUMAN_REVIEW" if all_passed else "REJECTED_THRESHOLD_NOT_MET"

        evaluation_record = {
            "audit_timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "candidate_name": candidate_name,
            "candidate_version": candidate_version,
            "governance_status": governance_status,
            "promotion_eligible": all_passed,
            "thresholds_configured": self.thresholds,
            "threshold_evaluations": threshold_results,
            "candidate_metrics": candidate_metrics,
            "baseline_comparison": baseline_metrics,
            "dataset_provenance": dataset_provenance or {},
            "evaluation_samples": len(y_test),
            "governance_rule": "Candidate models CANNOT be automatically promoted to production. Human DBA/MLOps signoff is required."
        }

        # Save auditable report
        timestamp_str = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%d_%H%M%S")
        report_file = self.evaluation_dir / f"governance_audit_{timestamp_str}.json"
        with open(report_file, "w", encoding="utf-8") as f:
            json.dump(evaluation_record, f, indent=2)

        logger.info(
            "Governance Audit Completed: Status=%s, F1=%.4f (Pass=%s), FPR=%.4f (Pass=%s)",
            governance_status,
            candidate_metrics["f1"], threshold_results["f1_passed"],
            candidate_metrics["false_positive_rate"], threshold_results["fpr_passed"]
        )
        logger.info("Audit Report Archived: %s", report_file)
        return evaluation_record

    def promote_candidate_model(
        self,
        candidate_model_path: str,
        human_approval_signature: str,
        audit_report_path: str
    ) -> bool:
        """
        Gated promotion: Replaces production model ONLY after validating human signoff
        and audit report confirmation.
        """
        if not human_approval_signature or len(human_approval_signature.strip()) < 5:
            raise ValueError("Model promotion denied: Valid human operator approval signature is required.")

        report_p = Path(audit_report_path)
        if not report_p.exists():
            raise FileNotFoundError(f"Audit report not found at {audit_report_path}")

        with open(report_p, "r", encoding="utf-8") as f:
            report_data = json.load(f)

        if not report_data.get("promotion_eligible", False):
            raise ValueError("Model promotion denied: Candidate model failed one or more governance threshold gates.")

        # Archive current baseline before replacing
        if self.baseline_model_path.exists():
            backup_path = self.baseline_model_path.parent / f"baseline_backup_{datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d_%H%M%S')}.joblib"
            joblib.dump(joblib.load(self.baseline_model_path), backup_path)
            logger.info("Archived prior baseline model to %s", backup_path)

        # Deploy candidate model
        candidate = joblib.load(candidate_model_path)
        joblib.dump(candidate, self.baseline_model_path)
        logger.info(
            "SUCCESS: Candidate model promoted to production by '%s'. Artifact: %s",
            human_approval_signature, self.baseline_model_path
        )
        return True
