from typing import Any, Optional, List, Dict
from pydantic import BaseModel, Field, field_validator
import re

class PredictionRequest(BaseModel):
    url: str = Field(..., min_length=4, max_length=2048, description="Target URL to inspect for phishing indicators")

    @field_validator("url")
    @classmethod
    def validate_url_protocol(cls, v: str) -> str:
        v_clean = v.strip()
        if not v_clean:
            raise ValueError("URL cannot be empty or whitespace.")
        
        # Check scheme if present
        if re.match(r"^[a-zA-Z0-9+.-]+://", v_clean):
            if not (v_clean.startswith("http://") or v_clean.startswith("https://")):
                raise ValueError("Only http:// and https:// protocols are supported.")
        return v_clean

class FeatureExtractionRequest(BaseModel):
    url: str = Field(..., min_length=4, max_length=2048)

class RuleIndicatorResponse(BaseModel):
    code: str
    severity: str
    message: str
    weight: float

class ModelMetaResponse(BaseModel):
    name: str
    version: str
    selection_metric: Optional[str] = "f1"

class PredictionResponse(BaseModel):
    success: bool = True
    url: str
    prediction: str  # SAFE, PHISHING
    risk_level: str  # SAFE, SUSPICIOUS, HIGH_RISK
    risk_score: int
    ml_probability: float
    confidence: float
    threat_category: str
    indicators: list[RuleIndicatorResponse]
    explanation: list[str]
    model: ModelMetaResponse
    execution_time_ms: Optional[float] = None

class OCRResponse(BaseModel):
    success: bool = True
    extracted_text: str
    extracted_urls: List[str]
    detected_language_signals: List[str]

class QRResponse(BaseModel):
    success: bool = True
    qr_detected: bool
    decoded_url: Optional[str] = None
    message: str

class HealthResponse(BaseModel):
    status: str
    service: str
    model_loaded: bool
    model_version: Optional[str] = None
    model_name: Optional[str] = None

class ErrorDetail(BaseModel):
    code: str
    message: str

class ErrorResponse(BaseModel):
    success: bool = False
    error: ErrorDetail
