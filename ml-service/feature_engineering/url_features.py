import re
from urllib.parse import parse_qs, unquote
from app.utils.url_utils import (
    parse_url_safe,
    is_ip_address,
    contains_punycode,
    is_url_shortener,
    calculate_shannon_entropy,
    check_suspicious_keywords
)
from app.config import settings

FEATURE_NAMES = [
    "url_length",
    "hostname_length",
    "path_length",
    "query_length",
    "fragment_length",
    "dot_count",
    "hyphen_count",
    "underscore_count",
    "slash_count",
    "question_mark_count",
    "equal_count",
    "ampersand_count",
    "percent_count",
    "at_symbol_count",
    "digit_count",
    "special_character_count",
    "subdomain_count",
    "path_segment_count",
    "query_parameter_count",
    "has_https",
    "has_ip_address",
    "has_port",
    "has_punycode",
    "has_url_shortener",
    "has_encoded_characters",
    "suspicious_keyword_count",
    "contains_suspicious_keyword",
    "entropy",
    "digit_ratio",
    "special_char_ratio"
]

def get_feature_names() -> list[str]:
    """Returns the ordered canonical feature column names."""
    return list(FEATURE_NAMES)

def extract_features(url: str, custom_keywords: list[str] = None, custom_shorteners: list[str] = None) -> dict:
    """
    Extracts deterministic numerical/boolean feature vector for a URL.
    Used identically during training and real-time inference.
    """
    keywords = custom_keywords if custom_keywords is not None else settings.SUSPICIOUS_KEYWORDS
    shorteners = custom_shorteners if custom_shorteners is not None else settings.SHORTENER_DOMAINS
    
    if not url or not isinstance(url, str):
        # Return zeroed baseline dictionary
        return {name: 0 for name in FEATURE_NAMES}
    
    url_clean = url.strip()
    parsed = parse_url_safe(url_clean)
    
    if not parsed:
        return {name: 0 for name in FEATURE_NAMES}
    
    hostname = parsed.hostname or ""
    path = parsed.path or ""
    query = parsed.query or ""
    fragment = parsed.fragment or ""
    
    # 1. Lexical Length Features
    url_length = len(url_clean)
    hostname_length = len(hostname)
    path_length = len(path)
    query_length = len(query)
    fragment_length = len(fragment)
    
    # 2. Character Count Features
    dot_count = url_clean.count(".")
    hyphen_count = url_clean.count("-")
    underscore_count = url_clean.count("_")
    slash_count = url_clean.count("/")
    question_mark_count = url_clean.count("?")
    equal_count = url_clean.count("=")
    ampersand_count = url_clean.count("&")
    percent_count = url_clean.count("%")
    at_symbol_count = url_clean.count("@")
    digit_count = sum(1 for c in url_clean if c.isdigit())
    
    # Special characters (anything not alphanumeric and not standard url delimiter)
    special_chars = set("!$&'()*+,;=~`^{}[]|\\<>\"#%")
    special_character_count = sum(1 for c in url_clean if c in special_chars or c in "@-_.")
    
    # 3. Structural Features
    # Subdomain count: parts in hostname excluding domain and TLD
    host_parts = hostname.split(".")
    subdomain_count = max(0, len(host_parts) - 2) if len(host_parts) > 2 else 0
    
    # Path segments
    path_segments = [seg for seg in path.strip("/").split("/") if seg]
    path_segment_count = len(path_segments)
    
    # Query parameters count
    try:
        query_params = parse_qs(query)
        query_parameter_count = len(query_params)
    except Exception:
        query_parameter_count = query.count("=")
        
    # 4. Security-Related Indicators
    has_https = 1 if parsed.scheme.lower() == "https" else 0
    has_ip_address_val = 1 if is_ip_address(hostname) else 0
    has_port_val = 1 if parsed.port is not None and parsed.port not in (80, 443) else 0
    has_punycode_val = 1 if contains_punycode(hostname) else 0
    has_url_shortener_val = 1 if is_url_shortener(hostname, shorteners) else 0
    has_encoded_chars = 1 if "%" in url_clean else 0
    
    # 5. Suspicious Keywords
    contains_kw, matched_kw = check_suspicious_keywords(url_clean, keywords)
    suspicious_keyword_count = len(matched_kw)
    contains_suspicious_keyword_val = 1 if contains_kw else 0
    
    # 6. Entropy & Ratios
    entropy = calculate_shannon_entropy(url_clean)
    digit_ratio = round(digit_count / max(1, url_length), 4)
    special_char_ratio = round(special_character_count / max(1, url_length), 4)
    
    return {
        "url_length": url_length,
        "hostname_length": hostname_length,
        "path_length": path_length,
        "query_length": query_length,
        "fragment_length": fragment_length,
        "dot_count": dot_count,
        "hyphen_count": hyphen_count,
        "underscore_count": underscore_count,
        "slash_count": slash_count,
        "question_mark_count": question_mark_count,
        "equal_count": equal_count,
        "ampersand_count": ampersand_count,
        "percent_count": percent_count,
        "at_symbol_count": at_symbol_count,
        "digit_count": digit_count,
        "special_character_count": special_character_count,
        "subdomain_count": subdomain_count,
        "path_segment_count": path_segment_count,
        "query_parameter_count": query_parameter_count,
        "has_https": has_https,
        "has_ip_address": has_ip_address_val,
        "has_port": has_port_val,
        "has_punycode": has_punycode_val,
        "has_url_shortener": has_url_shortener_val,
        "has_encoded_characters": has_encoded_chars,
        "suspicious_keyword_count": suspicious_keyword_count,
        "contains_suspicious_keyword": contains_suspicious_keyword_val,
        "entropy": entropy,
        "digit_ratio": digit_ratio,
        "special_char_ratio": special_char_ratio
    }
