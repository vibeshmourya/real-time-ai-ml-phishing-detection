import logging
from typing import Tuple
import pandas as pd
from sklearn.model_selection import train_test_split

from feature_engineering.url_features import extract_features, get_feature_names
from app.config import settings

logger = logging.getLogger("phishshield.ml.preprocess")

def extract_features_dataframe(df: pd.DataFrame, url_col: str = "url") -> pd.DataFrame:
    """
    Extracts features for all URLs in a DataFrame.
    """
    logger.info("Extracting feature vectors for %d URLs...", len(df))
    feature_list = []
    
    for url in df[url_col]:
        feat = extract_features(url)
        feature_list.append(feat)
        
    features_df = pd.DataFrame(feature_list)
    return features_df

def prepare_train_test_data(
    df_clean: pd.DataFrame,
    url_col: str = "url",
    test_size: float = 0.20,
    random_state: int = 42
) -> Tuple[pd.DataFrame, pd.DataFrame, pd.Series, pd.Series]:
    """
    Extracts feature matrix X and target y, and performs stratified train/test split.
    Prevents data leakage.
    """
    X_features = extract_features_dataframe(df_clean, url_col=url_col)
    y = df_clean["target"]
    
    X_train, X_test, y_train, y_test = train_test_split(
        X_features, y,
        test_size=test_size,
        random_state=random_state,
        stratify=y
    )
    
    logger.info("Train/Test split complete: Train=%d, Test=%d (Test size=%.1f%%)", len(X_train), len(X_test), test_size * 100)
    return X_train, X_test, y_train, y_test
