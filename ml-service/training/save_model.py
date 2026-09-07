import json
import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
import joblib

logger = logging.getLogger("phishshield.ml.save")

def save_trained_model(
    model: Any,
    model_name: str,
    metrics: dict[str, float],
    feature_names: list[str],
    selection_metric: str,
    output_dir: str = "models",
    version: str = "1.0.0"
) -> tuple[str, str]:
    """
    Serializes best model artifact to .joblib and saves structured metadata to .json.
    """
    out_path = Path(output_dir)
    out_path.mkdir(parents=True, exist_ok=True)
    
    model_file = out_path / "best_model.joblib"
    meta_file = out_path / "model_metadata.json"
    
    # 1. Serialize Model
    joblib.dump(model, str(model_file))
    logger.info("Saved serialized model artifact to %s", model_file)
    
    # 2. Serialize Metadata
    metadata = {
        "model_name": model_name,
        "version": version,
        "training_timestamp": datetime.now(timezone.utc).isoformat(),
        "selection_metric": selection_metric,
        "metrics": metrics,
        "features": feature_names,
        "feature_count": len(feature_names)
    }
    
    with open(meta_file, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
        
    logger.info("Saved model metadata to %s", meta_file)
    return str(model_file), str(meta_file)
