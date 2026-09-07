import json
import logging
import os
from pathlib import Path
from typing import Any, Optional
import joblib

from app.config import settings

logger = logging.getLogger("phishshield.ml.loader")

class ModelContainer:
    _instance: Optional["ModelContainer"] = None
    
    def __init__(self):
        self.model: Any = None
        self.metadata: dict[str, Any] = {}
        self.feature_names: list[str] = []
        self.is_loaded: bool = False
        self.load_error: Optional[str] = None
        
    @classmethod
    def get_instance(cls) -> "ModelContainer":
        if cls._instance is None:
            cls._instance = ModelContainer()
        return cls._instance

    def load_model(self, model_path: str = None, metadata_path: str = None) -> bool:
        """
        Loads the serialized machine learning model and its associated metadata.
        """
        m_path = Path(model_path or settings.MODEL_PATH)
        meta_path = Path(metadata_path or settings.MODEL_METADATA_PATH)
        
        if not m_path.exists():
            self.is_loaded = False
            self.load_error = f"Model file not found at {m_path}"
            logger.warning(self.load_error)
            return False
            
        try:
            self.model = joblib.load(str(m_path))
            
            if meta_path.exists():
                with open(meta_path, "r", encoding="utf-8") as f:
                    self.metadata = json.load(f)
                    self.feature_names = self.metadata.get("features", [])
            else:
                self.metadata = {
                    "model_name": type(self.model).__name__,
                    "version": "1.0.0",
                    "features": []
                }
                
            self.is_loaded = True
            self.load_error = None
            logger.info("Successfully loaded ML model '%s' from %s", self.metadata.get("model_name"), m_path)
            return True
        except Exception as e:
            self.is_loaded = False
            self.load_error = f"Failed to deserialize model: {str(e)}"
            logger.error(self.load_error, exc_info=True)
            return False

def get_model_container() -> ModelContainer:
    return ModelContainer.get_instance()
