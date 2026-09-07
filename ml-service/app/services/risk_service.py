from typing import Any

class RiskService:
    @staticmethod
    def calculate_risk(
        url: str,
        ml_probability: float,
        rule_indicators: list[dict[str, Any]],
        features: dict[str, Any]
    ) -> dict[str, Any]:
        """
        Combines ML model output with rule-based heuristics to compute:
        - final_risk_score (0-100)
        - risk_level ('SAFE', 'SUSPICIOUS', 'HIGH_RISK')
        - threat_category
        """
        # 1. Base score from ML probability (0 - 70 points weight)
        ml_score = ml_probability * 70.0
        
        # 2. Additive score from rule indicators (0 - 30 points weight)
        indicator_weight_sum = sum(ind.get("weight", 0.0) for ind in rule_indicators)
        rule_score = min(30.0, indicator_weight_sum * 0.5)
        
        # 3. Aggregate Risk Score bounded between 0 and 100
        combined_score = ml_score + rule_score
        
        # If critical indicators exist (e.g. Punycode, @ symbol, direct IP + sensitive keyword), guarantee minimum floor
        critical_count = sum(1 for ind in rule_indicators if ind.get("severity") in ("CRITICAL", "HIGH"))
        if critical_count >= 2:
            combined_score = max(combined_score, 75.0)
            
        final_risk_score = int(min(100, max(0, round(combined_score))))
        
        # 4. Risk Level determination
        if final_risk_score >= 70:
            risk_level = "HIGH_RISK"
        elif final_risk_score >= 40:
            risk_level = "SUSPICIOUS"
        else:
            risk_level = "SAFE"
            
        # 5. Threat Category classification based on contextual keywords
        threat_category = RiskService._determine_category(url, final_risk_score, features)
        
        return {
            "risk_score": final_risk_score,
            "risk_level": risk_level,
            "threat_category": threat_category
        }

    @staticmethod
    def _determine_category(url: str, risk_score: int, features: dict[str, Any]) -> str:
        """Categorizes specific phishing vector based on URL cues."""
        if risk_score < 40:
            return "UNKNOWN_SUSPICIOUS" if risk_score > 20 else "BENIGN"
        
        url_lower = (url or "").lower()
        
        # Banking Phishing
        if any(kw in url_lower for kw in ["bank", "netbanking", "hdfc", "sbi", "icici", "paypal", "chase", "wellsfargo", "bankofamerica"]):
            return "BANKING_PHISHING"
            
        # Government / Tax Impersonation
        if any(kw in url_lower for kw in ["incometax", "tax-refund", "epfindia", "gov", "passport", "irs"]):
            return "GOVERNMENT_IMPERSONATION"
            
        # Fake KYC
        if any(kw in url_lower for kw in ["kyc", "pan-card", "aadhaar", "sim-block", "document-verify"]):
            return "FAKE_KYC"
            
        # Delivery / Courier Scams
        if any(kw in url_lower for kw in ["indiapost", "fedex", "dhl", "ups", "parcel-track", "delivery-fee"]):
            return "DELIVERY_SCAM"
            
        # Job / Work from Home Scams
        if any(kw in url_lower for kw in ["parttime-job", "daily-earning", "telegram-task", "job-offer", "hiring"]):
            return "JOB_SCAM"
            
        # Shopping / E-commerce Scams
        if any(kw in url_lower for kw in ["discount-store", "iphone-free", "giftcard-win", "flash-sale", "superdeal"]):
            return "SHOPPING_SCAM"
            
        # Generic Credential Theft
        if any(kw in url_lower for kw in ["login", "signin", "verify", "password", "security", "account", "webmail", "office365"]):
            return "CREDENTIAL_THEFT"
            
        return "PHISHING" if risk_score >= 70 else "UNKNOWN_SUSPICIOUS"
