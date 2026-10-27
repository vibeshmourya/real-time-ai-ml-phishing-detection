import client from './client';

/**
 * Custom error class for Prahar Chatbot service failures
 */
export class ChatServiceError extends Error {
  constructor(type, message, status = null, diagnostics = null) {
    super(message);
    this.name = 'ChatServiceError';
    this.type = type; // 'TIMEOUT' | 'BACKEND_UNAVAILABLE' | 'SERVER_ERROR' | 'HTTP_ERROR' | 'INVALID_RESPONSE'
    this.status = status;
    this.diagnostics = diagnostics;
  }
}

export const chatApi = {
  /**
   * Send a question or message to Prahar AI Chat Assistant.
   * Preserves backend as primary. Never silently masks backend failures.
   *
   * @param {Object} payload
   * @param {string} payload.message
   * @param {number} [payload.scanId]
   * @param {string} [payload.scanUrl]
   * @param {number} [payload.scanRiskScore]
   * @param {string} [payload.scanRiskLevel]
   * @param {string} [payload.scanThreatCategory]
   * @param {string} [payload.scanExplanation]
   * @param {Array} [payload.conversationHistory]
   * @returns {Promise<Object>} Backend ChatResponse DTO
   */
  async sendMessage(payload) {
    const startTime = Date.now();
    try {
      const response = await client.post('/chat', payload, {
        timeout: 10000
      });

      if (response?.data?.data && typeof response.data.data.reply === 'string') {
        if (import.meta.env.DEV) {
          console.debug('[Prahar Chat] Backend responded in', Date.now() - startTime, 'ms', {
            intent: response.data.data.intent,
            provider: response.data.data.provider,
            isLiveAi: response.data.data.isLiveAi
          });
        }
        return {
          ...response.data.data,
          isOfflineFallback: false
        };
      }

      throw new ChatServiceError(
        'INVALID_RESPONSE',
        'Backend returned an unexpected response envelope without reply data.',
        response?.status
      );
    } catch (err) {
      const durationMs = Date.now() - startTime;
      let statusCode = err.response?.status || null;

      // AUTOMATIC GRACEFUL DEGRADATION:
      // If the backend is not yet deployed (404) or network is unreachable,
      // seamlessly return verified deterministic offline safety guidance so the user is never blocked.
      if (!err.response || statusCode === 404) {
        console.warn('[Prahar Chat] Backend not yet reachable or returned 404; activating offline emergency guidance.', {
          status: statusCode,
          endpoint: '/api/chat',
          durationMs
        });
        const failureReason = statusCode === 404
          ? 'Backend endpoint not yet deployed. Showing verified offline safety rules.'
          : 'Backend server currently unreachable. Showing verified offline safety rules.';
        return this.getOfflineEmergencyGuidance(payload, { message: failureReason, statusCode });
      }

      let errorType = 'HTTP_ERROR';
      let userFriendlyMessage = 'Failed to communicate with Prahar Assistant.';

      if (err instanceof ChatServiceError) {
        throw err;
      }

      if (err.code === 'ECONNABORTED' || err.message?.toLowerCase().includes('timeout')) {
        errorType = 'TIMEOUT';
        userFriendlyMessage = 'The request to Prahar Assistant timed out (10s). The server or AI provider may be overloaded.';
      } else if (statusCode === 401 || statusCode === 403) {
        errorType = 'AUTH_ERROR';
        userFriendlyMessage = 'Authorization check failed for this chat session.';
      } else if (statusCode >= 500) {
        errorType = 'SERVER_ERROR';
        const serverDetail = err.response?.data?.message || 'Internal server error';
        userFriendlyMessage = `Prahar backend encountered an error (HTTP ${statusCode}): ${serverDetail}`;
      } else {
        userFriendlyMessage = err.response?.data?.message || err.message || userFriendlyMessage;
      }

      const diagnostics = {
        endpoint: '/api/chat',
        durationMs,
        statusCode,
        errorType,
        timestamp: new Date().toISOString()
      };

      if (import.meta.env.DEV) {
        console.warn('[Prahar Chat Diagnostics]', diagnostics, 'Message length:', payload?.message?.length || 0);
      }

      throw new ChatServiceError(errorType, userFriendlyMessage, statusCode, diagnostics);
    }
  },

  /**
   * Health & AI Provider Status Check
   * Calls GET /api/chat/status safely without exposing secrets
   */
  async getServiceStatus() {
    try {
      const response = await client.get('/chat/status', { timeout: 4000 });
      return {
        isOnline: true,
        ...(response.data?.data || {})
      };
    } catch (err) {
      return {
        isOnline: false,
        provider: 'none',
        configured: false,
        activeMode: 'OFFLINE',
        status: 'BACKEND_UNAVAILABLE',
        error: err.message
      };
    }
  },

  /**
   * Pre-computed Offline Safety Guidance
   * Invoked for seamless offline fallback or when explicitly requested.
   * Explicitly labeled as offline emergency advice.
   */
  async listConversations(page = 0, size = 15) {
    const response = await client.get('/chat/conversations', {
      params: { page, size }
    });
    return response.data?.data;
  },

  async createConversation(title) {
    const response = await client.post('/chat/conversations', { title });
    return response.data?.data;
  },

  async getConversation(id, page = 0, size = 30) {
    const response = await client.get(`/chat/conversations/${id}`, {
      params: { page, size }
    });
    return response.data?.data;
  },

  async deleteConversation(id) {
    const response = await client.delete(`/chat/conversations/${id}`);
    return response.data;
  },

  getOfflineEmergencyGuidance(payload, failureInfo) {
    const text = (payload.message || '').trim().toLowerCase();
    const history = payload.conversationHistory || [];
    const lastTurn = history.length > 0 ? history[history.length - 1].content.toLowerCase() : '';

    let intent = 'GENERAL_CYBER_SAFETY';
    let reply = '';
    let followUpSuggestions = [];
    let actionChecklist = [];

    // Phishing incident containment
    if (text.includes('phish') || text.includes('fake link') || text.includes('scam website') || text.includes('suspicious link') || text.includes('sus link') || text.includes('attack')) {
      intent = 'PHISHING_INCIDENT';
      reply = `### [Offline Guidance] Phishing Incident Containment Protocol\n\nIf you encountered or suspect you are under a **phishing attack**:\n\n1. **Do Not Enter Credentials:** If the webpage asks for NetBanking passwords, UPI PINs, Aadhaar details, or OTPs, close the page immediately.\n2. **Disconnect / Change Passwords:** If you already entered login credentials, immediately change your password from a clean, secure device and terminate active sessions.\n3. **Bank Account Safeguard:** If debit/credit card or banking details were provided, call your bank's 24x7 emergency helpline immediately or freeze digital access via your mobile banking app.\n4. **Report to Indian Cyber Helpline (1930):** Immediately call **1930** (National Cyber Crime Reporting Helpline) or log a complaint at [cybercrime.gov.in](https://cybercrime.gov.in/).\n5. **Scan with Prahar:** Use the **Link Scanner** on Prahar's Home tab to inspect lexical features and brand impersonation indicators.`;
      followUpSuggestions = ['What if I entered my OTP?', 'How do I report on cybercrime.gov.in?', 'Dial 1930 Helpline'];
      actionChecklist = [
        'Close suspicious page immediately',
        'Change compromised passwords from a safe device',
        'Contact bank helpline or freeze cards if banking info entered',
        'Report to 1930 National Cyber Helpline or cybercrime.gov.in'
      ];
      return buildOfflineResponse(reply, intent, followUpSuggestions, actionChecklist, failureInfo);
    }

    // Contextual Follow-up checking
    if (lastTurn.includes('payment channel') || lastTurn.includes('upi, debit/credit card')) {
      if (text.includes('upi') || text.includes('gpay') || text.includes('phonepe') || text.includes('paytm')) {
        intent = 'UPI_FRAUD';
        reply = `### [Offline Guidance] Unauthorized UPI Transfer Containment\n\nSince this transaction was initiated via **UPI**:\n\n1. **Locate the 12-Digit UTR:** Open your UPI app (Google Pay, PhonePe, Paytm, or BHIM) and copy the **12-digit UTR/UPI reference number**.\n2. **Dispute in App:** Go to the transaction history and select **'Report Fraud / Having an issue'**.\n3. **Golden Hour Reporting (1930):** Immediately call the Indian National Cyber Crime Helpline at **1930** with the UTR number. Authorities can instruct the beneficiary bank to place a temporary freeze on the funds.\n4. **Change UPI PIN:** If you entered your UPI PIN on a suspicious web page or QR scan, reset your UPI PIN inside your bank application.`;
        followUpSuggestions = ['What if I shared my OTP?', 'How do I file on cybercrime.gov.in?', 'Check my recent transactions'];
        actionChecklist = ['Obtain 12-digit UTR from UPI app', 'Raise dispute in UPI payment app', 'Call 1930 National Cyber Helpline', 'Change UPI PIN inside banking app'];
        return buildOfflineResponse(reply, intent, followUpSuggestions, actionChecklist, failureInfo);
      }
    }

    // Bank details disclosure
    if (text.includes('bank details') || text.includes('banking details') || text.includes('card details') || text.includes('entered my bank')) {
      intent = 'ACCOUNT_COMPROMISE';
      reply = `### [Offline Guidance] Banking Details Compromise Protocol\n\nSince you entered banking details on an untrusted page:\n\n1. **Lock Your Cards & NetBanking:** Immediately open your banking app and freeze your debit/credit card and electronic transactions.\n2. **Contact Bank's Fraud Desk:** Call the 24x7 customer helpline printed on the back of your card.\n3. **Monitor Debits & OTPs:** Watch for unauthorized transactions; never approve or share any verification code.\n4. **Report to 1930 Helpline:** Call **1930** or report at cybercrime.gov.in if an unauthorized transaction occurs.`;
      followUpSuggestions = ['Did an unauthorized debit occur?', 'Call 1930 Helpline', 'How to lock debit card?'];
      actionChecklist = ['Freeze card/NetBanking in banking app', 'Contact bank 24x7 emergency helpline', 'Monitor SMS for unauthorized debits', 'Report to 1930 Cyber Helpline'];
      return buildOfflineResponse(reply, intent, followUpSuggestions, actionChecklist, failureInfo);
    }

    if (text.includes('otp') || text.includes('password') || text.includes('pin')) {
      if (text.includes('shar') || text.includes('gave') || text.includes('enter') || text.includes('leak') || text.includes('too')) {
        intent = 'OTP_DISCLOSURE';
        reply = `### [Offline Guidance] Urgent: OTP / Password Disclosure Protocol\n\nIf you shared an OTP or entered your NetBanking password on an untrusted page:\n\n1. **Immediately Freeze NetBanking & Cards:** Contact your bank's emergency helpline or use mobile banking to freeze electronic channels.\n2. **Change Passwords from a Clean Device:** Do not use the same device if you suspect malware was downloaded. Choose *'Log out of all existing sessions'*.\n3. **Call 1930 Helpline:** Immediately inform Indian cyber authorities so the fraudulent destination account can be flagged.\n4. **File Complaint at cybercrime.gov.in:** Upload screenshots and SMS alerts as supporting evidence.`;
        followUpSuggestions = ['Draft a formal complaint letter', 'How do I file on cybercrime.gov.in?', 'Check my transaction ledger'];
        actionChecklist = ['Freeze accounts via bank hotline', 'Change NetBanking password from clean device', 'Report to 1930 National Cyber Helpline'];
        return buildOfflineResponse(reply, intent, followUpSuggestions, actionChecklist, failureInfo);
      }
    }

    if (text.includes('phone') && (text.includes('stolen') || text.includes('lost') || text.includes('theft'))) {
      intent = 'STOLEN_PHONE';
      reply = `### [Offline Guidance] Lost / Stolen Phone Security Checklist\n\n1. **Block SIM Immediately:** Call your telecom provider (Jio, Airtel, Vi, BSNL) from another phone to disable SMS OTPs.\n2. **Unlink UPI:** Contact your bank to block UPI payments linked to that phone number.\n3. **Block IMEI on CEIR:** Visit [ceir.gov.in](https://www.ceir.gov.in/) to block the phone across India.\n4. **Remotely Wipe Device:** Use Google Find My Device or Apple iCloud to erase all stored data.`;
      followUpSuggestions = ['How to block IMEI on CEIR?', 'How to unlink UPI?'];
      actionChecklist = ['Block SIM with telecom operator', 'Unlink UPI with bank', 'Block IMEI on ceir.gov.in'];
      return buildOfflineResponse(reply, intent, followUpSuggestions, actionChecklist, failureInfo);
    }

    // Default general offline advice
    intent = 'GENERAL_CYBER_SAFETY';
    reply = `### [Offline Guidance] Cyber Safety Advice\n\n*Notice: Operating in offline mode (${failureInfo?.message || 'Backend unreachable'}).*\n\n1. **Financial Fraud Emergencies:** Call the National Cyber Crime Helpline at **1930** immediately.\n2. **Suspicious Links:** Never enter passwords, banking details, or OTPs on unverified links received via SMS, WhatsApp, or email.\n3. **UPI Safety:** Entering a UPI PIN always sends money; you never need to enter a PIN to receive money.`;
    followUpSuggestions = ['1930 Helpline details', 'How do I spot a fake bank SMS?'];
    actionChecklist = ['Dial 1930 for financial cyber fraud', 'Never share banking OTP or UPI PIN'];

    return buildOfflineResponse(reply, intent, followUpSuggestions, actionChecklist, failureInfo);
  }
};

function buildOfflineResponse(reply, intent, followUps, actionChecklist, failureInfo) {
  return {
    reply,
    intent,
    confidence: 0.85,
    isLiveAi: false,
    provider: 'prahar_offline_cached',
    isOfflineFallback: true,
    offlineNotice: `Offline Emergency Mode: The live Prahar service was unreachable (${failureInfo?.message || 'Network error'}). Showing pre-computed safety guidance.`,
    followUpSuggestions: followUps,
    actionChecklist
  };
}
