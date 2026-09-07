# Security Architecture & SSRF Mitigation Guide

## 1. Security Architecture Principles

PhishShield is engineered with **Defense-in-Depth** and **Zero-Trust Input Validation** at every layer:

1. **Principle of Least Privilege**: Granular role-based authorization (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_SECURITY_ANALYST`).
2. **Stateless JWT Authentication**: Cryptographically signed tokens (HMAC-SHA256/512) with short lifespans and strict claim validation.
3. **No Sensitive Data Persistence**: Plaintext passwords, OTPs, PINs, or raw email archives are never stored in databases or logs.
4. **Complete SSRF Protection Shield**: Server-Side Request Forgery prevention on all URL scanning endpoints.

---

## 2. Server-Side Request Forgery (SSRF) Defense Shield

Because a cybersecurity scanner accepts arbitrary URLs from untrusted public users, it must NEVER be weaponized into an internal network proxy or cloud metadata exfiltration tool.

### 2.1 Enforced Mitigation Rules in `SsrfProtectionService`:
- **Protocol Whitelisting**: Only `http` and `https` schemes are accepted. Any `file://`, `gopher://`, `dict://`, `ftp://`, `ldap://`, or `php://` schemes trigger immediate termination.
- **Authority Credentials Stripping**: URLs containing embedded user info (e.g. `http://admin:secret@malicious.com`) are rejected.
- **DNS Resolution Inspection**:
  - Hostnames are resolved to canonical IP addresses prior to processing.
  - The resolved IP is inspected against prohibited ranges:
    - **Loopback**: `127.0.0.0/8`, `::1`
    - **RFC 1918 Private**: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`
    - **Link-Local & Cloud Metadata**: `169.254.0.0/16` (blocks AWS/GCP/Azure instance metadata service), `fe80::/10`
    - **Multicast / Broadcast / Reserved**: `224.0.0.0/4`, `240.0.0.0/4`, `0.0.0.0/8`
    - **IPv6 Mapped IPv4 evasions**: `::ffff:127.0.0.1` etc.
- **Port Restrictions**: Non-standard server probing is prohibited (only standard web ports 80, 443, 8080 allowed for external checks).

---

## 3. SQL Injection & XSS Protection
- **Spring Data JPA & Prepared Statements**: All database operations use parameterized queries.
- **Input Sanitization & DTO Validation**: `@Valid`, `@NotBlank`, `@Size`, `@Pattern` enforce boundary checks.
- **Security Response Headers**: Content Security Policy (CSP), X-Content-Type-Options: `nosniff`, X-Frame-Options: `DENY`, Strict-Transport-Security (HSTS).

---

## 4. Audit Logging Standard

Every security-relevant operation is recorded in the `audit_logs` table:
- User login / logout / failed authentication attempts
- URL scans submitted
- User threat reports filed
- Admin status updates & advisory broadcasts
- *Strict Rule: Passwords, OTPs, and JWT tokens are sanitized and never written to audit tables or log streams.*
