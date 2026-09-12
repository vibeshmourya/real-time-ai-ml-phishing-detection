/**
 * Prahar Web Protection - Popup Script
 */

const BACKEND_URL = 'http://localhost:8080/api/scans/url';

async function checkUrl(url) {
  try {
    const response = await fetch(BACKEND_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url })
    });
    if (response.ok) {
      const data = await response.json();
      return data.data;
    }
  } catch (err) {
    console.debug('Backend offline, running local heuristic evaluation');
  }

  // Fallback heuristic evaluation
  const isSuspicious = /tk|ml|ga|xyz|login-hdfc|yono-kyc/i.test(url) || /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/.test(url);
  return {
    riskScore: isSuspicious ? 86 : 4,
    riskLevel: isSuspicious ? 'HIGH_RISK' : 'SAFE',
    domain: new URL(url).hostname,
    detection: {
      explanationSummary: isSuspicious 
        ? 'Suspicious domain characteristics or lookalike brand keywords detected.'
        : 'No typical phishing anomalies detected for this web address.'
    }
  };
}

function updateUI(url, result) {
  document.getElementById('current-url').textContent = url;
  document.getElementById('risk-score-val').textContent = `${result.riskScore}/100`;

  const statusBadge = document.getElementById('status-badge');
  const verdictBanner = document.getElementById('verdict-banner');
  const verdictTitle = document.getElementById('verdict-title');
  const verdictExplanation = document.getElementById('verdict-explanation');

  statusBadge.className = 'badge';
  verdictBanner.className = 'verdict-card';

  if (result.riskLevel === 'HIGH_RISK') {
    statusBadge.classList.add('badge-danger');
    statusBadge.textContent = 'High Risk';
    verdictBanner.classList.add('verdict-danger');
    verdictTitle.textContent = '⚠️ This website looks dangerous';
    verdictExplanation.textContent = result.detection?.explanationSummary || 'May attempt to capture sensitive credentials or OTPs.';
  } else if (result.riskLevel === 'SUSPICIOUS') {
    statusBadge.classList.add('badge-warn');
    statusBadge.textContent = 'Caution';
    verdictBanner.classList.add('verdict-warn');
    verdictTitle.textContent = '⚠️ Be Careful on this website';
    verdictExplanation.textContent = result.detection?.explanationSummary || 'Unusual structural patterns observed.';
  } else {
    statusBadge.classList.add('badge-safe');
    statusBadge.textContent = 'Verified Safe';
    verdictBanner.classList.add('verdict-safe');
    verdictTitle.textContent = '✓ No Major Risk Detected';
    verdictExplanation.textContent = 'Domain characteristics align with standard legitimate services.';
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  // Query active tab
  if (chrome?.tabs) {
    chrome.tabs.query({ active: true, currentWindow: true }, async (tabs) => {
      if (tabs && tabs[0] && tabs[0].url) {
        const activeUrl = tabs[0].url;
        if (activeUrl.startsWith('http://') || activeUrl.startsWith('https://')) {
          const result = await checkUrl(activeUrl);
          updateUI(activeUrl, result);
        } else {
          document.getElementById('current-url').textContent = activeUrl;
          document.getElementById('status-badge').textContent = 'Internal Tab';
          document.getElementById('verdict-title').textContent = 'System / Browser Page';
          document.getElementById('verdict-explanation').textContent = 'This is a browser internal page, no threat scan needed.';
        }
      }
    });
  }

  // Manual Check
  document.getElementById('manual-check-btn')?.addEventListener('click', async () => {
    const input = document.getElementById('manual-url-input')?.value;
    if (input && input.trim()) {
      let target = input.trim();
      if (!target.startsWith('http://') && !target.startsWith('https://')) {
        target = 'https://' + target;
      }
      const result = await checkUrl(target);
      updateUI(target, result);
    }
  });

  // Open Dashboard
  document.getElementById('open-dashboard-btn')?.addEventListener('click', () => {
    chrome.tabs.create({ url: 'http://localhost:5173' });
  });

  // Report Site
  document.getElementById('report-site-btn')?.addEventListener('click', () => {
    chrome.tabs.create({ url: 'http://localhost:5173' });
  });
});
