import math
import re
from collections import Counter
from urllib.parse import urlparse, unquote

IP_V4_PATTERN = re.compile(r"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")

def parse_url_safe(url: str):
    """
    Safely parses a URL string into urllib ParseResult.
    Ensures protocol is present if missing for parsing.
    """
    if not url or not isinstance(url, str):
        return None
    
    url = url.strip()
    if not (url.startswith("http://") or url.startswith("https://")):
        url = "http://" + url
        
    try:
        parsed = urlparse(url)
        return parsed
    except Exception:
        return None

def normalize_url(url: str) -> str:
    """Removes scheme, trailing slash and standardizes lowercase."""
    if not url:
        return ""
    u = url.strip().lower()
    u = re.sub(r"^https?://", "", u)
    u = re.sub(r"^www\.", "", u)
    return u.rstrip("/")

def is_ip_address(hostname: str) -> bool:
    """Checks if hostname is an IPv4 address."""
    if not hostname:
        return False
    # Strip port if present
    host = hostname.split(":")[0].strip("[]")
    return bool(IP_V4_PATTERN.match(host))

def contains_punycode(domain: str) -> bool:
    """Checks if domain uses punycode homograph encoding."""
    if not domain:
        return False
    return "xn--" in domain.lower()

def is_url_shortener(hostname: str, shorteners: list[str]) -> bool:
    """Checks if hostname belongs to a known URL shortener service."""
    if not hostname:
        return False
    host = hostname.lower().split(":")[0].strip()
    for s in shorteners:
        if host == s or host.endswith("." + s):
            return True
    return False

def calculate_shannon_entropy(text: str) -> float:
    """
    Calculates Shannon Character Entropy: H(X) = - sum(P(x) * log2(P(x))).
    Higher entropy indicates random/algorithmically generated strings.
    """
    if not text:
        return 0.0
    length = len(text)
    counts = Counter(text)
    entropy = 0.0
    for count in counts.values():
        p = count / length
        entropy -= p * math.log2(p)
    return round(entropy, 4)

def check_suspicious_keywords(url: str, keywords: list[str]) -> tuple[bool, list[str]]:
    """
    Checks for presence of suspicious phishing keywords.
    Returns (contains_flag, matched_keywords_list).
    """
    if not url:
        return False, []
    
    lower_url = url.lower()
    matched = [kw for kw in keywords if kw.lower() in lower_url]
    return len(matched) > 0, matched
