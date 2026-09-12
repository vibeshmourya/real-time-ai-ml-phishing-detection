/**
 * Prahar Web Protection - Service Worker (Background Script)
 */

const API_ENDPOINT = 'http://localhost:8080/api/scans/url';

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.status === 'complete' && tab.url) {
    if (tab.url.startsWith('http://') || tab.url.startsWith('https://')) {
      inspectTabUrl(tabId, tab.url);
    }
  }
});

async function inspectTabUrl(tabId, url) {
  // Quick exclusion for localhost / dashboard
  if (url.includes('localhost') || url.includes('127.0.0.1')) {
    return;
  }

  try {
    const response = await fetch(API_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url })
    });

    if (response.ok) {
      const data = await response.json();
      const verdict = data.data;
      if (verdict && verdict.riskLevel === 'HIGH_RISK') {
        chrome.tabs.sendMessage(tabId, {
          action: 'SHOW_PRAHAR_WARNING',
          verdict: verdict
        }).catch(() => {
          // Content script might not be injected on some restricted pages
        });
      }
    }
  } catch (err) {
    // Silent fail if backend is not running
  }
}
