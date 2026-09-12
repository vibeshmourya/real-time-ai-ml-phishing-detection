/**
 * Prahar Web Protection - In-Page Content Script Warning
 */

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'SHOW_PRAHAR_WARNING') {
    displayWarningBanner(message.verdict);
  }
});

function displayWarningBanner(verdict) {
  if (document.getElementById('prahar-security-banner')) {
    return;
  }

  const banner = document.createElement('div');
  banner.id = 'prahar-security-banner';
  banner.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    z-index: 2147483647;
    background: #dc2626;
    color: #ffffff;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    padding: 12px 20px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.3);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    font-size: 13px;
  `;

  banner.innerHTML = `
    <div style="display: flex; align-items: center; gap: 12px;">
      <span style="font-size: 24px;">🛡️</span>
      <div>
        <strong style="font-size: 14px; display: block;">⚠️ Prahar Security Alert: HIGH RISK WEBSITE</strong>
        <span>This website may be trying to steal your personal or banking credentials. Do not enter passwords or OTPs.</span>
      </div>
    </div>
    <div style="display: flex; align-items: center; gap: 10px;">
      <button id="prahar-back-btn" style="
        background: #ffffff;
        color: #dc2626;
        border: none;
        padding: 8px 14px;
        font-weight: 700;
        font-size: 12px;
        border-radius: 6px;
        cursor: pointer;
      ">Go Back to Safety</button>
      <button id="prahar-dismiss-btn" style="
        background: transparent;
        color: #ffffff;
        border: 1px solid rgba(255,255,255,0.6);
        padding: 8px 12px;
        font-size: 12px;
        border-radius: 6px;
        cursor: pointer;
      ">Dismiss Warning</button>
    </div>
  `;

  document.body.prepend(banner);

  document.getElementById('prahar-back-btn')?.addEventListener('click', () => {
    window.history.back();
  });

  document.getElementById('prahar-dismiss-btn')?.addEventListener('click', () => {
    banner.remove();
  });
}
