import pytest
from app.rules.phishing_rules import evaluate_phishing_rules

def test_ip_based_rule():
    url = "http://192.168.1.1/admin"
    indicators = evaluate_phishing_rules(url)
    codes = [ind["code"] for ind in indicators]
    assert "IP_BASED_HOST" in codes

def test_punycode_rule():
    url = "https://xn--appl-4qa.com/login"
    indicators = evaluate_phishing_rules(url)
    codes = [ind["code"] for ind in indicators]
    assert "PUNYCODE_HOMOGLYPH" in codes

def test_at_symbol_rule():
    url = "http://www.google.com@phishing-target.com/signin"
    indicators = evaluate_phishing_rules(url)
    codes = [ind["code"] for ind in indicators]
    assert "AT_SYMBOL_OBFUSCATION" in codes

def test_shortener_rule():
    url = "https://tinyurl.com/bank-kyc"
    indicators = evaluate_phishing_rules(url)
    codes = [ind["code"] for ind in indicators]
    assert "URL_SHORTENER" in codes

def test_suspicious_keywords_rule():
    url = "https://example-domain.com/security/account-verify/password"
    indicators = evaluate_phishing_rules(url)
    codes = [ind["code"] for ind in indicators]
    assert "SUSPICIOUS_KEYWORD" in codes

def test_unencrypted_auth_rule():
    url = "http://login-portal.net/verify"
    indicators = evaluate_phishing_rules(url)
    codes = [ind["code"] for ind in indicators]
    assert "UNENCRYPTED_AUTH" in codes

def test_benign_url_no_false_critical_rules():
    url = "https://www.wikipedia.org/wiki/Main_Page"
    indicators = evaluate_phishing_rules(url)
    codes = [ind["code"] for ind in indicators]
    assert "IP_BASED_HOST" not in codes
    assert "PUNYCODE_HOMOGLYPH" not in codes
    assert "AT_SYMBOL_OBFUSCATION" not in codes
