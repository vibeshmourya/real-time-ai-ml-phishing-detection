import io
import re
import cv2
import numpy as np
import logging
from typing import Dict, Any, List

logger = logging.getLogger("phishshield.ml.ocr")

URL_REGEX = re.compile(
    r"(?:https?://|www\.)[^\s<>'\"{}|\^`\[\]]+",
    re.IGNORECASE
)

class OCRService:
    @staticmethod
    def decode_qr(image_bytes: bytes) -> Dict[str, Any]:
        """
        Decodes QR codes from image bytes using OpenCV QRCodeDetector.
        """
        try:
            nparr = np.frombuffer(image_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            if img is None:
                return {
                    "success": False,
                    "qr_detected": False,
                    "decoded_url": None,
                    "message": "Unable to decode image file format."
                }

            detector = cv2.QRCodeDetector()
            data, bbox, _ = detector.detectAndDecode(img)
            
            if data and data.strip():
                return {
                    "success": True,
                    "qr_detected": True,
                    "decoded_url": data.strip(),
                    "message": "QR Code detected and decoded successfully."
                }
            
            return {
                "success": True,
                "qr_detected": False,
                "decoded_url": None,
                "message": "No QR code was detected in this image."
            }
        except Exception as e:
            logger.error("QR decoding exception: %s", str(e), exc_info=True)
            return {
                "success": False,
                "qr_detected": False,
                "decoded_url": None,
                "message": f"QR decoding error: {str(e)}"
            }

    @staticmethod
    def extract_text_and_urls(image_bytes: bytes, fallback_text: str = "") -> Dict[str, Any]:
        """
        Extracts textual content and embedded URLs from image.
        Uses fallback text if provided or scans image buffer.
        """
        extracted_text = fallback_text or ""
        
        # Check if pytesseract or image OCR is available
        try:
            import pytesseract
            from PIL import Image
            img = Image.open(io.BytesIO(image_bytes))
            ocr_text = pytesseract.image_to_string(img)
            if ocr_text and ocr_text.strip():
                extracted_text = ocr_text.strip()
        except Exception:
            # If tesseract binary is not installed locally, use fallback or QR text
            pass

        # Extract URLs
        found_urls = URL_REGEX.findall(extracted_text)
        
        # Detect language signals
        signals = []
        lower = extracted_text.lower()
        if any(k in lower for k in ["kyc", "aadhaar", "pan card", "sim block"]):
            signals.append("FAKE_KYC_KEYWORDS")
        if any(k in lower for k in ["urgent", "within 24 hours", "suspended", "immediately"]):
            signals.append("ARTIFICIAL_URGENCY")
        if any(k in lower for k in ["won", "lottery", "gift card", "prize"]):
            signals.append("REWARD_BAIT")
        if any(k in lower for k in ["netbanking", "login", "password", "otp"]):
            signals.append("CREDENTIAL_REQUEST")

        return {
            "success": True,
            "extracted_text": extracted_text,
            "extracted_urls": found_urls,
            "detected_language_signals": signals
        }
