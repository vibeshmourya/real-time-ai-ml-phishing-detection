import logging
from fastapi import APIRouter, HTTPException, status
from app.schemas.prediction import (
    PredictionRequest,
    PredictionResponse,
    HealthResponse,
    FeatureExtractionRequest,
    ErrorResponse
)
from app.services.prediction_service import PredictionService
from app.services.feature_service import FeatureService
from app.ml.model_loader import get_model_container

logger = logging.getLogger("phishshield.ml.routes")
router = APIRouter()

prediction_service = PredictionService()

@router.get("/health", response_model=HealthResponse, tags=["Monitoring"])
def health_check():
    """
    Returns the operational health and model loading status of the ML microservice.
    """
    container = get_model_container()
    model_meta = container.metadata or {}
    
    return HealthResponse(
        status="UP",
        service="phishing-ml-service",
        model_loaded=container.is_loaded,
        model_version=model_meta.get("version", "1.0.0") if container.is_loaded else None,
        model_name=model_meta.get("model_name") if container.is_loaded else None
    )

@router.post(
    "/predict",
    response_model=PredictionResponse,
    responses={
        400: {"model": ErrorResponse},
        422: {"model": ErrorResponse},
        503: {"model": ErrorResponse}
    },
    tags=["Phishing Detection"]
)
def predict_url(request: PredictionRequest):
    """
    Inspects target URL, extracts lexical/structural features, executes ML classification,
    evaluates rule indicators, and returns a structured risk assessment with human-friendly explanations.
    """
    container = get_model_container()
    if not container.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "code": "MODEL_UNAVAILABLE",
                "message": f"ML model is not loaded: {container.load_error or 'Model artifact missing. Please train model.'}"
            }
        )
        
    try:
        result = prediction_service.process_url(request.url)
        return result
    except Exception as e:
        logger.error("Inference execution failed for URL: %s", str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={
                "code": "INFERENCE_ERROR",
                "message": f"Failed to execute machine learning inference: {str(e)}"
            }
        )

@router.get("/model/info", tags=["Monitoring"])
def get_model_info():
    """Returns metadata and training metrics of the currently active model."""
    container = get_model_container()
    if not container.is_loaded:
        return {
            "model_loaded": False,
            "error": container.load_error or "Model not loaded"
        }
    return {
        "model_loaded": True,
        "metadata": container.metadata,
        "feature_count": len(container.feature_names)
    }

@router.post("/features", tags=["Inspection"])
def extract_url_features(request: FeatureExtractionRequest):
    """Returns the raw extracted 30-feature vector for a given URL without running inference."""
    return FeatureService.get_features(request.url)
