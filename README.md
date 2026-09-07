# Real-Time AI/ML-Based Phishing Detection and Prevention System (PhishShield)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%20%2F%2021%20%2F%2025-orange.svg)](https://www.oracle.com/java/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.111.0-teal.svg)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18.x-blue.svg)](https://reactjs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

An integrated, enterprise-grade public cybersecurity platform designed to detect phishing attacks in real-time, explain threats in non-technical terms, enforce SSRF protection, supply immediate incident-response checklists ("I already clicked"), and educate digital citizens via short multimedia safety guides.

---

## 🛡️ Core Innovation

### **DETECT → ANALYZE → CLASSIFY → EXPLAIN → WARN → PREVENT → EDUCATE → REPORT → LEARN**

Most existing phishing solutions are simple black-box URL classifiers. PhishShield combines:
1. **Hybrid Threat Detection**: High-dimensional ML models + deterministic rule engine + lexical heuristics.
2. **Explainable AI (XAI)**: Converts raw mathematical weights into plain-language bullet points for everyday users.
3. **Multimedia Cyber Awareness**: Dynamic, threat-tailored 30–60 second video/audio prevention modules.
4. **Multi-Input Threat Analysis**: Scan raw URLs, raw email headers/bodies, SMS/WhatsApp text, OCR screenshots, and QR codes.
5. **SSRF Guard Shield**: Military-grade server-side protection preventing malicious intranet exploitation.
6. **Incident Mitigation ("I Already Clicked")**: Step-by-step contextual containment workflow for victims.

---

## 📐 System Architecture

```text
                                  +-----------------------+
                                  |     React Web UI      |
                                  |  (Desktop / Mobile)   |
                                  +-----------+-----------+
                                              |
                                              | HTTPS / JSON
                                              v
+------------------------+        +-----------+-----------+        +------------------------+
|   Browser Extension    +------->+  Spring Boot Backend  +<-------+      MySQL 8.0 DB      |
|  (Chrome Manifest V3)  |        |  (REST API / Security)|        | (Users, Scans, Reports)|
+------------------------+        +-----------+-----------+        +------------------------+
                                              |
                                              | Internal REST
                                              v
                                  +-----------+-----------+
                                  |   Python ML Engine    |
                                  |  (FastAPI / XGBoost)  |
                                  +-----------------------+
```

---

## 📂 Monorepo Project Structure

```text
phishing-detection-system/
│
├── frontend/                     # React.js web interface
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── README.md
│
├── backend/                      # Spring Boot 3 REST API Server
│   ├── src/main/java/com/phishshield/
│   ├── src/main/resources/
│   ├── pom.xml
│   ├── Dockerfile
│   └── README.md
│
├── ml-service/                   # Python FastAPI ML microservice
│   ├── app/
│   ├── data/
│   ├── models/
│   ├── training/
│   ├── requirements.txt
│   └── README.md
│
├── browser-extension/            # Chrome Extension (Manifest V3)
│   ├── manifest.json
│   ├── src/
│   └── README.md
│
├── database/                     # Relational schemas & seed scripts
│   ├── schema.sql
│   └── seed.sql
│
├── docs/                         # In-depth architectural & security docs
│   ├── architecture.md
│   ├── api.md
│   ├── ml-methodology.md
│   ├── security.md
│   └── user-guide.md
│
├── media/                        # Multimedia awareness audio/video assets
│   ├── videos/
│   ├── audio/
│   └── thumbnails/
│
├── .env.example
├── docker-compose.yml
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (Java 21 or Java 25 recommended)
- **Maven 3.9+**
- **Node.js 18+** & npm
- **Python 3.10+**
- **Docker & Docker Compose** (Optional, for containerized deployment)

### 1. Running Backend (Spring Boot)

#### Option A: Quick Dev Mode (In-Memory H2 Database)
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- API Server: `http://localhost:8080`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON Spec: `http://localhost:8080/v3/api-docs`

#### Option B: MySQL Production Mode
1. Ensure MySQL is running on `localhost:3306` with credentials configured in `.env`.
2. Initialize database:
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```
3. Run backend:
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

#### Option C: Docker Compose (Full Stack)
```bash
docker-compose up --build
```

---

## 🔒 Default Credentials & Roles

| Role | Username | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `Admin@123456` | Full administrative oversight, threat intelligence & metrics |
| **Security Analyst** | `analyst` | `Analyst@123456` | Threat report triage, indicator tuning, model audits |
| **Regular User** | `demo_user` | `User@123456` | Personal scan history, incident reporting & safety guide |

---

## 📚 Documentation Links
- [System Architecture](docs/architecture.md)
- [REST API Specifications](docs/api.md)
- [ML Methodology & Feature Engineering](docs/ml-methodology.md)
- [Security & SSRF Mitigation Guide](docs/security.md)
- [User & Incident Response Guide](docs/user-guide.md)

---

## 📄 License
This project is licensed under the MIT License.
