# Prahar Spring Boot Backend

Production-ready backend API service for the Real-Time AI/ML-Based Phishing Detection and Prevention System.

## Features
- **Stateless JWT Security**: Role-based access control (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_SECURITY_ANALYST`).
- **SSRF Shield**: Strict outbound URI validation, loopback/private IP blocking, and port constraints.
- **Hybrid Threat Analysis**: Lexical, structural, and domain heuristic rule engine with plain-language explanation synthesis.
- **Multi-Input Scanning**: Endpoints for URL, Email, and SMS/Message content.
- **Incident Response & Awareness**: Contextual prevention content, checklists, and video/audio mapping.
- **Crowdsourced Threat Reporting**: Public report submission with admin triage workflow.
- **Interactive OpenAPI Documentation**: Swagger UI at `/swagger-ui/index.html`.

## Configuration & Profiles
- `dev`: In-memory H2 database with automatic schema generation and seed data for quick local testing.
- `mysql`: Production profile connecting to MySQL 8.0 instance using environment variables.
- `test`: Automated test profile using in-memory H2 and MockMvc.

## Running the Application
```bash
# In-memory development mode (no external MySQL needed)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# MySQL mode (requires running MySQL server)
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
