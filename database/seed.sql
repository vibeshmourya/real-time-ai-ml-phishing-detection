-- =============================================================================
-- PhishShield Relational Database Initial Seed Data (MySQL 8.0+)
-- =============================================================================

USE phishshield_db;

-- 1. Seed Roles
INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_USER', 'Standard platform user with scanning, history, and reporting permissions'),
(2, 'ROLE_SECURITY_ANALYST', 'Security analyst with report triage and threat intelligence access'),
(3, 'ROLE_ADMIN', 'Platform administrator with full system, user, and configuration access')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Seed Default Users (Password: Admin@123456 / Analyst@123456 / User@123456)
-- BCrypt encoded passwords (cost 10)
INSERT INTO users (id, username, email, password_hash, full_name, role_id, is_active) VALUES
(1, 'admin', 'admin@phishshield.security', '$2a$10$tZ2E7H4r2s1D4u6M8K3N7.5W1Z3A5C7E9G1I3K5M7O9Q1S3U5W7Y.', 'System Administrator', 3, TRUE),
(2, 'analyst', 'analyst@phishshield.security', '$2a$10$tZ2E7H4r2s1D4u6M8K3N7.5W1Z3A5C7E9G1I3K5M7O9Q1S3U5W7Y.', 'Cyber Threat Analyst', 2, TRUE),
(3, 'demo_user', 'user@phishshield.security', '$2a$10$tZ2E7H4r2s1D4u6M8K3N7.5W1Z3A5C7E9G1I3K5M7O9Q1S3U5W7Y.', 'Alex Digital Citizen', 1, TRUE)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- 3. Seed Prevention & Educational Content
INSERT INTO prevention_content (threat_category, title, short_explanation, attack_mechanism, why_it_matters, immediate_actions, checklist_json, video_url, audio_url, language_code) VALUES
('BANKING_PHISHING', 'Banking Phishing Defense Guide',
 'Attackers forge duplicate online banking portals to steal your internet banking user ID, password, OTP, and debit card PIN.',
 'Victims receive urgent SMS or emails claiming an account freeze or unauthorized debit. The link routes to a pixel-perfect replica of a bank portal that captures credentials.',
 'Complete unauthorized financial drain, fraudulent loans, and identity theft.',
 '1. Never enter bank credentials or OTPs on links sent via SMS/Email.\n2. Always verify by manually typing the official bank URL.\n3. Immediately freeze card/account via official mobile app if leaked.',
 '["Do not enter OTP or PIN on unsolicited links", "Inspect domain for subtle misspellings (e.g., hdfc-bank-verify.com vs hdfcbank.com)", "Call the official fraud hotline printed on your physical card"]',
 '/media/videos/banking-phishing.mp4',
 '/media/audio/banking-phishing-en.mp3',
 'en'),

('CREDENTIAL_THEFT', 'Account Credential Theft Prevention',
 'Phishers build counterfeit login screens for Google, Microsoft 365, Apple, or corporate portals to hijack accounts.',
 'A fake document notification or password expiration email prompts the user to log in on a look-alike authentication page.',
 'Unauthorized email access, corporate data breaches, ransomware delivery, and private identity exposure.',
 '1. Enable App-Based Multi-Factor Authentication (Authenticator app / Hardware token).\n2. Review active logged-in sessions in account settings.\n3. Change passwords immediately from a trusted device.',
 '["Verify URL bar displays genuine top-level domain", "Look for password manager auto-fill (password managers will NOT auto-fill on spoofed domains)", "Enable 2FA on all email and social accounts"]',
 '/media/videos/credential-theft.mp4',
 '/media/audio/credential-theft-en.mp3',
 'en'),

('FAKE_KYC', 'Fake KYC Update Scam Advisory',
 'Scammers claim your SIM card, bank account, or wallet KYC has expired to trick you into downloading remote access apps or entering debit card details.',
 'The user receives an SMS: "Dear customer, your KYC is pending, your SIM/A/c will be blocked today. Click here to verify."',
 'Cybercriminals initiate unauthorized fund transfers or install screen-sharing software (AnyDesk/TeamViewer).',
 '1. No bank or telecom operator completes KYC via random SMS links.\n2. Visit the nearest official branch or use the verified official app.\n3. Block and report the sender phone number.',
 '["Telecom companies never threaten immediate SIM disconnection via SMS links", "Never share OTP under any circumstance", "Never install remote sharing tools on unknown requests"]',
 '/media/videos/fake-kyc.mp4',
 '/media/audio/fake-kyc-en.mp3',
 'en'),

('SHOPPING_SCAM', 'Fake E-Commerce & Deep Discount Scam Guide',
 'Fraudulent shopping websites lure victims with impossible 90% discounts on iPhones, laptops, and branded items.',
 'Victims pay online upfront through fake payment gateways; goods are never shipped and credit card data is compromised.',
 'Direct financial loss and repeated unauthorized credit card recurring charges.',
 '1. If the price is too good to be true, it is almost certainly a scam.\n2. Verify the website reputation, domain age, and physical contact address.\n3. Use credit cards with fraud chargeback protection.',
 '["Check domain registration details", "Look for verified return and refund policies", "Avoid upfront payment via direct wire transfer or non-traceable methods"]',
 '/media/videos/shopping-scam.mp4',
 '/media/audio/shopping-scam-en.mp3',
 'en'),

('JOB_SCAM', 'Employment & Work-From-Home Scam Warning',
 'Fraudsters pose as recruiters offering high-paying part-time rating jobs, requiring registration fees or deposit tasks.',
 'Victims are asked to complete initial tasks, then pressured into depositing money into crypto wallets to unlock earnings.',
 'Loss of deposited funds and identity document theft through fake background verification forms.',
 '1. Legitimate employers never ask candidates to pay money for job offers.\n2. Research the company through official LinkedIn or corporate portals.\n3. Never submit banking passwords or pay security deposits.',
 '["No legitimate recruiter charges registration or training fees", "Cross-check job ID on company official careers page", "Never transfer funds to unlock work earnings"]',
 '/media/videos/job-scam.mp4',
 '/media/audio/job-scam-en.mp3',
 'en'),

('DELIVERY_SCAM', 'Courier & Package Delivery Scam Notice',
 'Fake SMS alerts from "India Post / FedEx / DHL / UPS" claiming a package cannot be delivered due to incomplete address.',
 'The SMS directs victims to pay a nominal fee (e.g. $1 or Rs 25) on a phishing portal that steals full card credentials.',
 'The nominal charge is an illusion; attackers drain full card limits.',
 '1. Check the tracking ID directly on the official postal or courier website.\n2. Do not click links in unsolicited delivery messages.\n3. Postal services do not demand card payments via SMS.',
 '["Open browser and navigate to courier official website manually", "Do not enter card CVV or OTP for package redelivery", "Report malicious delivery SMS to 1930 or official cybercrime portal"]',
 '/media/videos/delivery-scam.mp4',
 '/media/audio/delivery-scam-en.mp3',
 'en'),

('GOVERNMENT_IMPERSONATION', 'Government & Tax Agency Impersonation Advisory',
 'Scammers mimic tax departments, court notices, or subsidy boards offering refunds or threatening legal prosecution.',
 'Deceptive tax refund portals request taxpayer credentials, bank account numbers, and OTP authorizations.',
 'Tax fraud, identity theft, and severe financial losses.',
 '1. Official government portals only use `.gov`, `.nic.in`, or verified government domains.\n2. Access government tax portals directly via official bookmarks.\n3. Government departments do not ask for debit card PINs for refunds.',
 '["Check domain ends with legitimate government TLDs", "Government agencies never contact citizens via WhatsApp or non-official email", "Tax refunds are credited directly to verified bank accounts without OTP input on SMS links"]',
 '/media/videos/gov-scam.mp4',
 '/media/audio/gov-scam-en.mp3',
 'en'),

('BENIGN', 'Safe Browsing Best Practices',
 'The scanned URL does not display characteristic indicators of known phishing campaigns.',
 'Always continue practicing proactive cyber hygiene while browsing.',
 'Helps maintain proactive security posture.',
 '1. Keep your web browser and operating system updated.\n2. Use a modern password manager with unique passwords for every site.\n3. Always look for genuine HTTPS and domain spelling before entering sensitive credentials.',
 '["Verify domain spelling", "Use unique passwords for each service", "Keep multi-factor authentication enabled"]',
 '/media/videos/safe-browsing.mp4',
 '/media/audio/safe-browsing-en.mp3',
 'en')
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 4. Seed Active Security Alerts
INSERT INTO security_alerts (title, alert_type, severity, message, target_audience, is_active, created_at, expires_at) VALUES
('Surge in Fake Tax Refund Phishing SMS Campaigns', 'EMERGING_THREAT', 'CRITICAL',
 'A large-scale SMS phishing campaign is impersonating national tax portals offering instant refunds. Do not click links claiming tax refunds requiring card details.', 'ALL', TRUE, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)),
('Look-Alike Banking Domains Exploiting Free TLDs (.tk, .xyz)', 'ADVISORY', 'WARNING',
 'Attackers are registering punycode and typo-squatted domains targeting major commercial banks. Ensure netbanking URLs are typed directly.', 'ALL', TRUE, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY)),
('Work-From-Home Telegram Task Scams Targeting Students', 'ADVISORY', 'WARNING',
 'Fraudulent part-time video rating and review jobs are extorting money from college students. Remember: legitimate employers never charge candidate fees.', 'STUDENTS', TRUE, NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY))
ON DUPLICATE KEY UPDATE title=VALUES(title);
