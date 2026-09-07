import logging
import sys
from pathlib import Path

# Add project root to sys.path
BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from app.config import settings
from preprocessing.clean_dataset import clean_dataset
from preprocessing.preprocess import prepare_train_test_data
from training.compare_models import train_and_compare_models
from training.save_model import save_trained_model
from feature_engineering.url_features import get_feature_names

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("phishshield.ml.train")

def run_training_pipeline(
    dataset_path: str = None,
    url_col: str = None,
    label_col: str = None,
    selection_metric: str = None,
    random_state: int = None
):
    """
    Complete end-to-end training and serialization pipeline:
    CSV Dataset -> Clean -> Feature Extraction -> Train/Test Split ->
    Train 4 Models -> Compare Metrics -> Save Best Model & Metadata.
    """
    d_path = dataset_path or settings.DATASET_PATH
    u_col = url_col or settings.URL_COLUMN
    l_col = label_col or settings.LABEL_COLUMN
    sel_metric = selection_metric or settings.MODEL_SELECTION_METRIC
    seed = random_state or settings.RANDOM_SEED
    
    logger.info("Starting PhishShield Model Training Pipeline...")
    logger.info("Dataset: %s | URL Col: '%s' | Label Col: '%s' | Metric: '%s'", d_path, u_col, l_col, sel_metric)
    
    # 1. Dataset Validation & Cleaning
    df_clean, summary = clean_dataset(d_path, url_col=u_col, label_col=l_col)
    
    # 2. Feature Extraction & Stratified Train/Test Split
    X_train, X_test, y_train, y_test = prepare_train_test_data(
        df_clean,
        url_col=u_col,
        test_size=0.20,
        random_state=seed
    )
    
    # 3. Model Training, Evaluation & Comparison
    best_model, best_name, best_eval, comparison_df = train_and_compare_models(
        X_train, X_test, y_train, y_test,
        selection_metric=sel_metric,
        random_state=seed
    )
    
    # 4. Model Serialization & Metadata Archiving
    feature_names = get_feature_names()
    model_path, meta_path = save_trained_model(
        model=best_model,
        model_name=best_name,
        metrics=best_eval["metrics"],
        feature_names=feature_names,
        selection_metric=sel_metric,
        output_dir=str(BASE_DIR / "models"),
        version="1.0.0"
    )
    
    logger.info("=== TRAINING PIPELINE COMPLETED SUCCESSFULLY ===")
    logger.info("Best Model: %s", best_name)
    logger.info("Model Artifact: %s", model_path)
    logger.info("Metadata Artifact: %s", meta_path)
    logger.info("Top Features: %s", best_eval.get("top_features", [])[:5])
    
    return {
        "best_model_name": best_name,
        "comparison_table": comparison_df.to_dict(orient="records"),
        "metrics": best_eval["metrics"],
        "summary": summary
    }

if __name__ == "__main__":
    run_training_pipeline()
