import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.api.routes import router
from app.config import settings
from app.ml.model_loader import get_model_container

# Configure structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("phishshield.ml.main")

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan context manager: executes model initialization on startup
    and clean shutdown hooks.
    """
    logger.info("Initializing PhishShield Machine Learning Microservice...")
    container = get_model_container()
    loaded = container.load_model()
    if loaded:
        logger.info("ML Model successfully loaded into memory.")
    else:
        logger.warning("ML Model artifact is not currently loaded: %s", container.load_error)
        
    yield
    
    logger.info("Shutting down PhishShield Machine Learning Microservice.")

app = FastAPI(
    title="PhishShield ML Detection Service",
    description="Real-time machine learning inference & heuristic explainability microservice for phishing detection.",
    version="1.0.0",
    lifespan=lifespan
)

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Exception Handlers for Clean Standardized Error Responses
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    msg = errors[0].get("msg", "Invalid request parameters.") if errors else "Validation failed."
    # Clean standard error message
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "success": False,
            "error": {
                "code": "VALIDATION_ERROR",
                "message": msg
            }
        }
    )

@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    if isinstance(exc.detail, dict) and "code" in exc.detail:
        err_content = exc.detail
    else:
        err_content = {
            "code": "HTTP_ERROR",
            "message": str(exc.detail)
        }
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error": err_content
        }
    )

@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    logger.error("Unhandled server exception: %s", str(exc), exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "success": False,
            "error": {
                "code": "INTERNAL_SERVER_ERROR",
                "message": "An unexpected error occurred during processing."
            }
        }
    )

# Include API Router
app.include_router(router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.APP_HOST, port=settings.APP_PORT, reload=True)
