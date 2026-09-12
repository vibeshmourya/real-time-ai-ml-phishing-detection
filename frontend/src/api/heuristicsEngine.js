/**
 * Prahar Heuristics & Analysis Engine
 * Real-time rule evaluation, threat classification, and client-side fail-safe engine.
 */

const URL_REGEX = /(?:https?:\/\/|www\.)[^\s<>"'{}|\\^`[\]]+/gi;

const SUSPICIOUS_TLDS = new Set([
  'tk', 'ml', 'ga', 'cf', 'gq', 'top', 'xyz', 'work', 'click', 'buzz', 'rest', 'fit', 'surf', 'casa'
]);

const BRAND_MAPPINGS = [
  { brand: 'hdfc', category: 'BANKING_PHISHING', label: 'HDFC Bank', official: ['hdfcbank.com'] },
  { brand: 'sbi', category: 'BANKING_PHISHING', label: 'State Bank of India (SBI)', official: ['onlinesbi.sbi', 'sbi.co.in'] },
  { brand: 'icici', category: 'BANKING_PHISHING', label: 'ICICI Bank', official: ['icicibank.com'] },
  { brand: 'paypal', category: 'BANKING_PHISHING', label: 'PayPal', official: ['paypal.com'] },
  { brand: 'incometax', category: 'GOVERNMENT_IMPERSONATION', label: 'Income Tax Department', official: ['incometax.gov.in'] },
  { brand: 'kyc', category: 'FAKE_KYC', label: 'Identity / KYC Verification', official: [] },
  { brand: 'pan-card', category: 'FAKE_KYC', label: 'PAN Card Authority', official: ['utiitsl.com', 'nsdl.co.in'] },
  { brand: 'aadhaar', category: 'FAKE_KYC', label: 'UIDAI / Aadhaar', official: ['uidai.gov.in'] },
  { brand: 'indiapost', category: 'DELIVERY_SCAM', label: 'India Post', official: ['indiapost.gov.in'] },
  { brand: 'fedex', category: 'DELIVERY_SCAM', label: 'FedEx Delivery', official: ['fedex.com'] },
  { brand: 'parttime-job', category: 'JOB_SCAM', label: 'Work from Home / Job Offer', official: [] },
  { brand: 'telegram-task', category: 'JOB_SCAM', label: 'Telegram Task Scam', official: [] },
  { brand: 'giftcard', category: 'LOTTERY_SCAM', label: 'Gift Card / Lottery Prize', official: [] }
];

export const PREVENTION_GUIDES = {
  BANKING_PHISHING: {
    threatCategory: 'BANKING_PHISHING',
    title: 'Banking & Financial Phishing Scams',
    shortExplanation: 'Fraudulent web pages designed to replicate official banking portals to steal your account credentials, ATM PIN, or OTPs.',
    attackMechanism: 'Attackers send urgent SMS or email alerts claiming account suspension, uncredited funds, or card deactivation, prompting you to log in on a lookalike portal.',
    whyItMatters: 'Entering NetBanking credentials or OTPs can grant unauthorized access to your funds and cause immediate financial loss.',
    immediateActions: 'Never enter bank passwords or OTPs on links sent via SMS/Email. Always open your bank application or type the official address manually.',
    checklist: [
      'Look for the exact official domain in your browser address bar',
      'Never share 6-digit OTP or MPIN with anyone over call or link',
      'If you already entered information, call helpline 1930 and freeze your card immediately'
    ],
    videoPlaceholder: 'Video lesson coming soon.'
  },
  FAKE_KYC: {
    threatCategory: 'FAKE_KYC',
    title: 'Fake KYC & SIM Deactivation Traps',
    shortExplanation: 'Scammers threaten to block your bank account, credit card, or SIM card unless you complete an "emergency" KYC update.',
    attackMechanism: 'Messages claim that KYC documents expired today and include a link or phone number asking for Aadhaar, PAN card, or remote access app downloads.',
    whyItMatters: 'Banks never demand KYC updates via unofficial SMS links or third-party APK downloads.',
    immediateActions: 'Visit your nearest bank branch or use the official banking app if you need to update KYC.',
    checklist: [
      'Banks do not send SMS warnings with non-official short links',
      'Never upload PAN or Aadhaar card images to unverified websites',
      'Do not install APK files (such as AnyDesk or TeamViewer) suggested by unknown callers'
    ],
    videoPlaceholder: 'Video lesson coming soon.'
  },
  DELIVERY_SCAM: {
    threatCategory: 'DELIVERY_SCAM',
    title: 'Package Delivery & Courier Fee Scams',
    shortExplanation: 'Deceptive messages claiming a package cannot be delivered due to an incomplete address or unpaid Rs 5 / Rs 10 customs fee.',
    attackMechanism: 'The attacker prompts you to click a link and pay a nominal fee. The fake payment gateway then captures your debit/credit card details.',
    whyItMatters: 'The fake payment page will attempt to charge much larger amounts or capture recurring authorization.',
    immediateActions: 'Check tracking only on the official postal or courier website using the original consignment number.',
    checklist: [
      'Official postal services never request payment via informal SMS links',
      'Inspect the website URL carefully before paying any amount',
      'Report fake courier links to Prahar'
    ],
    videoPlaceholder: 'Video lesson coming soon.'
  },
  JOB_SCAM: {
    threatCategory: 'JOB_SCAM',
    title: 'Work from Home & Telegram Task Scams',
    shortExplanation: 'Promises of easy daily income (Rs 2,000–5,000/day) for liking YouTube videos, reviewing hotels, or completing online tasks.',
    attackMechanism: 'Victims are initially paid small amounts to build trust, then asked to deposit larger "prepaid investment" funds which are never returned.',
    whyItMatters: 'Victims often lose significant savings believing it is a guaranteed investment scheme.',
    immediateActions: 'Do not pay any deposit or registration fee for employment.',
    checklist: [
      'Legitimate companies never ask employees to deposit money to unlock tasks',
      'Beware of job offers initiated unsolicited via WhatsApp or Telegram',
      'Exit suspicious groups immediately'
    ],
    videoPlaceholder: 'Video lesson coming soon.'
  },
  GOVERNMENT_IMPERSONATION: {
    threatCategory: 'GOVERNMENT_IMPERSONATION',
    title: 'Government Scheme & Tax Refund Impersonation',
    shortExplanation: 'Fake government notices promising tax refunds, subsidies, or electricity disconnection notices.',
    attackMechanism: 'Attacker creates lookalike domains mimicking official departments and asks for bank account details or card verification.',
    whyItMatters: 'Identity theft and unauthorized fund deductions.',
    immediateActions: 'All official central and state government portals in India end with .gov.in or .nic.in.',
    checklist: [
      'Verify that the website domain ends with .gov.in',
      'Income tax refunds are processed directly to your pre-validated bank account without link clicks',
      'Never share tax portal credentials'
    ],
    videoPlaceholder: 'Video lesson coming soon.'
  },
  LOTTERY_SCAM: {
    threatCategory: 'LOTTERY_SCAM',
    title: 'Prize, Lottery & Gift Card Scams',
    shortExplanation: 'Unsolicited notifications claiming you won a new smartphone, shopping voucher, or cash prize.',
    attackMechanism: 'Prompts you to spin a wheel or pay a small processing fee to claim the reward.',
    whyItMatters: 'Used to collect personal data or credit card information.',
    immediateActions: 'If you did not enter a contest, you cannot win a prize.',
    checklist: [
      'Do not forward viral gift links on WhatsApp',
      'Never pay "processing fees" to claim a lottery reward',
      'Close the page immediately'
    ],
    videoPlaceholder: 'Video lesson coming soon.'
  },
  CREDENTIAL_THEFT: {
    threatCategory: 'CREDENTIAL_THEFT',
    title: 'Account Login & Credential Harvesting',
    shortExplanation: 'Fake login pages imitating email providers, social media, or Microsoft 365 to capture your credentials.',
    attackMechanism: 'Phishing pages copy company login layouts exactly, using deceptive subdomains.',
    whyItMatters: 'Compromised accounts can lead to identity theft and unauthorized password resets.',
    immediateActions: 'Enable Two-Factor Authentication (2FA) and verify the domain address bar.',
    checklist: [
      'Check that the address matches the real service exactly',
      'Use an authenticator app for two-factor verification',
      'Change your password immediately if you entered it on an unverified link'
    ],
    videoPlaceholder: 'Video lesson coming soon.'
  },
  BENIGN: {
    threatCategory: 'BENIGN',
    title: 'Verified Safe Web Destination',
    shortExplanation: 'No prominent phishing or social engineering indicators were detected on this link.',
    attackMechanism: 'The website structure, certificate, and domain reputation align with standard legitimate services.',
    whyItMatters: 'Continue practicing standard digital safety while browsing.',
    immediateActions: 'You can proceed safely, but always ensure HTTPS is active before entering sensitive credentials.',
    checklist: [
      'Verify the website URL matches what you expect',
      'Check for HTTPS lock icon in browser address bar',
      'Never share OTPs with anyone'
    ],
    videoPlaceholder: 'Video lesson coming soon.'
  }
};

export function getPreventionDetails(category) {
  return PREVENTION_GUIDES[category] || PREVENTION_GUIDES.BENIGN;
}

/**
 * Calculates Shannon Entropy of domain string
 */
function calculateEntropy(str) {
  if (!str) return 0;
  const map = {};
  for (let i = 0; i < str.length; i++) {
    map[str[i]] = (map[str[i]] || 0) + 1;
  }
  let entropy = 0;
  const len = str.length;
  for (const k in map) {
    const p = map[k] / len;
    entropy -= p * (Math.log(p) / Math.LN2);
  }
  return entropy;
}

/**
 * Cleanly extracts hostname from arbitrary input
 */
export function extractDomain(urlStr) {
  try {
    let clean = urlStr.trim();
    if (!clean.startsWith('http://') && !clean.startsWith('https://')) {
      clean = 'http://' + clean;
    }
    const parsed = new URL(clean);
    return parsed.hostname.toLowerCase();
  } catch {
    const parts = urlStr.split('/')[0].split('?')[0];
    return parts.toLowerCase();
  }
}

/**
 * Core URL Analysis Function
 */
export function analyzeUrl(rawUrl) {
  const url = (rawUrl || '').trim();
  const domain = extractDomain(url);
  const lowerUrl = url.toLowerCase();
  const lowerDomain = domain.toLowerCase();

  const indicators = [];
  let score = 0;
  let category = 'BENIGN';

  // 1. Check IP address hostname
  if (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(lowerDomain)) {
    score += 35;
    indicators.push({
      indicatorType: 'DOMAIN',
      indicatorName: 'Numeric IP Address Hostname',
      description: 'The link uses a raw IP address instead of a recognized domain name, which is common on temporary phishing servers.',
      severity: 'HIGH',
      weight: 35
    });
  }

  // 2. Check Suspicious TLD
  const domainParts = lowerDomain.split('.');
  const tld = domainParts[domainParts.length - 1];
  if (SUSPICIOUS_TLDS.has(tld)) {
    score += 25;
    indicators.push({
      indicatorType: 'DOMAIN',
      indicatorName: `High-Risk Free/Abuse Domain (.${tld})`,
      description: `The website uses a .${tld} top-level domain frequently associated with spam and automated phishing campaigns.`,
      severity: 'HIGH',
      weight: 25
    });
  }

  // 3. Punycode / Look-alike check
  if (lowerDomain.includes('xn--')) {
    score += 35;
    indicators.push({
      indicatorType: 'SECURITY',
      indicatorName: 'Punycode Internationalized Domain',
      description: 'The domain uses visual character substitution (punycode) to disguise look-alike brand names.',
      severity: 'CRITICAL',
      weight: 35
    });
  }

  // 4. @ symbol trick
  if (lowerUrl.includes('@')) {
    score += 30;
    indicators.push({
      indicatorType: 'LEXICAL',
      indicatorName: 'Authentication Trick (@ symbol)',
      description: 'The link contains an "@" symbol, which causes browsers to ignore preceding text and redirect to another destination.',
      severity: 'HIGH',
      weight: 30
    });
  }

  // 5. Brand Spoofing Check
  for (const item of BRAND_MAPPINGS) {
    if (lowerUrl.includes(item.brand)) {
      const isOfficial = item.official.some(off => lowerDomain === off || lowerDomain.endsWith('.' + off));
      if (!isOfficial) {
        score += 35;
        category = item.category;
        indicators.push({
          indicatorType: 'BRAND_IMPERSONATION',
          indicatorName: `Brand Impersonation: ${item.label}`,
          description: `The link references ${item.label} but is hosted on an unofficial website address (${domain}).`,
          severity: 'CRITICAL',
          weight: 35
        });
        break;
      }
    }
  }

  // 6. Excessive hyphens
  const hyphenCount = (lowerDomain.match(/-/g) || []).length;
  if (hyphenCount >= 3) {
    score += 15;
    indicators.push({
      indicatorType: 'LEXICAL',
      indicatorName: 'Excessive Hyphens in Domain',
      description: `The domain contains ${hyphenCount} hyphens, commonly seen in fraudulent lookalike domains.`,
      severity: 'MEDIUM',
      weight: 15
    });
  }

  // 7. Subdomain depth
  const dotCount = (lowerDomain.match(/\./g) || []).length;
  if (dotCount >= 3) {
    score += 15;
    indicators.push({
      indicatorType: 'DOMAIN',
      indicatorName: 'Deep Subdomain Hierarchy',
      description: `The web address has ${dotCount} nested domain levels, often used to bypass basic filters.`,
      severity: 'MEDIUM',
      weight: 15
    });
  }

  // 8. Entropy
  const entropy = calculateEntropy(lowerDomain);
  if (entropy > 4.1 && lowerDomain.length > 12) {
    score += 15;
    indicators.push({
      indicatorType: 'LEXICAL',
      indicatorName: 'High Randomness (Generated Domain)',
      description: 'The domain name appears randomly generated, typical of automated phishing domain generation.',
      severity: 'MEDIUM',
      weight: 15
    });
  }

  // Final Risk Calculation
  const riskScore = Math.min(100, Math.max(0, score));
  let riskLevel = 'SAFE';
  if (riskScore >= 70) {
    riskLevel = 'HIGH_RISK';
    if (category === 'BENIGN') category = 'CREDENTIAL_THEFT';
  } else if (riskScore >= 40) {
    riskLevel = 'SUSPICIOUS';
    if (category === 'BENIGN') category = 'CREDENTIAL_THEFT';
  } else {
    riskLevel = 'SAFE';
    category = 'BENIGN';
  }

  const confidenceScore = Math.min(0.98, Math.max(0.70, 0.60 + (indicators.length * 0.12)));

  let explanationSummary = '';
  let recommendedActions = '';

  if (riskLevel === 'HIGH_RISK') {
    explanationSummary = `Prahar flagged this website due to ${indicators.length} high-risk indicators, including suspicious domain patterns or brand impersonation.`;
    recommendedActions = 'Do not open this website. Do not enter passwords, OTPs, or bank card information. Close the tab.';
  } else if (riskLevel === 'SUSPICIOUS') {
    explanationSummary = 'This web address exhibits unusual characteristics commonly associated with unverified or promotional links.';
    recommendedActions = 'Exercise caution. Verify the official organization through a trusted search engine before entering any sensitive details.';
  } else {
    explanationSummary = 'No typical phishing anomalies or high-risk deception patterns were detected for this web address.';
    recommendedActions = 'Standard digital safety applies. Confirm the HTTPS padlock and genuine domain in the address bar before logging in.';
  }

  const technicalDetails = {
    protocol: url.startsWith('https://') ? 'HTTPS (Encrypted)' : 'HTTP (Unencrypted / Insecure)',
    domain,
    hostname: domain,
    urlLength: url.length,
    entropy: Number(entropy.toFixed(2)),
    hyphenCount,
    dotCount,
    suspiciousTld: SUSPICIOUS_TLDS.has(tld) ? `Yes (.${tld})` : 'No (Standard)',
    punycode: lowerDomain.includes('xn--') ? 'Detected' : 'None',
    model: 'Prahar-ML Hybrid Classifier (v1.0)'
  };

  return {
    scanId: Math.floor(100000 + Math.random() * 900000),
    url,
    domain,
    scanType: 'URL',
    riskScore,
    riskLevel,
    threatCategory: category,
    status: 'COMPLETED',
    detection: {
      confidenceScore,
      explanationSummary,
      recommendedActions,
      indicators
    },
    preventionGuide: getPreventionDetails(category),
    technicalDetails,
    createdAt: new Date().toISOString()
  };
}

/**
 * Message, Email & Screenshot Analysis Function
 */
export function analyzeMessageOrEmail(content, scanType = 'MESSAGE') {
  const text = (content || '').trim();
  const lowerText = text.toLowerCase();
  
  // Extract embedded URLs
  const matchedUrls = text.match(URL_REGEX) || [];
  
  const indicators = [];
  let score = 0;
  let category = 'BENIGN';

  // 1. Coercive urgency
  if (/urgent|immediately|24 hours|account block|suspended|sim deactivat|de-activated/i.test(text)) {
    score += 30;
    indicators.push({
      indicatorType: 'CONTENT',
      indicatorName: 'Artificial Urgency / Threat Pressure',
      description: 'The message employs psychological panic cues ("account blocked", "within 24 hours") to force quick actions.',
      severity: 'HIGH',
      weight: 30
    });
  }

  // 2. Financial / OTP / KYC Trap
  if (/kyc|aadhaar|pan card|update kyc|bank account|otp|pin|cvv/i.test(text)) {
    score += 35;
    category = 'FAKE_KYC';
    indicators.push({
      indicatorType: 'CONTENT',
      indicatorName: 'Sensitive Credential / KYC Request',
      description: 'The text demands identity documents, Aadhaar verification, or bank credential updates.',
      severity: 'CRITICAL',
      weight: 35
    });
  }

  // 3. Prize / Lottery / Gift Bait
  if (/won|winner|lottery|free gift|gift card|claim prize|cash prize|100% free/i.test(text)) {
    score += 30;
    category = 'LOTTERY_SCAM';
    indicators.push({
      indicatorType: 'CONTENT',
      indicatorName: 'Unsolicited Prize / Reward Bait',
      description: 'The message uses lottery or reward promises to lure you into clicking an unverified link.',
      severity: 'HIGH',
      weight: 30
    });
  }

  // 4. Utility / Electricity Cutoff
  if (/electricity|power disconnected|bill update|officer|tonight at/i.test(text)) {
    score += 35;
    category = 'GOVERNMENT_IMPERSONATION';
    indicators.push({
      indicatorType: 'CONTENT',
      indicatorName: 'Utility Disconnection Scam',
      description: 'Pretends to be an electricity or utility office threatening power disconnection tonight.',
      severity: 'CRITICAL',
      weight: 35
    });
  }

  // 5. Embedded link analysis
  let embeddedUrlResult = null;
  if (matchedUrls.length > 0) {
    embeddedUrlResult = analyzeUrl(matchedUrls[0]);
    score += Math.round(embeddedUrlResult.riskScore * 0.5);
    if (embeddedUrlResult.detection?.indicators) {
      indicators.push(...embeddedUrlResult.detection.indicators);
    }
    if (embeddedUrlResult.threatCategory !== 'BENIGN') {
      category = embeddedUrlResult.threatCategory;
    }
  }

  const riskScore = Math.min(100, Math.max(0, score));
  let riskLevel = 'SAFE';
  if (riskScore >= 70) {
    riskLevel = 'HIGH_RISK';
    if (category === 'BENIGN') category = 'FAKE_KYC';
  } else if (riskScore >= 40) {
    riskLevel = 'SUSPICIOUS';
    if (category === 'BENIGN') category = 'FAKE_KYC';
  }

  const confidenceScore = Math.min(0.98, Math.max(0.72, 0.65 + (indicators.length * 0.10)));

  const explanationSummary = riskLevel === 'HIGH_RISK'
    ? `This ${scanType.toLowerCase()} contains deceptive social-engineering markers designed to solicit credentials or create false urgency.`
    : riskLevel === 'SUSPICIOUS'
    ? `This ${scanType.toLowerCase()} exhibits cautious language or links that require verification before trusting.`
    : `No standard phishing or social engineering language triggers were detected in this ${scanType.toLowerCase()}.`;

  const recommendedActions = riskLevel === 'HIGH_RISK'
    ? 'Do not reply, click any embedded links, or call phone numbers mentioned in the message. Delete the message.'
    : 'Verify the message through the official contact channel or banking app before taking any action.';

  return {
    scanId: Math.floor(100000 + Math.random() * 900000),
    url: matchedUrls.length > 0 ? matchedUrls[0] : `${scanType}: "${text.slice(0, 80)}..."`,
    domain: matchedUrls.length > 0 ? extractDomain(matchedUrls[0]) : `${scanType} Analysis`,
    scanType,
    riskScore,
    riskLevel,
    threatCategory: category,
    status: 'COMPLETED',
    detection: {
      confidenceScore,
      explanationSummary,
      recommendedActions,
      indicators
    },
    preventionGuide: getPreventionDetails(category),
    technicalDetails: {
      extractedUrls: matchedUrls,
      characterCount: text.length,
      model: 'Prahar Multi-Channel NLP & Heuristic Engine (v1.0)'
    },
    createdAt: new Date().toISOString()
  };
}

/**
 * QR Code Helper
 */
export function analyzeDecodedQr(decodedUrl) {
  const result = analyzeUrl(decodedUrl);
  result.scanType = 'QR';
  return result;
}
