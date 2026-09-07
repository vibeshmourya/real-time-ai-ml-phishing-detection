from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parent.parent

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="allow")

    APP_ENV: str = "development"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8001
    
    # Model Artifacts
    MODEL_PATH: str = str(BASE_DIR / "models" / "best_model.joblib")
    MODEL_METADATA_PATH: str = str(BASE_DIR / "models" / "model_metadata.json")
    
    # Dataset Configuration
    DATASET_PATH: str = str(BASE_DIR / "data" / "raw" / "phishing_dataset.csv")
    URL_COLUMN: str = "url"
    LABEL_COLUMN: str = "label"
    
    # Training Configuration
    MODEL_SELECTION_METRIC: str = "f1"
    RANDOM_SEED: int = 42
    
    # Privacy & Logging
    LOG_URLS: bool = False
    
    # URL Shorteners
    SHORTENER_DOMAINS: list[str] = [
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd",
        "buff.ly", "adf.ly", "bit.do", "cutt.ly", "tiny.cc", "rb.gy",
        "qr.ae", "rebrand.ly", "shorte.st"
    ]
    
    # Suspicious Phishing Keywords
    SUSPICIOUS_KEYWORDS: list[str] = [
        "login", "verify", "verification", "secure", "account", "update",
        "password", "bank", "wallet", "payment", "confirm", "signin",
        "security", "credential", "suspend", "unauthorized", "kyc", "alert",
        "refund", "claim", "billing", "service", "support", "authenticate"
    ]

settings = Settings()
