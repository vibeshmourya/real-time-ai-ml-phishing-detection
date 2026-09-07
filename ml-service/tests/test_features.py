import pytest
from feature_engineering.url_features import extract_features, get_feature_names, FEATURE_NAMES

def test_feature_names_count():
    names = get_feature_names()
    assert len(names) == 30
    assert "url_length" in names
    assert "has_https" in names
    assert "has_ip_address" in names
    assert "has_punycode" in names
    assert "has_url_shortener" in names
    assert "entropy" in names

def test_normal_https_url():
    url = "https://www.google.com/search?q=cybersecurity"
    features = extract_features(url)
    
    assert features["has_https"] == 1
    assert features["has_ip_address"] == 0
    assert features["has_punycode"] == 0
    assert features["has_url_shortener"] == 0
    assert features["dot_count"] >= 2
    assert features["query_parameter_count"] == 1
    assert features["url_length"] == len(url)
    assert features["entropy"] > 0

def test_ip_address_url():
    url = "http://192.168.1.100/login.php"
    features = extract_features(url)
    
    assert features["has_https"] == 0
    assert features["has_ip_address"] == 1
    assert features["suspicious_keyword_count"] >= 1
    assert features["contains_suspicious_keyword"] == 1

def test_punycode_url():
    url = "http://xn--pypal-4ve.com/account-verification"
    features = extract_features(url)
    
    assert features["has_punycode"] == 1
    assert features["suspicious_keyword_count"] >= 1

def test_shortened_url():
    url = "https://bit.ly/3xXBankVerify"
    features = extract_features(url)
    
    assert features["has_url_shortener"] == 1
    assert features["has_https"] == 1

def test_long_complex_url():
    url = "https://sub.portal.domain-verify.com/auth/login/step1?token=abcdef123456&session=987654#target"
    features = extract_features(url)
    
    assert features["subdomain_count"] >= 2
    assert features["hyphen_count"] >= 1
    assert features["path_segment_count"] >= 3
    assert features["query_parameter_count"] >= 2
    assert features["fragment_length"] > 0

def test_malformed_url_safety():
    # Service must never throw unhandled exception on garbage strings
    features_empty = extract_features("")
    assert features_empty["url_length"] == 0
    
    features_none = extract_features(None)
    assert features_none["url_length"] == 0
    
    features_special = extract_features("://bad-url@@@///")
    assert isinstance(features_special, dict)
    assert len(features_special) == len(FEATURE_NAMES)
