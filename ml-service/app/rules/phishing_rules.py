from typing import Any
from app.utils.url_utils import (
    parse_url_safe,
    is_ip_address,
    contains_punycode,
    is_url_shortener,
    calculate_shannon_entropy,
    check_suspicious_keywords
)
from app.config import settings

class RuleIndicator:
    def __init__(self, code: str, severity: str, message: str, weight: float):
        self.code = code
        self.severity = severity  # LOW, MEDIUM, HIGH, CRITICAL
        self.message = message
        self.weight = weight

    def to_dict(self) -> dict[str, Any]:
        return {
            "code": self.code,
            "severity": self.severity,
            "message": self.message,
            "weight": self.weight
        }

def evaluate_phishing_rules(url: str, custom_keywords: list[str] = None) -> list[dict[str, Any]]:
    """
    Evaluates transparent heuristic rules against a URL and returns triggered indicators.
    """
    indicators: list[RuleIndicator] = []
    
    if not url or not isinstance(url, str):
        return []
    
    clean_url = url.strip()
    parsed = parse_url_safe(clean_url)
    if not parsed:
        return []
    
    hostname = parsed.hostname or ""
    path = parsed.path or ""
    keywords = custom_keywords if custom_keywords is not None else settings.SUSPICIOUS_KEYWORDS
    
    # 1. IP-based Hostname
    if is_ip_address(hostname):
        indicators.append(RuleIndicator(
            code="IP_BASED_HOST",
            severity="HIGH",
            message="The hostname is an IP address rather than a registered domain name.",
            weight=30.0
        ))
        
    # 2. Punycode / Homoglyph Encoding
    if contains_punycode(hostname):
        indicators.append(RuleIndicator(
            code="PUNYCODE_HOMOGLYPH",
            severity="CRITICAL",
            message="The domain uses Punycode ('xn--') characters to impersonate legitimate brand spellings.",
            weight=35.0
        ))
        
    # 3. URL Shortener Service
    if is_url_shortener(hostname, settings.SHORTENER_DOMAINS):
        indicators.append(RuleIndicator(
            code="URL_SHORTENER",
            severity="MEDIUM",
            message="The link uses a URL shortening service that conceals the true destination.",
            weight=15.0
        ))
        
    # 4. @ Symbol Redirection Trap
    if "@" in clean_url:
        indicators.append(RuleIndicator(
            code="AT_SYMBOL_OBFUSCATION",
            severity="CRITICAL",
            message="The URL contains an '@' symbol, which causes browsers to ignore preceding text.",
            weight=30.0
        ))
        
    # 5. Suspicious Phishing Keywords
    has_kw, matched_kws = check_suspicious_keywords(clean_url, keywords)
    if has_kw:
        matched_str = ", ".join(matched_kws[:3])
        indicators.append(RuleIndicator(
            code="SUSPICIOUS_KEYWORD",
            severity="MEDIUM",
            message=f"The URL contains sensitive security or verification keywords ({matched_str}).",
            weight=20.0
        ))
        
    # 6. Deep Subdomain Hierarchy
    host_parts = hostname.split(".")
    if len(host_parts) > 3:
        indicators.append(RuleIndicator(
            code="DEEP_SUBDOMAINS",
            severity="MEDIUM",
            message="The domain contains multiple nested subdomain tiers commonly used in phishing infrastructure.",
            weight=15.0
        ))
        
    # 7. Excessive Hyphens
    hyphen_count = hostname.count("-")
    if hyphen_count >= 3:
        indicators.append(RuleIndicator(
            code="EXCESSIVE_HYPHENS",
            severity="MEDIUM",
            message=f"The domain contains an unusually high number of hyphens ({hyphen_count}).",
            weight=15.0
        ))
        
    # 8. Excessive URL Length
    if len(clean_url) > 90:
        indicators.append(RuleIndicator(
            code="EXCESSIVE_URL_LENGTH",
            severity="LOW",
            message=f"The URL is abnormally long ({len(clean_url)} characters), which can obscure malicious parameters.",
            weight=10.0
        ))
        
    # 9. Non-standard Port
    if parsed.port and parsed.port not in (80, 443, 8080):
        indicators.append(RuleIndicator(
            code="SUSPICIOUS_PORT",
            severity="HIGH",
            message=f"The URL connects via a non-standard port ({parsed.port}).",
            weight=20.0
        ))
        
    # 10. High Character Entropy
    entropy = calculate_shannon_entropy(hostname)
    if entropy > 3.85 and len(hostname) > 12:
        indicators.append(RuleIndicator(
            code="HIGH_ENTROPY",
            severity="MEDIUM",
            message=f"The domain name exhibits high character randomness (entropy score {entropy:.2f}).",
            weight=15.0
        ))
        
    # 11. Unencrypted Authentication / Sensitive Action
    if parsed.scheme.lower() == "http" and has_kw:
        indicators.append(RuleIndicator(
            code="UNENCRYPTED_AUTH",
            severity="HIGH",
            message="The URL asks for credentials or verification over unencrypted HTTP protocol.",
            weight=25.0
        ))
        
    # 12. Encoded Special Characters
    if clean_url.count("%") >= 3:
        indicators.append(RuleIndicator(
            code="ENCODED_CHARACTERS",
            severity="LOW",
            message="The URL contains multiple hex-encoded characters that may obfuscate parameters.",
            weight=10.0
        ))
        
    return [ind.to_dict() for ind in indicators]
