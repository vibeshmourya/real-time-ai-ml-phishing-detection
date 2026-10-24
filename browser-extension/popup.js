/**
 * Prahar Phishing & Web Shield — Popup Controller (Manifest V3)
 * Handles user-initiated active tab scanning, manual URL checks, settings sync, and honest reporting.
 */

document.addEventListener('DOMContentLoaded', () => {
  // --- DOM Elements ---
  const statusIndicator = document.getElementById('backend-status-indicator');
  const toggleSettingsBtn = document.getElementById('toggle-settings-btn');
  const closeSettingsBtn = document.getElementById('close-settings-btn');
  const settingsPanel = document.getElementById('settings-panel');
  const backendUrlInput = document.getElementById('backend-url-input');
  const authTokenInput = document.getElementById('auth-token-input');
  const realtimeCb = document.getElementById('realtime-protection-checkbox');
  const saveSettingsBtn = document.getElementById('save-settings-btn');

  const tabSchemeBadge = document.getElementById('tab-scheme-badge');
  const tabHostname = document.getElementById('tab-hostname');
  const tabFullUrl = document.getElementById('tab-full-url');
  const scanTabBtn = document.getElementById('scan-tab-btn');
  const scanSpinner = scanTabBtn ? scanTabBtn.querySelector('.btn-spinner') : null;
  const scanText = scanTabBtn ? scanTabBtn.querySelector('.btn-text') : null;

  const unsupportedCard = document.getElementById('unsupported-scheme-card');
  const errorBanner = document.getElementById('error-banner');
  const errorTitle = document.getElementById('error-title');
  const errorDesc = document.getElementById('error-desc');

  const resultsCard = document.getElementById('results-card');
  const verdictBanner = document.getElementById('verdict-banner');
  const verdictBadge = document.getElementById('verdict-badge');
  const verdictTitle = document.getElementById('verdict-title');
  const riskScoreVal = document.getElementById('risk-score-val');
  const gaugeBar = document.getElementById('gauge-bar');
  const threatCategoryVal = document.getElementById('threat-category-val');
  const modelAttributionVal = document.getElementById('model-attribution-val');
  const explanationSummary = document.getElementById('explanation-summary');
  const indicatorsSection = document.getElementById('indicators-section');
  const indicatorsList = document.getElementById('indicators-list');
  const recommendationText = document.getElementById('recommendation-text');

  const manualUrlInput = document.getElementById('manual-url-input');
  const manualScanBtn = document.getElementById('manual-scan-btn');
  const openDashboardBtn = document.getElementById('open-dashboard-btn');
  const openSafetyBtn = document.getElementById('open-safety-btn');

  // --- State ---
  let backendUrl = 'http://localhost:8080';
  let authToken = '';
  let realtimeProtection = false;
  let currentTabUrl = '';
  let isScanning = false;

  // --- 1. Load Settings from Storage ---
  if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
    chrome.storage.local.get(['backendUrl', 'authToken', 'realtimeProtection'], (items) => {
      if (items.backendUrl) backendUrl = items.backendUrl.replace(/\/+$/, '');
      if (items.authToken) authToken = items.authToken;
      if (items.realtimeProtection !== undefined) realtimeProtection = Boolean(items.realtimeProtection);

      if (backendUrlInput) backendUrlInput.value = backendUrl;
      if (authTokenInput) authTokenInput.value = authToken;
      if (realtimeCb) realtimeCb.checked = realtimeProtection;

      checkBackendHealth();
    });
  } else {
    checkBackendHealth();
  }

  // --- 2. Check Backend Health ---
  async function checkBackendHealth() {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2500);

      const resp = await fetch(`${backendUrl}/api/health`, {
        method: 'GET',
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (resp.ok) {
        if (statusIndicator) {
          statusIndicator.className = 'status-dot online';
          statusIndicator.title = `Backend Connected: ${backendUrl}`;
        }
      } else {
        markBackendOffline(`Backend returned status ${resp.status}`);
      }
    } catch (e) {
      markBackendOffline(`Cannot reach Prahar backend at ${backendUrl}`);
    }
  }

  function markBackendOffline(reason) {
    if (statusIndicator) {
      statusIndicator.className = 'status-dot offline';
      statusIndicator.title = reason || 'Backend Offline';
    }
  }

  // --- 3. Detect and Parse Active Browser Tab ---
  function inspectActiveTab() {
    if (typeof chrome !== 'undefined' && chrome.tabs && chrome.tabs.query) {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (!tabs || tabs.length === 0) {
          showUnsupported('No active browser tab found');
          return;
        }

        const tab = tabs[0];
        currentTabUrl = tab.url || '';
        evaluateTabUrl(currentTabUrl);
      });
    } else {
      // Standalone / Test preview mock
      evaluateTabUrl('https://demo.prahar-shield.local/login-verify');
    }
  }

  function evaluateTabUrl(rawUrl) {
    if (!rawUrl || typeof rawUrl !== 'string') {
      showUnsupported('Empty or invalid URL');
      return;
    }

    const unsupportedSchemes = ['chrome:', 'chrome-extension:', 'edge:', 'about:', 'data:', 'file:', 'view-source:', 'brave:'];
    const lower = rawUrl.toLowerCase();
    const isUnsupported = unsupportedSchemes.some(s => lower.startsWith(s));

    if (isUnsupported) {
      showUnsupported(rawUrl);
      return;
    }

    try {
      const parsed = new URL(rawUrl);
      if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
        showUnsupported(rawUrl);
        return;
      }

      // Valid HTTP/HTTPS web page
      if (unsupportedCard) unsupportedCard.classList.add('hidden');
      if (scanTabBtn) scanTabBtn.disabled = false;

      const schemeText = parsed.protocol.replace(':', '').toUpperCase();
      if (tabSchemeBadge) {
        tabSchemeBadge.textContent = schemeText;
        tabSchemeBadge.className = schemeText === 'HTTPS' ? 'scheme-badge' : 'scheme-badge warn';
      }

      if (tabHostname) {
        tabHostname.textContent = parsed.hostname;
      }

      if (tabFullUrl) {
        const displayUrl = rawUrl.length > 58 ? rawUrl.substring(0, 55) + '...' : rawUrl;
        tabFullUrl.textContent = displayUrl;
        tabFullUrl.title = rawUrl;
      }
    } catch (e) {
      showUnsupported(rawUrl);
    }
  }

  function showUnsupported(schemeInfo) {
    if (unsupportedCard) unsupportedCard.classList.remove('hidden');
    if (scanTabBtn) scanTabBtn.disabled = true;

    if (tabSchemeBadge) {
      tabSchemeBadge.textContent = 'SYSTEM';
      tabSchemeBadge.className = 'scheme-badge';
    }

    if (tabHostname) {
      tabHostname.textContent = 'Browser Internal Page';
    }

    if (tabFullUrl) {
      tabFullUrl.textContent = schemeInfo || 'Internal page cannot be scanned';
      tabFullUrl.title = schemeInfo || '';
    }
  }

  // --- 4. User-Initiated Scan Action ---
  async function performScan(targetUrl) {
    if (isScanning || !targetUrl) return;

    // Validate URL scheme before submission
    let cleanUrl = targetUrl.trim();
    if (!cleanUrl.startsWith('http://') && !cleanUrl.startsWith('https://')) {
      cleanUrl = 'https://' + cleanUrl;
    }

    try {
      new URL(cleanUrl);
    } catch (e) {
      showError('Invalid URL Format', 'Please enter a valid, well-formed web URL.');
      return;
    }

    // Enter loading state
    setScanningState(true);
    hideError();
    if (resultsCard) resultsCard.classList.add('hidden');

    try {
      const headers = {
        'Content-Type': 'application/json'
      };
      if (authToken && authToken.trim()) {
        headers['Authorization'] = `Bearer ${authToken.trim()}`;
      }

      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 12000); // 12s timeout

      const response = await fetch(`${backendUrl}/api/scans/url`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          url: cleanUrl,
          includeThreatIntelConsent: false
        }),
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        let errMessage = `Server returned HTTP ${response.status}`;
        try {
          const errJson = await response.json();
          if (errJson && errJson.message) errMessage = errJson.message;
        } catch (_) {}
        showError('Scan Failed', errMessage);
        markBackendOffline();
        return;
      }

      const resJson = await response.json();
      const scanData = resJson.data || resJson;

      // Real results obtained -> render UI honestly
      renderScanResults(scanData, cleanUrl);
      if (statusIndicator) {
        statusIndicator.className = 'status-dot online';
      }
    } catch (err) {
      if (err.name === 'AbortError') {
        showError('Request Timeout', `Scan request to ${backendUrl} timed out after 12 seconds.`);
      } else {
        showError(
          'Backend Unavailable',
          `Cannot reach Prahar backend at ${backendUrl}. Ensure the Spring Boot server is running on port 8080.`
        );
        markBackendOffline();
      }
    } finally {
      setScanningState(false);
    }
  }

  // --- 5. Render Scan Results (Honest, Evidence-Based) ---
  function renderScanResults(data, scannedUrl) {
    if (!resultsCard) return;

    const score = (typeof data.riskScore === 'number') ? Math.min(100, Math.max(0, Math.round(data.riskScore))) : 0;
    const level = data.riskLevel || (score >= 70 ? 'HIGH_RISK' : (score >= 35 ? 'SUSPICIOUS' : 'SAFE'));
    const category = data.threatCategory || 'BENIGN';

    // Update Score
    if (riskScoreVal) {
      riskScoreVal.textContent = score;
      riskScoreVal.style.color = score >= 70 ? '#ef4444' : (score >= 35 ? '#f59e0b' : '#10b981');
    }

    // Update Gauge Bar
    if (gaugeBar) {
      gaugeBar.style.width = `${score}%`;
      gaugeBar.className = 'gauge-bar ' + (score >= 70 ? 'danger' : (score >= 35 ? 'warn' : 'safe'));
    }

    // Update Verdict Banner
    if (verdictBanner) {
      verdictBanner.className = 'verdict-banner ' + (score >= 70 ? 'danger' : (score >= 35 ? 'warn' : 'safe'));
    }

    if (verdictBadge) {
      if (level === 'HIGH_RISK' || score >= 70) {
        verdictBadge.textContent = 'HIGH RISK PHISHING';
        verdictBadge.className = 'verdict-badge danger';
      } else if (level === 'SUSPICIOUS' || score >= 35) {
        verdictBadge.textContent = 'SUSPICIOUS';
        verdictBadge.className = 'verdict-badge warn';
      } else {
        verdictBadge.textContent = 'LEGITIMATE / SAFE';
        verdictBadge.className = 'verdict-badge safe';
      }
    }

    if (verdictTitle) {
      if (level === 'HIGH_RISK' || score >= 70) {
        verdictTitle.textContent = 'Critical Phishing Indicators Detected';
      } else if (level === 'SUSPICIOUS' || score >= 35) {
        verdictTitle.textContent = 'Potential Security Risks Identified';
      } else {
        verdictTitle.textContent = 'Domain Appears Legitimate';
      }
    }

    // Threat Category
    if (threatCategoryVal) {
      threatCategoryVal.textContent = category.replace(/_/g, ' ');
    }

    // Model Attribution (Distinguish AI/ML vs Heuristics)
    if (modelAttributionVal) {
      if (data.modelPrediction && data.modelPrediction.isMlAvailable) {
        const mName = data.modelPrediction.modelName || 'Supervised Classifier';
        const prob = typeof data.modelPrediction.phishingProbability === 'number'
          ? ` (${Math.round(data.modelPrediction.phishingProbability * 100)}% prob)`
          : '';
        modelAttributionVal.textContent = `AI/ML: ${mName}${prob}`;
      } else {
        modelAttributionVal.textContent = 'Heuristic Security Rule Engine';
      }
    }

    // Explanation Summary
    if (explanationSummary) {
      let explanation = '';
      if (data.modelPrediction && Array.isArray(data.modelPrediction.explanations) && data.modelPrediction.explanations.length > 0) {
        explanation = data.modelPrediction.explanations.join(' ');
      } else if (data.detection && data.detection.explanationSummary) {
        explanation = data.detection.explanationSummary;
      } else if (score >= 70) {
        explanation = 'This URL exhibits strong patterns commonly associated with credential harvesting and fraudulent sites.';
      } else if (score >= 35) {
        explanation = 'Some unusual patterns or suspicious lexical properties were detected on this domain.';
      } else {
        explanation = 'Domain characteristics align with standard legitimate traffic and established reputation benchmarks.';
      }
      explanationSummary.textContent = explanation;
    }

    // Detected Risk Indicators
    if (indicatorsSection && indicatorsList) {
      indicatorsList.innerHTML = '';
      const rawIndicators = (data.detection && Array.isArray(data.detection.indicators)) ? data.detection.indicators : [];

      if (rawIndicators.length > 0) {
        rawIndicators.forEach((ind) => {
          const li = document.createElement('li');
          li.className = 'indicator-item';

          const name = typeof ind === 'string' ? ind : (ind.indicatorName || ind.ruleTriggered || 'Flagged Feature');
          const desc = ind.description ? ` — ${ind.description}` : '';
          const severity = ind.severity || 'HIGH';

          li.innerHTML = `
            <span class="indicator-dot ${severity.toLowerCase()}"></span>
            <span class="indicator-text"><strong>${escapeHtml(name)}</strong>${escapeHtml(desc)}</span>
          `;
          indicatorsList.appendChild(li);
        });
        indicatorsSection.classList.remove('hidden');
      } else {
        indicatorsSection.classList.add('hidden');
      }
    }

    // Recommended Action
    if (recommendationText) {
      let rec = '';
      if (data.preventionGuide && data.preventionGuide.immediateActions) {
        rec = data.preventionGuide.immediateActions;
      } else if (data.detection && data.detection.recommendedActions) {
        rec = data.detection.recommendedActions;
      } else if (score >= 70) {
        rec = 'Do not enter passwords, credit card numbers, or OTP credentials on this site. Navigate away immediately.';
      } else if (score >= 35) {
        rec = 'Verify the authenticity of this URL through independent channels before submitting personal details.';
      } else {
        rec = 'No threat detected. Continue browsing normally with standard security awareness.';
      }
      recommendationText.textContent = rec;
    }

    // Unhide Results Card
    resultsCard.classList.remove('hidden');
  }

  // --- 6. Helper Functions ---
  function setScanningState(scanning) {
    isScanning = scanning;
    if (scanTabBtn) {
      scanTabBtn.disabled = scanning;
      if (scanSpinner) scanSpinner.classList.toggle('hidden', !scanning);
      if (scanText) scanText.textContent = scanning ? 'Scanning URL...' : 'Scan This Page';
    }
    if (manualScanBtn) {
      manualScanBtn.disabled = scanning;
    }
  }

  function showError(title, message) {
    if (errorBanner) {
      if (errorTitle) errorTitle.textContent = title;
      if (errorDesc) errorDesc.textContent = message;
      errorBanner.classList.remove('hidden');
    }
  }

  function hideError() {
    if (errorBanner) {
      errorBanner.classList.add('hidden');
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // --- 7. Event Listeners ---
  if (scanTabBtn) {
    scanTabBtn.addEventListener('click', () => {
      if (currentTabUrl) {
        performScan(currentTabUrl);
      }
    });
  }

  if (manualScanBtn && manualUrlInput) {
    manualScanBtn.addEventListener('click', () => {
      const url = manualUrlInput.value.trim();
      if (url) performScan(url);
    });

    manualUrlInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const url = manualUrlInput.value.trim();
        if (url) performScan(url);
      }
    });
  }

  if (toggleSettingsBtn && settingsPanel) {
    toggleSettingsBtn.addEventListener('click', () => {
      settingsPanel.classList.toggle('hidden');
    });
  }

  if (closeSettingsBtn && settingsPanel) {
    closeSettingsBtn.addEventListener('click', () => {
      settingsPanel.classList.add('hidden');
    });
  }

  if (saveSettingsBtn) {
    saveSettingsBtn.addEventListener('click', () => {
      const newUrl = backendUrlInput ? backendUrlInput.value.trim() : 'http://localhost:8080';
      const newToken = authTokenInput ? authTokenInput.value.trim() : '';
      const newRealtime = realtimeCb ? realtimeCb.checked : false;

      backendUrl = newUrl.replace(/\/+$/, '') || 'http://localhost:8080';
      authToken = newToken;
      realtimeProtection = newRealtime;

      if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
        chrome.storage.local.set({
          backendUrl,
          authToken,
          realtimeProtection
        }, () => {
          saveSettingsBtn.textContent = 'Settings Saved ✓';
          setTimeout(() => {
            saveSettingsBtn.textContent = 'Save Settings';
            if (settingsPanel) settingsPanel.classList.add('hidden');
          }, 900);
          checkBackendHealth();
        });
      } else {
        if (settingsPanel) settingsPanel.classList.add('hidden');
      }
    });
  }

  if (openDashboardBtn) {
    openDashboardBtn.addEventListener('click', () => {
      const target = 'http://localhost:5173';
      if (typeof chrome !== 'undefined' && chrome.tabs && chrome.tabs.create) {
        chrome.tabs.create({ url: target });
      } else {
        window.open(target, '_blank');
      }
    });
  }

  if (openSafetyBtn) {
    openSafetyBtn.addEventListener('click', () => {
      const target = 'http://localhost:5173/safety';
      if (typeof chrome !== 'undefined' && chrome.tabs && chrome.tabs.create) {
        chrome.tabs.create({ url: target });
      } else {
        window.open(target, '_blank');
      }
    });
  }

  // Initial trigger
  inspectActiveTab();
});
