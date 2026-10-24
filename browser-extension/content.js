/**
 * Prahar Phishing & Web Shield — In-Page Warning Banner (Content Script)
 * Manifest V3 compatible, injected into web pages to display critical phishing alerts.
 */

(() => {
  // Guard against duplicate listener registrations
  if (window.__prahar_shield_injected) {
    return;
  }
  window.__prahar_shield_injected = true;

  chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message && message.action === 'SHOW_PRAHAR_WARNING') {
      displayWarningBanner(message.verdict);
      sendResponse({ status: 'BANNER_SHOWN' });
    }
    return true;
  });

  function displayWarningBanner(verdict) {
    if (document.getElementById('prahar-security-banner-host')) {
      return;
    }

    const host = document.createElement('div');
    host.id = 'prahar-security-banner-host';
    host.style.position = 'fixed';
    host.style.top = '0';
    host.style.left = '0';
    host.style.width = '100%';
    host.style.zIndex = '2147483647';
    host.style.pointerEvents = 'auto';

    // Use Shadow DOM to prevent host page CSS collisions
    const shadow = host.attachShadow({ mode: 'open' });

    const riskScore = verdict && verdict.riskScore !== undefined ? verdict.riskScore : 85;
    const threatCat = verdict && verdict.threatCategory ? verdict.threatCategory.replace(/_/g, ' ') : 'SUSPICIOUS SITE';

    const bannerHtml = `
      <style>
        .prahar-banner {
          background: linear-gradient(135deg, #991b1b 0%, #dc2626 100%);
          color: #ffffff;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
          padding: 12px 24px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.45);
          border-bottom: 2px solid #f87171;
          box-sizing: border-box;
          animation: praharSlideDown 0.3s ease-out forwards;
        }
        @keyframes praharSlideDown {
          from { transform: translateY(-100%); }
          to { transform: translateY(0); }
        }
        .prahar-left {
          display: flex;
          align-items: center;
          gap: 14px;
        }
        .prahar-icon {
          font-size: 26px;
          line-height: 1;
        }
        .prahar-alert-title {
          font-weight: 800;
          font-size: 14px;
          letter-spacing: 0.5px;
          text-transform: uppercase;
          margin-bottom: 2px;
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .prahar-score-pill {
          background: rgba(0, 0, 0, 0.35);
          border: 1px solid rgba(255, 255, 255, 0.4);
          padding: 2px 8px;
          border-radius: 999px;
          font-size: 11px;
          font-weight: 700;
        }
        .prahar-alert-desc {
          font-size: 12px;
          color: #fecaca;
          margin: 0;
          line-height: 1.4;
        }
        .prahar-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-shrink: 0;
        }
        .prahar-btn {
          border: none;
          cursor: pointer;
          font-weight: 700;
          font-size: 12px;
          padding: 8px 16px;
          border-radius: 6px;
          transition: transform 0.15s, opacity 0.15s;
        }
        .prahar-btn:active {
          transform: scale(0.97);
        }
        .prahar-btn-safety {
          background: #ffffff;
          color: #b91c1c;
          box-shadow: 0 2px 6px rgba(0,0,0,0.2);
        }
        .prahar-btn-safety:hover {
          background: #f1f5f9;
        }
        .prahar-btn-dismiss {
          background: rgba(255, 255, 255, 0.15);
          color: #ffffff;
          border: 1px solid rgba(255, 255, 255, 0.4);
        }
        .prahar-btn-dismiss:hover {
          background: rgba(255, 255, 255, 0.25);
        }
      </style>
      <div class="prahar-banner" role="alert">
        <div class="prahar-left">
          <div class="prahar-icon">🛡️</div>
          <div>
            <div class="prahar-alert-title">
              Prahar Phishing Alert
              <span class="prahar-score-pill">${riskScore}/100 Risk</span>
              <span class="prahar-score-pill">${threatCat}</span>
            </div>
            <p class="prahar-alert-desc">
              Warning: Prahar detected phishing or credential theft indicators on this page. Never enter passwords, banking details, or OTPs.
            </p>
          </div>
        </div>
        <div class="prahar-actions">
          <button id="prahar-btn-back" class="prahar-btn prahar-btn-safety">Go Back to Safety</button>
          <button id="prahar-btn-close" class="prahar-btn prahar-btn-dismiss">Dismiss</button>
        </div>
      </div>
    `;

    shadow.innerHTML = bannerHtml;
    document.documentElement.appendChild(host);

    shadow.getElementById('prahar-btn-back')?.addEventListener('click', () => {
      if (window.history.length > 1) {
        window.history.back();
      } else {
        window.location.href = 'about:blank';
      }
    });

    shadow.getElementById('prahar-btn-close')?.addEventListener('click', () => {
      host.remove();
    });
  }
})();
