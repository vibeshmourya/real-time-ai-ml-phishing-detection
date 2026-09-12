# Prahar Web Protection — Chrome Extension

**Prahar Web Protection** is a lightweight Manifest V3 browser extension that brings real-time AI and heuristic threat detection directly to your web browser.

---

## Capabilities

1. **Active Tab Inspection**: Evaluates the domain address of any web page against Prahar’s threat analysis engine.
2. **Instant Threat Warnings**: Flags deceptive look-alikes, punycode spoofing, suspicious TLDs, and known phishing patterns.
3. **In-Page Safety Warning**: Displays a non-intrusive safety banner on high-risk domains offering an immediate "Go Back to Safety" option.
4. **Manual Scanner**: Test any URL directly from the browser extension popup.
5. **Direct Integration**: Seamless connection to the local Prahar API (`http://localhost:8080`).

---

## Installation (Developer Mode)

1. Open Google Chrome (or Chromium-based browser) and navigate to `chrome://extensions`.
2. Enable **Developer mode** toggle in the top right corner.
3. Click **Load unpacked**.
4. Select the `browser-extension` directory from this repository.
5. Pin the **Prahar** shield icon to your browser toolbar.
