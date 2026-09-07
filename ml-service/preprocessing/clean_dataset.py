import logging
from pathlib import Path
from typing import Tuple
import pandas as pd

logger = logging.getLogger("phishshield.ml.clean")

def clean_dataset(
    input_path: str,
    url_col: str = "url",
    label_col: str = "label"
) -> Tuple[pd.DataFrame, dict]:
    """
    Validates, cleans, deduplicates, and normalizes a raw URL dataset.
    Returns (cleaned_dataframe, cleaning_summary_dict).
    """
    path = Path(input_path)
    if not path.exists():
        raise FileNotFoundError(f"Dataset not found at {input_path}")
        
    logger.info("Loading raw dataset from %s", path)
    df = pd.read_csv(path)
    total_records = len(df)
    
    # 1. Check for required columns
    if url_col not in df.columns:
        raise ValueError(f"URL column '{url_col}' not found in dataset. Columns: {list(df.columns)}")
    if label_col not in df.columns:
        raise ValueError(f"Label column '{label_col}' not found in dataset. Columns: {list(df.columns)}")
        
    # 2. Drop rows with null/empty URL or label
    df_clean = df[[url_col, label_col]].dropna().copy()
    null_dropped = total_records - len(df_clean)
    
    # Convert URLs to string and strip whitespace
    df_clean[url_col] = df_clean[url_col].astype(str).str.strip()
    df_clean = df_clean[df_clean[url_col] != ""]
    
    # 3. Remove duplicates
    initial_valid = len(df_clean)
    df_clean = df_clean.drop_duplicates(subset=[url_col]).copy()
    duplicates_removed = initial_valid - len(df_clean)
    
    # 4. Standardize / Normalize labels to binary 0 (Legitimate/Safe) and 1 (Phishing/Malicious)
    def normalize_label(val):
        if isinstance(val, (int, float)):
            return 1 if int(val) == 1 else 0
        s = str(val).strip().lower()
        if s in ("1", "phishing", "malicious", "bad", "phish", "spam", "fraud"):
            return 1
        return 0

    df_clean["target"] = df_clean[label_col].apply(normalize_label)
    
    # Class breakdown
    legitimate_count = int((df_clean["target"] == 0).sum())
    phishing_count = int((df_clean["target"] == 1).sum())
    
    summary = {
        "total_records_raw": total_records,
        "valid_records": len(df_clean),
        "removed_null_or_empty": null_dropped,
        "duplicates_removed": duplicates_removed,
        "legitimate_count": legitimate_count,
        "phishing_count": phishing_count,
        "phishing_ratio": round(phishing_count / max(1, len(df_clean)), 4)
    }
    
    report_text = f"""
Dataset Summary
----------------------------
Total records:            {summary['total_records_raw']}
Valid records:            {summary['valid_records']}
Null / Empty removed:     {summary['removed_null_or_empty']}
Duplicate records removed:{summary['duplicates_removed']}
Legitimate:               {summary['legitimate_count']}
Phishing:                 {summary['phishing_count']}
Phishing Ratio:           {summary['phishing_ratio'] * 100:.1f}%
----------------------------"""
    
    logger.info(report_text)
    return df_clean[[url_col, "target"]], summary
