import os
import json
import logging
from pathlib import Path
from fastapi import APIRouter, HTTPException, status, UploadFile, File, Form
from app.schemas.prediction import (
    PredictionRequest,
    PredictionResponse,
    HealthResponse,
    FeatureExtractionRequest,
    OCRResponse,
    QRResponse,
    ErrorResponse
)
from app.services.prediction_service import PredictionService
from app.services.feature_service import FeatureService
from app.services.ocr_service import OCRService
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
        service="prahar-ml-service",
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

@router.post("/ocr", response_model=OCRResponse, tags=["Inspection"])
async def extract_ocr_from_image(
    file: UploadFile = File(...),
    text_hint: str = Form("")
):
    """
    Extracts text and embedded URLs from an uploaded screenshot or message image.
    """
    try:
        content = await file.read()
        res = OCRService.extract_text_and_urls(content, fallback_text=text_hint)
        return OCRResponse(
            success=res["success"],
            extracted_text=res["extracted_text"],
            extracted_urls=res["extracted_urls"],
            detected_language_signals=res["detected_language_signals"]
        )
    except Exception as e:
        logger.error("OCR extraction failed: %s", str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"code": "OCR_ERROR", "message": str(e)}
        )

@router.post("/qr", response_model=QRResponse, tags=["Inspection"])
async def decode_qr_image(file: UploadFile = File(...)):
    """
    Decodes QR code from an uploaded image using OpenCV.
    """
    try:
        content = await file.read()
        res = OCRService.decode_qr(content)
        return QRResponse(
            success=res["success"],
            qr_detected=res["qr_detected"],
            decoded_url=res["decoded_url"],
            message=res["message"]
        )
    except Exception as e:
        logger.error("QR decode failed: %s", str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"code": "QR_DECODE_ERROR", "message": str(e)}
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

@router.get("/model/evaluations", tags=["Monitoring"])
def get_model_evaluations():
    """Returns real empirical benchmark evaluations for all trained model architectures."""
    eval_dir = Path(__file__).resolve().parent.parent.parent.parent / "evaluation"
    evaluations = {}
    if eval_dir.exists():
        for file in eval_dir.glob("*_eval.json"):
            try:
                with open(file, "r", encoding="utf-8") as f:
                    name = file.stem.replace("_eval", "")
                    evaluations[name] = json.load(f)
            except Exception as e:
                logger.warning("Could not read evaluation file %s: %s", file, e)
    return {"evaluations": evaluations}

@router.post("/features", tags=["Inspection"])
def extract_url_features(request: FeatureExtractionRequest):
    """Returns the raw extracted 30-feature vector for a given URL without running inference."""
    return FeatureService.get_features(request.url)
