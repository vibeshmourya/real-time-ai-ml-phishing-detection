# PhishShield User Guide & Incident Playbook

## 1. Welcome to PhishShield

PhishShield is an AI-powered cybersecurity companion built to protect students, employees, senior citizens, and organizations from digital deception.

---

## 2. How to Check a Suspicious Link

1. **Copy the link** from your email, SMS, or social media chat.
2. Open **PhishShield Scanner** (`http://localhost:3000` or public deployment).
3. Paste the URL into the search bar and click **[Scan Link]**.
4. Review the real-time safety verdict:
   - 🟢 **SAFE**: Low risk. Normal legitimate domain patterns.
   - 🟡 **SUSPICIOUS**: Potential indicators detected. Proceed with extreme caution.
   - 🔴 **HIGH RISK**: Strong indicators of phishing or deception. DO NOT ENTER ANY PASSWORDS OR OTPS.

---

## 3. "I Already Clicked the Link" — Immediate Incident Playbook

If you accidentally clicked a dangerous link and entered information:

### Scenario A: Entered Password or Credentials
1. **Change Password Immediately**: Navigate directly to the official website (type the address manually into your browser).
2. **End Active Sessions**: Click "Log out of all other devices" in your account security settings.
3. **Turn on Two-Factor Authentication (2FA)**: Use an authenticator app (Google Authenticator / Authy) rather than SMS where possible.

### Scenario B: Entered Banking / Credit Card / Debit Card Details
1. **Freeze / Block Your Card**: Open your official mobile banking app and immediately freeze the compromised card.
2. **Contact Bank Fraud Hotline**: Call the 24/7 toll-free fraud helpline printed on the back of your physical card.
3. **File a National Cybercrime Report**:
   - In India: [cybercrime.gov.in](https://cybercrime.gov.in) or dial `1930`.
   - In the US: [ic3.gov](https://www.ic3.gov).

### Scenario C: Shared OTP (One-Time Password)
1. OTPs authorize immediate financial transactions or device takeovers. Call your bank hotline immediately to request transaction rollback or account freeze.

---

## 4. Cyber Awareness & Educational Guides

PhishShield features targeted educational guides across all common attack vectors:
- **Banking Phishing**: How fake banking interfaces harvest credentials.
- **Fake KYC Updates**: Fake SIM card or bank KYC expiration SMS traps.
- **Job & Lottery Scams**: Work-from-home fee demands and bogus lottery wins.
- **Delivery Scams**: Fake courier address update / parcel tracking traps.
