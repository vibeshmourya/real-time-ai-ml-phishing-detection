# REST API Specification

This document defines the REST API contract for the **PhishShield Platform**.
Base URL: `http://localhost:8080/api`

Interactive Swagger documentation is available at:
`http://localhost:8080/swagger-ui/index.html`

---

## 1. Authentication & Authorization

### 1.1 Register User
- **Endpoint**: `POST /api/auth/register`
- **Access**: Public
- **Request Body**:
```json
{
  "username": "johndoe",
  "email": "johndoe@example.com",
  "password": "SecurePassword123!",
  "fullName": "John Doe"
}
```
- **Response** `201 Created`:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "johndoe",
      "email": "johndoe@example.com",
      "fullName": "John Doe",
      "role": "ROLE_USER"
    }
  }
}
```

### 1.2 Login User
- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
```json
{
  "usernameOrEmail": "johndoe",
  "password": "SecurePassword123!"
}
```
- **Response** `200 OK`:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "johndoe",
      "email": "johndoe@example.com",
      "fullName": "John Doe",
      "role": "ROLE_USER"
    }
  }
}
```

### 1.3 Get Current Profile
- **Endpoint**: `GET /api/auth/me`
- **Access**: Authenticated (`Bearer <JWT>`)
- **Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "johndoe",
    "email": "johndoe@example.com",
    "fullName": "John Doe",
    "role": "ROLE_USER",
    "createdAt": "2026-09-02T10:00:00Z"
  }
}
```

---

## 2. Threat Scanning Engine

### 2.1 Scan URL
- **Endpoint**: `POST /api/scans/url`
- **Access**: Public (Authenticated users get scans saved to history automatically)
- **Request Body**:
```json
{
  "url": "http://secure-login-hdfcbank.account-verify.tk/login.php"
}
```
- **Response** `200 OK`:
```json
{
  "success": true,
  "message": "Scan completed successfully",
  "data": {
    "scanId": 42,
    "url": "http://secure-login-hdfcbank.account-verify.tk/login.php",
    "normalizedUrl": "secure-login-hdfcbank.account-verify.tk/login.php",
    "domain": "secure-login-hdfcbank.account-verify.tk",
    "scanType": "URL",
    "riskScore": 92,
    "riskLevel": "HIGH_RISK",
    "threatCategory": "BANKING_PHISHING",
    "status": "COMPLETED",
    "detection": {
      "confidenceScore": 0.94,
      "explanationSummary": "Potential high-risk banking phishing attempt detected impersonating HDFC Bank.",
      "recommendedActions": "Do not enter account credentials, OTPs, or passwords. Close this page immediately.",
      "indicators": [
        {
          "indicatorType": "DOMAIN",
          "indicatorName": "Suspicious Free TLD",
          "description": "Domain uses a high-abuse top-level domain (.tk).",
          "severity": "HIGH",
          "weight": 25.0
        },
        {
          "indicatorType": "LEXICAL",
          "indicatorName": "Brand Impersonation Keyword",
          "description": "The URL contains banking keywords ('hdfcbank', 'secure-login', 'verify') in a non-official domain.",
          "severity": "CRITICAL",
          "weight": 35.0
        }
      ]
    },
    "preventionGuide": {
      "category": "BANKING_PHISHING",
      "title": "Banking Phishing Defense Guide",
      "shortExplanation": "Attackers construct duplicate banking login portals to capture your netbanking passwords and OTPs.",
      "attackMechanism": "The victim receives an urgent warning claiming account suspension and is directed to an imposter site.",
      "whyItMatters": "Full compromise of financial accounts, unauthorized fund transfers, and identity theft.",
      "immediateActions": "1. Never enter OTPs. 2. Navigate directly to the bank's official URL. 3. Call bank fraud helpline.",
      "videoUrl": "/media/videos/banking-phishing-guide.mp4",
      "audioUrl": "/media/audio/banking-warning-en.mp3"
    },
    "createdAt": "2026-09-02T10:15:00Z"
  }
}
```

### 2.2 Scan Email Body
- **Endpoint**: `POST /api/scans/email`
- **Access**: Public
- **Request Body**:
```json
{
  "emailContent": "URGENT: Your bank account will be blocked within 24 hours. Update your KYC now at http://update-kyc-alert.xyz to avoid suspension."
}
```

### 2.3 Scan Message (SMS / WhatsApp)
- **Endpoint**: `POST /api/scans/message`
- **Access**: Public
- **Request Body**:
```json
{
  "messageContent": "Congratulations! You won a $500 gift card. Claim here: http://free-gift-reward.top/claim"
}
```

### 2.4 Get Scan by ID
- **Endpoint**: `GET /api/scans/{id}`
- **Access**: Public

### 2.5 Get User Scan History
- **Endpoint**: `GET /api/scans/my-scans?page=0&size=10`
- **Access**: Authenticated

### 2.6 Delete Scan Record (Privacy by Design)
- **Endpoint**: `DELETE /api/scans/{id}`
- **Access**: Authenticated (Owner only)

---

## 3. Prevention & Cyber Awareness

### 3.1 Get Prevention Guide by Category
- **Endpoint**: `GET /api/prevention/{category}?lang=en`
- **Access**: Public

### 3.2 List All Prevention Categories
- **Endpoint**: `GET /api/prevention/categories`
- **Access**: Public

---

## 4. Threat Reporting & Crowd Intelligence

### 4.1 Submit Suspicious URL Report
- **Endpoint**: `POST /api/reports`
- **Access**: Public
- **Request Body**:
```json
{
  "url": "http://fake-income-tax-refund.org/claim",
  "threatCategory": "GOVERNMENT_IMPERSONATION",
  "description": "Received SMS claiming tax refund of Rs 15,000 requiring card details.",
  "reporterEmail": "citizen@example.com"
}
```

### 4.2 Get User Reports
- **Endpoint**: `GET /api/reports/my-reports`
- **Access**: Authenticated

### 4.3 Admin Report Management
- **Endpoint**: `GET /api/reports?status=PENDING&page=0&size=20`
- **Endpoint**: `PATCH /api/reports/{id}/status`
- **Access**: `ROLE_ADMIN` or `ROLE_SECURITY_ANALYST`

---

## 5. Security Advisories & Alerts

### 5.1 Get Active Advisories
- **Endpoint**: `GET /api/alerts/active`
- **Access**: Public

### 5.2 Create Advisory
- **Endpoint**: `POST /api/alerts`
- **Access**: `ROLE_ADMIN`

---

## 6. Dashboards & Analytics

### 6.1 User Dashboard Metrics
- **Endpoint**: `GET /api/dashboard/user`
- **Access**: Authenticated

### 6.2 Admin Global Threat Analytics
- **Endpoint**: `GET /api/dashboard/admin`
- **Access**: `ROLE_ADMIN` or `ROLE_SECURITY_ANALYST`
