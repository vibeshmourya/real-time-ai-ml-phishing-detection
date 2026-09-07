import logging
from typing import Any, Tuple
import pandas as pd
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import SVC

from training.evaluate import evaluate_model
from app.config import settings

logger = logging.getLogger("phishshield.ml.compare")

def get_candidate_models(random_state: int = 42) -> dict[str, Any]:
    """
    Initializes candidate classifiers with appropriate preprocessing pipelines.
    """
    models = {}
    
    # 1. Logistic Regression (Requires scaled features)
    models["Logistic Regression"] = Pipeline([
        ("scaler", StandardScaler()),
        ("classifier", LogisticRegression(max_iter=1000, random_state=random_state, class_weight="balanced"))
    ])
    
    # 2. Random Forest (Ensemble tree, scale invariant)
    models["Random Forest"] = RandomForestClassifier(
        n_estimators=100,
        max_depth=15,
        min_samples_split=4,
        random_state=random_state,
        class_weight="balanced",
        n_jobs=-1
    )
    
    # 3. XGBoost Classifier
    try:
        from xgboost import XGBClassifier
        models["XGBoost"] = XGBClassifier(
            n_estimators=100,
            max_depth=6,
            learning_rate=0.1,
            random_state=random_state,
            eval_metric="logloss",
            n_jobs=-1
        )
    except ImportError:
        logger.warning("XGBoost is not available; continuing with remaining candidate algorithms.")
        
    # 4. Support Vector Machine (Requires scaling + probability calibration)
    models["SVM"] = Pipeline([
        ("scaler", StandardScaler()),
        ("classifier", SVC(kernel="rbf", probability=True, random_state=random_state, class_weight="balanced"))
    ])
    
    return models

def train_and_compare_models(
    X_train: pd.DataFrame,
    X_test: pd.DataFrame,
    y_train: pd.Series,
    y_test: pd.Series,
    selection_metric: str = "f1",
    random_state: int = 42
) -> Tuple[Any, str, dict[str, Any], pd.DataFrame]:
    """
    Trains all candidate models, evaluates on test set, generates comparison table,
    and selects the best performing model based on selection_metric.
    """
    candidates = get_candidate_models(random_state=random_state)
    comparison_records = []
    evaluation_dict = {}
    trained_models = {}
    
    for name, model in candidates.items():
        logger.info("Training candidate model: %s ...", name)
        model.fit(X_train, y_train)
        trained_models[name] = model
        
        eval_result = evaluate_model(model, X_test, y_test, model_name=name)
        evaluation_dict[name] = eval_result
        
        metrics = eval_result["metrics"]
        comparison_records.append({
            "Model": name,
            "Accuracy": metrics["accuracy"],
            "Precision": metrics["precision"],
            "Recall": metrics["recall"],
            "F1": metrics["f1"],
            "ROC-AUC": metrics["roc_auc"]
        })
        
    comparison_df = pd.DataFrame(comparison_records)
    
    # Format comparison display
    logger.info("\n=== MODEL COMPARISON TABLE ===\n%s\n", comparison_df.to_string(index=False))
    
    # Select best model based on metric
    metric_key = selection_metric.lower()
    if metric_key == "f1":
        metric_col = "F1"
    elif metric_key == "roc_auc" or metric_key == "roc-auc":
        metric_col = "ROC-AUC"
    elif metric_key == "precision":
        metric_col = "Precision"
    elif metric_key == "recall":
        metric_col = "Recall"
    else:
        metric_col = "Accuracy"
        
    best_row = comparison_df.sort_values(by=[metric_col, "F1"], ascending=False).iloc[0]
    best_name = str(best_row["Model"])
    best_model = trained_models[best_name]
    best_eval = evaluation_dict[best_name]
    
    logger.info("Best model selected: '%s' with %s = %.4f", best_name, metric_col, best_row[metric_col])
    return best_model, best_name, best_eval, comparison_df
