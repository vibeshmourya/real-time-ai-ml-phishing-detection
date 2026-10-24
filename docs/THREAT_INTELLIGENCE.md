# Prahar 2.0 — Threat Intelligence Integration & Privacy Architecture

## 1. Overview & Vendor Evaluation

Prahar integrates external threat intelligence as a confirmatory, high-confidence signal within its multi-tier detection architecture. Rather than relying solely on third-party blacklists (which suffer from zero-hour latency), threat intelligence acts as an authoritative verification channel.

| Provider / Feed | Integration Type | API Version | Intended Role | Licensing / Usage Terms |
| :--- | :--- | :--- | :--- | :--- |
| **Google SafeBrowsing** | Native REST Client | API v4 (`threatMatches:find`) | Confirms active phishing, malware, and unwanted software campaigns. | Permitted for non-commercial and platform defense under Google Cloud API Terms. |
| **VirusTotal** | Native REST Client | API v3 (`/api/v3/urls`) | Multi-engine consensus aggregation across 70+ antivirus and threat scanners. | Permitted for public community API limits (4 req/min, 500 req/day). |
| **OpenPhish Community Feed** | Snapshot / Feed Ingestion | Public Feed | Batch training and heuristic rule tuning in Phase 25/27 data pipeline. | Permitted for research and security evaluation under OpenPhish community license. |
| **PhishTank** | Developer Snapshot | XML/CSV API | Historical phishing verification and hard-negative benchmarking. | Free for registered developer use with attribution. |

---

## 2. Privacy Architecture & Consent Gates

Querying external reputation APIs requires transmitting the target URL (or its cryptographic hash) to third-party cloud servers. To protect user privacy and corporate confidentiality:

1. **Explicit User Consent Requirement (`app.threat-intel.require-consent: true`):**
   - In accordance with privacy-by-design principles, Prahar **does not forward scanned URLs to external providers by default**.
   - If `userConsent == false`, the engine immediately halts the external check and returns:
     ```json
     {
       "status": "CONSENT_REQUIRED",
       "configured": true,
       "providerName": "GoogleSafeBrowsing",
       "details": "User consent required before submitting URL to third-party reputation database."
     }
     ```
2. **SSRF Outbound Guard (`SSRFProtectionService`):**
   - Before any HTTP request leaves the Spring Boot backend, the destination domain is resolved via DNS.
   - Any target resolving to `127.0.0.1`, `10.0.0.0/8`, `192.168.0.0/16`, `172.16.0.0/12`, or `169.254.169.254` is rejected immediately with an `InvalidUrlException`.

---

## 3. Reliability, Caching & Fault Tolerance

```text
Target URL Request
       │
       ▼
 [SSRF Protection] ──(Resolves to Private IP)──► REJECT (HTTP 400 Bad Request)
       │
 (Valid Public URL)
       ▼
 [In-Memory Cache] ──(Within 1-Hour TTL)───────► Return CachedReport (fromCache=true)
       │
 (Cache Miss / Expired)
       ▼
 [Active Provider] ──(Provider Timeout / 503)──► Return ThreatIntelReport (status="ERROR")
       │                                         (Never treat failure as proof of safety!)
 (Provider Response)
       ▼
 Cache & Return Fresh Report (status="MALICIOUS" or "CLEAN")
```

1. **TTL In-Memory Caching (`CACHE_TTL = 1 Hour`):**
   - High-traffic or frequently scanned domains are cached in a thread-safe `ConcurrentHashMap`.
   - Cached reports include `fromCache: true` and the original `checkedAt` timestamp.
2. **Strict Timeout Bounds:**
   - Network timeouts are bounded via `THREAT_INTEL_TIMEOUT_MS: 3000` (3 seconds). If the vendor fails to respond within 3 seconds, the connection is cleanly aborted.
3. **The "Clean $\neq$ Safe" Invariant:**
   - **Crucial Rule:** A `CLEAN` verdict from a threat intelligence provider merely indicates that the domain has not yet been indexed by security crawlers.
   - A `CLEAN` response **never overrides high-severity structural indicators** (such as punycode homoglyphs, brand spoofing keywords, or IP hostnames).

---

## 4. Configuration & Mock Testing

All threat intelligence credentials reside strictly in backend environment variables and are never exposed to the client or browser extension:

```yaml
app:
  threat-intel:
    enabled: ${THREAT_INTEL_ENABLED:false}
    provider: ${THREAT_INTEL_PROVIDER:google}
    google-api-key: ${THREAT_INTEL_GOOGLE_KEY:}
    virustotal-api-key: ${THREAT_INTEL_VT_KEY:}
    timeout-ms: ${THREAT_INTEL_TIMEOUT_MS:3000}
    require-consent: ${THREAT_INTEL_REQUIRE_CONSENT:true}
```

### Deterministic Unit Testing:
Automated test suites in [`ThreatIntelServiceTest.java`](../backend/src/test/java/com/phishshield/ThreatIntelServiceTest.java) use mock responses and synthetic providers to test:
- Disabled provider handling (`DISABLED`)
- Consent enforcement (`CONSENT_REQUIRED`)
- SSRF prevention blocking
- Clean vs. Malicious separation of concerns
