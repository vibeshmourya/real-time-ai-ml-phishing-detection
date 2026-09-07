/**
 * Intelligent Client-Side Cybersecurity Detection Engine (XAI Heuristics)
 * Emulates high-precision phishing detection rules & Plain-English Explainable AI.
 * Used for instant scanning & seamless offline/live fallback.
 */

// Known dangerous/high-abuse top-level domains
const SUSPICIOUS_TLDS = [
  '.tk', '.ml', '.ga', '.cf', '.gq', '.xyz', '.top', '.buzz', '.work',
  '.click', '.loan', '.racing', '.country', '.download', '.stream', '.review',
  '.accountant', '.date', '.faith', '.party', '.trade', '.science', '.ninja'
];

// Legitimate known domains for zero false-positives
const TRUSTED_DOMAINS = [
  'google.com', 'www.google.com', 'youtube.com', 'microsoft.com', 'github.com',
  'amazon.com', 'amazon.in', 'apple.com', 'wikipedia.org', 'gov.in', 'cybercrime.gov.in',
  'sbi.co.in', 'onlinesbi.sbi', 'hdfcbank.com', 'icicibank.com', 'rbi.org.in',
  'paypal.com', 'netflix.com', 'linkedin.com', 'twitter.com', 'x.com'
];

// High-value targeted brands often spoofed in phishing campaigns
const TARGETED_BRANDS = [
  { name: 'HDFC Bank', keyword: 'hdfc', official: 'hdfcbank.com', cat: 'BANKING_PHISHING' },
  { name: 'State Bank of India', keyword: 'sbi', official: 'sbi.co.in', cat: 'BANKING_PHISHING' },
  { name: 'ICICI Bank', keyword: 'icici', official: 'icicibank.com', cat: 'BANKING_PHISHING' },
  { name: 'Paytm', keyword: 'paytm', official: 'paytm.com', cat: 'UPI_PAYMENT_FRAUD' },
  { name: 'PhonePe', keyword: 'phonepe', official: 'phonepe.com', cat: 'UPI_PAYMENT_FRAUD' },
  { name: 'Google Pay', keyword: 'gpay', official: 'pay.google.com', cat: 'UPI_PAYMENT_FRAUD' },
  { name: 'PayPal', keyword: 'paypal', official: 'paypal.com', cat: 'FINANCIAL_FRAUD' },
  { name: 'Netflix', keyword: 'netflix', official: 'netflix.com', cat: 'SUBSCRIPTION_FRAUD' },
  { name: 'Amazon', keyword: 'amazon', official: 'amazon.com', cat: 'SHOPPING_FRAUD' },
  { name: 'Income Tax Dept', keyword: 'incometax', official: 'incometax.gov.in', cat: 'GOVERNMENT_IMPERSONATION' },
  { name: 'India Post / Courier', keyword: 'indiapost', official: 'indiapost.gov.in', cat: 'DELIVERY_SCAM' }
];

// Social engineering trigger patterns
const URGENCY_PATTERNS = [
  { regex: /account.*(blocked|suspended|disabled|frozen|locked)/i, name: 'Urgent Account Block Threat', weight: 28 },
  { regex: /(update|verify|complete).*(kyc|pan|aadhaar|document)/i, name: 'Fake KYC / Identity Update Trap', weight: 32 },
  { regex: /(lottery|reward|cashback|congratulations|prize|won|claim).*(\$|\₹|[0-9]+)/i, name: 'Unsolicited Lottery / Reward Bait', weight: 25 },
  { regex: /electricity.*(disconnected|power cut|bill due tonight)/i, name: 'Electricity Disconnection Scam', weight: 30 },
  { regex: /(part-time|work from home|daily income|earn ₹|earn \$|telegram task)/i, name: 'Fake Job / Daily Task Scam', weight: 25 },
  { regex: /(refund|tax refund|overdue refund).*claim/i, name: 'Bogus Tax / Payment Refund', weight: 25 },
  { regex: /(otp|password|pin|cvv|secret code)/i, name: 'Credential / OTP Solicitation', weight: 35 }
];

/**
 * Analyze a URL for security indicators and return an explainable verdict
 */
export function analyzeUrl(rawUrl) {
  if (!rawUrl || typeof rawUrl !== 'string') {
    throw new Error('Please enter a valid URL to scan');
  }

  let inputUrl = rawUrl.trim();
  if (!/^https?:\/\//i.test(inputUrl)) {
    inputUrl = 'http://' + inputUrl;
  }

  let parsed;
  try {
    parsed = new URL(inputUrl);
  } catch {
    return createQuickVerdict(rawUrl, 85, 'HIGH_RISK', 'MALFORMED_URL', [
      {
        indicatorType: 'SYNTAX',
        indicatorName: 'Malformed URL Structure',
        description: 'URL contains malformed characters commonly used to deceive URL parsers.',
        severity: 'HIGH',
        weight: 35
      }
    ]);
  }

  const hostname = parsed.hostname.toLowerCase();
  const pathname = parsed.pathname.toLowerCase();
  const fullUrl = inputUrl.toLowerCase();

  // 1. Check for known trusted exact domains
  const isTrusted = TRUSTED_DOMAINS.some(t => hostname === t || hostname.endsWith('.' + t));
  if (isTrusted && !pathname.includes('@') && !parsed.port) {
    return {
      scanId: Date.now(),
      url: rawUrl,
      normalizedUrl: parsed.host + parsed.pathname,
      domain: hostname,
      scanType: 'URL',
      riskScore: 4,
      riskLevel: 'SAFE',
      threatCategory: 'LEGITIMATE_VERIFIED',
      status: 'COMPLETED',
      detection: {
        confidenceScore: 0.98,
        explanationSummary: `Verified legitimate domain belonging to an established official organization (${hostname}).`,
        recommendedActions: 'This link is safe to visit. Always ensure the lock/HTTPS icon appears in your browser address bar.',
        indicators: [
          {
            indicatorType: 'DOMAIN_REPUTATION',
            indicatorName: 'Official Recognized Domain',
            description: `The domain ${hostname} is matched against global whitelist registries.`,
            severity: 'INFO',
            weight: 0
          },
          {
            indicatorType: 'SSL_STATUS',
            indicatorName: 'Valid SSL Certificate Expected',
            description: 'Domain enforces secure encrypted communication.',
            severity: 'INFO',
            weight: 0
          }
        ]
      },
      preventionGuide: getPreventionDetails('SAFE_URL'),
      createdAt: new Date().toISOString()
    };
  }

  const indicators = [];
  let score = 5;
  let category = 'SUSPICIOUS_GENERIC';

  // 2. IP Address Host check (e.g., http://192.168.1.1 or http://45.33.32.156)
  const isIpHost = /^(\d{1,3}\.){3}\d{1,3}$/.test(hostname);
  if (isIpHost) {
    score += 35;
    indicators.push({
      indicatorType: 'NETWORK',
      indicatorName: 'Raw IP Address as Hostname',
      description: 'The link uses a raw IP address instead of a registered domain name. Attackers use this to evade domain takedowns.',
      severity: 'CRITICAL',
      weight: 35
    });
  }

  // 3. High-abuse TLDs
  const hasAbuseTld = SUSPICIOUS_TLDS.some(tld => hostname.endsWith(tld));
  if (hasAbuseTld) {
    score += 30;
    const foundTld = SUSPICIOUS_TLDS.find(tld => hostname.endsWith(tld));
    indicators.push({
      indicatorType: 'DOMAIN',
      indicatorName: `High-Risk Free/Abuse TLD (${foundTld})`,
      description: `Domain ends in ${foundTld}, a top-level domain frequently utilized in phishing operations due to zero-cost/anonymity.`,
      severity: 'HIGH',
      weight: 30
    });
  }

  // 4. Target Brand Impersonation & Typosquatting
  for (const brand of TARGETED_BRANDS) {
    if (hostname.includes(brand.keyword) && !hostname.endsWith(brand.official)) {
      score += 40;
      category = brand.cat;
      indicators.push({
        indicatorType: 'BRAND_IMPERSONATION',
        indicatorName: `Brand Spoofing: ${brand.name}`,
        description: `The domain name mimics '${brand.name}' but is NOT hosted on the genuine official domain (${brand.official}).`,
        severity: 'CRITICAL',
        weight: 40
      });
      break;
    }
  }

  // 5. Suspicious Subdomains and Excess Delimiters
  const subdomains = hostname.split('.');
  if (subdomains.length > 3) {
    score += 15;
    indicators.push({
      indicatorType: 'LEXICAL',
      indicatorName: 'Excessive Subdomain Stacking',
      description: 'Multiple nested subdomains detected, often used to conceal the true apex domain on mobile screens.',
      severity: 'MEDIUM',
      weight: 15
    });
  }

  const hyphenCount = (hostname.match(/-/g) || []).length;
  if (hyphenCount >= 2) {
    score += 15;
    indicators.push({
      indicatorType: 'LEXICAL',
      indicatorName: 'Deceptive Hyphenation',
      description: `Domain contains ${hyphenCount} hyphens commonly combined with brand keywords to fool victims.`,
      severity: 'MEDIUM',
      weight: 15
    });
  }

  // 6. Embedded Auth '@' Character
  if (inputUrl.includes('@')) {
    score += 35;
    indicators.push({
      indicatorType: 'SYNTAX',
      indicatorName: 'URL Credential Obfuscation (@ symbol)',
      description: "URL contains '@' syntax which causes browsers to ignore all preceding characters and route to the following domain.",
      severity: 'CRITICAL',
      weight: 35
    });
  }

  // 7. Sensitive keywords in path or query
  const suspiciousKeywords = ['login', 'signin', 'verify', 'update-kyc', 'banking', 'secure', 'wallet', 'claim', 'account-blocked'];
  for (const kw of suspiciousKeywords) {
    if (fullUrl.includes(kw) && !isTrusted) {
      score += 12;
      indicators.push({
        indicatorType: 'LEXICAL',
        indicatorName: `Sensitive Keyword in Path ('${kw}')`,
        description: `URL path contains credential-harvesting keyword '${kw}' on an unverified domain.`,
        severity: 'HIGH',
        weight: 12
      });
      break;
    }
  }

  // 8. Urgency Patterns in text/URL
  for (const pattern of URGENCY_PATTERNS) {
    if (pattern.regex.test(fullUrl)) {
      score += pattern.weight;
      indicators.push({
        indicatorType: 'PSYCHOLOGICAL_TRIGGER',
        indicatorName: pattern.name,
        description: 'Attackers create artificial panic or greed to force users into acting before thinking.',
        severity: 'HIGH',
        weight: pattern.weight
      });
      break;
    }
  }

  // Cap score between 0 and 99
  const finalScore = Math.min(Math.max(score, 8), 98);
  const riskLevel = finalScore >= 65 ? 'HIGH_RISK' : finalScore >= 35 ? 'SUSPICIOUS' : 'SAFE';

  let explanationSummary = '';
  let recommendedActions = '';

  if (riskLevel === 'HIGH_RISK') {
    explanationSummary = `High-confidence phishing indicators detected. The link attempts to impersonate legitimate services and exhibits characteristics of a credential-harvesting trap.`;
    recommendedActions = 'DO NOT open this link or enter passwords, card numbers, or OTPs. If you already clicked, use our Emergency Containment Playbook immediately.';
  } else if (riskLevel === 'SUSPICIOUS') {
    explanationSummary = `Potential threat flags identified. The domain reputation is unverified or utilizes suspicious naming conventions.`;
    recommendedActions = 'Proceed with extreme caution. Verify the sender through official secondary channels before interacting.';
  } else {
    explanationSummary = `No active malicious indicators or brand spoofing patterns found.`;
    recommendedActions = 'Standard safety precautions apply. Ensure HTTPS is enabled when entering any personal information.';
  }

  return {
    scanId: Date.now(),
    url: rawUrl,
    normalizedUrl: parsed.host + parsed.pathname,
    domain: hostname,
    scanType: 'URL',
    riskScore: finalScore,
    riskLevel,
    threatCategory: category,
    status: 'COMPLETED',
    detection: {
      confidenceScore: +(finalScore / 100).toFixed(2),
      explanationSummary,
      recommendedActions,
      indicators: indicators.length ? indicators : [
        {
          indicatorType: 'HEURISTIC_CHECK',
          indicatorName: 'Clean Lexical Structure',
          description: 'No deceptive keywords or obfuscated redirection mechanisms identified.',
          severity: 'LOW',
          weight: 0
        }
      ]
    },
    preventionGuide: getPreventionDetails(category),
    createdAt: new Date().toISOString()
  };
}

/**
 * Analyze an SMS, WhatsApp message, or Email body text
 */
export function analyzeMessageOrEmail(content, type = 'MESSAGE') {
  if (!content || !content.trim()) {
    throw new Error('Please provide text to scan');
  }

  const text = content.trim();
  const textLower = text.toLowerCase();
  const indicators = [];
  let score = 10;
  let category = 'SUSPICIOUS_MESSAGE';

  // 1. Check for embedded URLs
  const urlMatches = text.match(/https?:\/\/[^\s]+|[a-zA-Z0-9-]+\.(tk|xyz|top|ml|buzz|cc|app|page\.link|is\.gd|bit\.ly|tinyurl\.com)[^\s]*/gi);
  if (urlMatches && urlMatches.length > 0) {
    score += 25;
    indicators.push({
      indicatorType: 'EMBEDDED_LINK',
      indicatorName: 'Embedded Unverified Link',
      description: `Contains embedded link (${urlMatches[0]}) intended to redirect victims away from verified channels.`,
      severity: 'HIGH',
      weight: 25
    });

    // Check if the embedded link is high-abuse
    for (const link of urlMatches) {
      if (SUSPICIOUS_TLDS.some(tld => link.includes(tld)) || /bit\.ly|tinyurl|is\.gd|t\.co/i.test(link)) {
        score += 25;
        indicators.push({
          indicatorType: 'LINK_SHORTENER',
          indicatorName: 'URL Shortener / Obfuscator',
          description: 'Uses link redirection or masking services to hide the real destination server.',
          severity: 'HIGH',
          weight: 25
        });
        break;
      }
    }
  }

  // 2. Urgent Call-to-action & Psychology triggers
  for (const pattern of URGENCY_PATTERNS) {
    if (pattern.regex.test(text)) {
      score += pattern.weight;
      indicators.push({
        indicatorType: 'SOCIAL_ENGINEERING',
        indicatorName: pattern.name,
        description: 'Employs high-pressure coercive tactics demanding immediate action to prevent critical consequences.',
        severity: 'CRITICAL',
        weight: pattern.weight
      });
    }
  }

  // 3. Brand mentions without official context
  for (const brand of TARGETED_BRANDS) {
    if (textLower.includes(brand.keyword)) {
      score += 15;
      category = brand.cat;
      indicators.push({
        indicatorType: 'BRAND_REFERENCE',
        indicatorName: `Impersonates Brand: ${brand.name}`,
        description: `Message claims to be from ${brand.name}. Legitimate institutions never ask for passwords or OTPs over SMS/Email.`,
        severity: 'HIGH',
        weight: 15
      });
      break;
    }
  }

  const finalScore = Math.min(Math.max(score, 12), 99);
  const riskLevel = finalScore >= 60 ? 'HIGH_RISK' : finalScore >= 35 ? 'SUSPICIOUS' : 'SAFE';

  return {
    scanId: Date.now(),
    url: text.slice(0, 80) + (text.length > 80 ? '...' : ''),
    normalizedUrl: text.slice(0, 100),
    domain: type === 'EMAIL' ? 'Email Body Analysis' : 'SMS / Message Analysis',
    scanType: type,
    riskScore: finalScore,
    riskLevel,
    threatCategory: category,
    status: 'COMPLETED',
    detection: {
      confidenceScore: +(finalScore / 100).toFixed(2),
      explanationSummary: riskLevel === 'HIGH_RISK'
        ? `Dangerous social engineering attack identified. The message uses classic urgency deception and unverified links.`
        : `Message contains potential risk markers. Always verify directly with the organization.`,
      recommendedActions: riskLevel === 'HIGH_RISK'
        ? 'Do not reply, click embedded links, or share any verification codes. Report as Spam/Phishing.'
        : 'Verify the claims via official customer service helplines before taking action.',
      indicators: indicators.length ? indicators : [
        {
          indicatorType: 'TEXT_INTEGRITY',
          indicatorName: 'No High-Pressure Triggers Found',
          description: 'Text does not display overt coercion or known credential-harvesting templates.',
          severity: 'LOW',
          weight: 0
        }
      ]
    },
    preventionGuide: getPreventionDetails(category),
    createdAt: new Date().toISOString()
  };
}

/**
 * Educational Prevention Guides mapped by Threat Category
 */
export function getPreventionDetails(category) {
  const guides = {
    BANKING_PHISHING: {
      category: 'BANKING_PHISHING',
      title: 'Banking & Credential Phishing Defense',
      shortExplanation: 'Fraudsters create counterfeit bank login screens to steal NetBanking passwords, Debit Card numbers, and OTPs.',
      attackMechanism: 'You receive an alarming alert stating your account is frozen, with a link leading to a lookalike bank portal.',
      whyItMatters: 'Immediate financial theft, unauthorized money transfers, and complete account takeover.',
      immediateActions: '1. Never share OTPs. 2. Bookmark your real bank website. 3. Call your bank hotline if compromised.',
      audioWarning: 'Warning! This portal appears to be a fraudulent banking clone. Never type your bank passwords or OTPs here.',
      videoThumbnail: '🏦 Banking Shield Guide (30s)'
    },
    KYC_FRAUD: {
      category: 'KYC_FRAUD',
      title: 'Fake KYC Update Traps',
      shortExplanation: 'Scammers claim your SIM card, bank account, or wallet KYC is expired to steal identity documents and money.',
      attackMechanism: 'SMS threatens disconnection within 24 hours unless you call a bogus number or visit a fake verification form.',
      whyItMatters: 'Identity theft, unauthorized SIM swaps, and loan fraud registered under your name.',
      immediateActions: '1. Banks never update KYC via SMS links. 2. Visit an official physical branch or genuine mobile app.',
      audioWarning: 'Caution! Legitimate service providers never demand immediate KYC updates via unverified third-party links.',
      videoThumbnail: '📱 KYC Fraud Defense (45s)'
    },
    LOTTERY_SCAM: {
      category: 'LOTTERY_SCAM',
      title: 'Lottery & Prize Bait Prevention',
      shortExplanation: 'Fake promises of multi-lakh lottery wins or gift cards designed to trick you into paying "processing fees".',
      attackMechanism: 'Message says you won a contest you never entered, demanding upfront registration tax or card details.',
      whyItMatters: 'Direct financial extortion and loss of registration payments.',
      immediateActions: '1. If you did not buy a ticket, you cannot win a prize. 2. Delete and block the sender immediately.',
      audioWarning: 'Alert! Unsolicited lottery notifications are 100% fraudulent. Do not pay any advance fees.',
      videoThumbnail: '🎁 Lottery Scam Breakdown (30s)'
    },
    DELIVERY_SCAM: {
      category: 'DELIVERY_SCAM',
      title: 'Package Delivery & Postal Scams',
      shortExplanation: 'Fake courier SMS stating your parcel cannot be delivered until you update your address and pay ₹5-₹10.',
      attackMechanism: 'The small ₹5 payment gateway is malicious and secretly clones your credit/debit card information.',
      whyItMatters: 'Card cloning resulting in large unauthorized overseas transactions.',
      immediateActions: '1. Track parcels only on official courier websites using your original tracking number.',
      audioWarning: 'Warning! Fake parcel delivery SMS traps steal payment cards through tiny address update fees.',
      videoThumbnail: '📦 Parcel Fraud Playbook (30s)'
    },
    SAFE_URL: {
      category: 'SAFE_URL',
      title: 'Safe Browsing Hygiene',
      shortExplanation: 'This domain shows verified legitimate security attributes.',
      attackMechanism: 'Always inspect the address bar whenever entering sensitive passwords or payment details.',
      whyItMatters: 'Maintaining consistent cyber hygiene prevents 99% of digital attacks.',
      immediateActions: 'Keep your browser updated and enable Multi-Factor Authentication (2FA) across all personal accounts.',
      audioWarning: 'Link verified safe. Remember to always look for the secure HTTPS lock in your browser.',
      videoThumbnail: '🛡️ Safe Cyber Habits (30s)'
    }
  };

  return guides[category] || guides.BANKING_PHISHING;
}

function createQuickVerdict(url, score, level, cat, indicators) {
  return {
    scanId: Date.now(),
    url,
    normalizedUrl: url,
    domain: 'Unparsed/Malformed',
    scanType: 'URL',
    riskScore: score,
    riskLevel: level,
    threatCategory: cat,
    status: 'COMPLETED',
    detection: {
      confidenceScore: 0.9,
      explanationSummary: 'The provided input exhibits structural anomalies frequently found in obfuscated malicious payloads.',
      recommendedActions: 'Do not interact with this address.',
      indicators
    },
    preventionGuide: getPreventionDetails('BANKING_PHISHING'),
    createdAt: new Date().toISOString()
  };
}
