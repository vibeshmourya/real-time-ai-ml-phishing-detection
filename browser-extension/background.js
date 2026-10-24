/**
 * Prahar Phishing & Web Shield — Background Service Worker (Manifest V3)
 * 
 * PRIVACY & SECURITY FIRST:
 * 1. Automatic real-time protection is DISABLED by default (opt-in only).
 * 2. Never collects or transmits user browsing history for analytics.
 * 3. Never scans browser internal pages (chrome://, edge://, about:, file:).
 * 4. Excludes local network/localhost loopback addresses to prevent scanning loops.
 * 5. Uses an in-memory TTL cache to minimize redundant backend network calls.
 */

const DEFAULT_CONFIG = {
  backendUrl: 'http://localhost:8080',
  realtimeProtection: false, // Privacy default: OFF until explicitly enabled
  authToken: ''
};

// In-memory LRU-like cache for navigation scan results: domain -> { riskLevel, timestamp }
const DOMAIN_SCAN_CACHE = new Map();
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes cache

// Initialize storage with defaults if not present
chrome.runtime.onInstalled.addListener(() => {
  chrome.storage.local.get(['backendUrl', 'realtimeProtection', 'authToken'], (res) => {
    const updates = {};
    if (!res.backendUrl) updates.backendUrl = DEFAULT_CONFIG.backendUrl;
    if (res.realtimeProtection === undefined) updates.realtimeProtection = DEFAULT_CONFIG.realtimeProtection;
    if (res.authToken === undefined) updates.authToken = DEFAULT_CONFIG.authToken;

    if (Object.keys(updates).length > 0) {
      chrome.storage.local.set(updates);
    }
  });
});

/**
 * Validates whether a URL is a legitimate public web URL safe to inspect.
 */
function isPublicWebUrl(urlString) {
  if (!urlString || typeof urlString !== 'string') return false;

  try {
    const parsed = new URL(urlString);
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
      return false;
    }

    const host = parsed.hostname.toLowerCase();

    // Ignore localhost and private IP subnets
    if (
      host === 'localhost' ||
      host === '127.0.0.1' ||
      host === '::1' ||
      host.endsWith('.local') ||
      host.endsWith('.internal') ||
      host.startsWith('192.168.') ||
      host.startsWith('10.') ||
      /^172\.(1[6-9]|2[0-9]|3[0-1])\./.test(host)
    ) {
      return false;
    }

    return true;
  } catch (e) {
    return false;
  }
}

/**
 * Background tab update listener for OPTIONAL real-time navigation protection.
 */
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.status !== 'complete' || !tab.url) {
    return;
  }

  // 1. Minimum permission check: Only act on valid web schemes
  if (!isPublicWebUrl(tab.url)) {
    return;
  }

  // 2. Read consent setting: If user did NOT opt-in, DO NOT send URL
  chrome.storage.local.get(['realtimeProtection', 'backendUrl', 'authToken'], (settings) => {
    if (!settings.realtimeProtection) {
      // User has not opted into real-time scanning -> respect privacy
      return;
    }

    const backendUrl = settings.backendUrl || DEFAULT_CONFIG.backendUrl;
    const token = settings.authToken || '';
    inspectUrlSilently(tabId, tab.url, backendUrl, token);
  });
});

/**
 * Inspects a URL using the Prahar backend when real-time protection is enabled.
 */
async function inspectUrlSilently(tabId, url, backendUrl, token) {
  try {
    const parsed = new URL(url);
    const domainKey = parsed.hostname.toLowerCase();

    // Check memory cache to avoid spamming the backend
    const cached = DOMAIN_SCAN_CACHE.get(domainKey);
    const now = Date.now();
    if (cached && (now - cached.timestamp < CACHE_TTL_MS)) {
      if (cached.riskLevel === 'HIGH_RISK') {
        notifyTabOfThreat(tabId, cached.verdict);
      }
      return;
    }

    const headers = { 'Content-Type': 'application/json' };
    if (token && token.trim()) {
      headers['Authorization'] = `Bearer ${token.trim()}`;
    }

    const response = await fetch(`${backendUrl}/api/scans/url`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        url,
        includeThreatIntelConsent: false
      })
    });

    if (!response.ok) {
      return; // Backend unavailable or bad request -> fail safe, do not disrupt user
    }

    const resJson = await response.json();
    const verdict = resJson.data || resJson;

    // Cache the result
    DOMAIN_SCAN_CACHE.set(domainKey, {
      riskLevel: verdict.riskLevel,
      riskScore: verdict.riskScore,
      verdict,
      timestamp: now
    });

    // Prune cache if it grows large
    if (DOMAIN_SCAN_CACHE.size > 200) {
      const oldestKey = DOMAIN_SCAN_CACHE.keys().next().value;
      DOMAIN_SCAN_CACHE.delete(oldestKey);
    }

    // If threat detected, warn the user
    if (verdict.riskLevel === 'HIGH_RISK' || (verdict.riskScore && verdict.riskScore >= 70)) {
      notifyTabOfThreat(tabId, verdict);
    } else {
      chrome.action.setBadgeText({ tabId, text: '' }).catch(() => {});
    }
  } catch (err) {
    // Network errors or backend offline are handled gracefully without noisy alerts
  }
}

/**
 * Sends in-page warning banner and updates action badge.
 */
function notifyTabOfThreat(tabId, verdict) {
  // Update badge
  chrome.action.setBadgeText({ tabId, text: '!' }).catch(() => {});
  chrome.action.setBadgeBackgroundColor({ tabId, color: '#dc2626' }).catch(() => {});

  // Send message to content script
  chrome.tabs.sendMessage(tabId, {
    action: 'SHOW_PRAHAR_WARNING',
    verdict
  }).catch(() => {
    // Content script may not be loaded on restricted frames or fast-redirects
  });
}
