from typing import Any

def generate_user_explanations(
    features: dict[str, Any],
    rule_indicators: list[dict[str, Any]],
    ml_probability: float,
    prediction: str
) -> list[str]:
    """
    Translates technical features and rule indicators into clear, non-technical,
    user-facing explanations.
    """
    explanations: list[str] = []
    
    # 1. High risk / Phishing explanations
    if prediction == "PHISHING" or ml_probability >= 0.5:
        if features.get("has_ip_address", 0) == 1:
            explanations.append(
                "The link uses a raw IP address instead of a recognized domain name, which is commonly seen on evasive scam servers."
            )
            
        if features.get("has_punycode", 0) == 1:
            explanations.append(
                "The web address uses look-alike international characters (punycode) that can visually disguise an impersonation domain."
            )
            
        if features.get("has_url_shortener", 0) == 1:
            explanations.append(
                "The link is masked using a URL shortening service, preventing you from seeing the destination website beforehand."
            )
            
        if features.get("at_symbol_count", 0) > 0:
            explanations.append(
                "The address includes an '@' symbol, which causes modern web browsers to ignore everything before it and redirect to an unexpected destination."
            )
            
        if features.get("suspicious_keyword_count", 0) > 0:
            explanations.append(
                "The web address contains sensitive action keywords (such as login, verify, or banking) that may be used to lure users into submitting credentials."
            )
            
        if features.get("subdomain_count", 0) >= 2:
            explanations.append(
                "The address includes multiple nested subdomains, a structure frequently employed in temporary phishing landing pages."
            )
            
        if features.get("hyphen_count", 0) >= 3:
            explanations.append(
                "The domain contains an unusually large number of hyphens, often used to simulate legitimate brand names."
            )
            
        if features.get("entropy", 0.0) > 4.0:
            explanations.append(
                "The domain name appears randomly generated, which is typical of automated domain generation algorithms used by malware operators."
            )
            
        if features.get("has_https", 1) == 0 and features.get("suspicious_keyword_count", 0) > 0:
            explanations.append(
                "The page does not use secure HTTPS encryption while requesting sensitive or login actions."
            )
            
        if not explanations:
            explanations.append(
                "The machine learning classifier detected combined structural and character patterns strongly associated with known phishing campaigns."
            )
    else:
        # Legitimate / Benign Explanations
        if features.get("has_https", 0) == 1:
            explanations.append(
                "The link uses standard HTTPS encryption to secure data transmission."
            )
        explanations.append(
            "The domain structure, length, and character distribution are consistent with typical benign web services."
        )
        explanations.append(
            "No high-risk evasion techniques (such as IP hostnames, punycode spoofing, or deceptive redirection tricks) were observed."
        )
        
    return explanations
