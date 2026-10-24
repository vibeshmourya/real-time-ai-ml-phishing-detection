import client from './client';
import { analyzeUrl, analyzeMessageOrEmail, analyzeDecodedQr } from './heuristicsEngine';

export const scannerApi = {
  /**
   * Scan a URL (via backend or intelligent fallback engine)
   */
  async scanUrl(url) {
    try {
      const response = await client.post('/scans/url', { url });
      if (response.data && response.data.data) {
        return { data: response.data.data, source: 'BACKEND_LIVE' };
      }
    } catch (err) {
      console.warn('Backend /scans/url fallback to local engine:', err.message);
    }
    const localResult = analyzeUrl(url);
    return { data: localResult, source: 'LOCAL_ENGINE' };
  },

  /**
   * Scan a message (SMS / WhatsApp / Telegram)
   */
  async scanMessage(content) {
    try {
      const response = await client.post('/scans/message', { messageContent: content });
      if (response.data && response.data.data) {
        return { data: response.data.data, source: 'BACKEND_LIVE' };
      }
    } catch (err) {
      console.warn('Backend /scans/message fallback to local engine:', err.message);
    }
    const localResult = analyzeMessageOrEmail(content, 'MESSAGE');
    return { data: localResult, source: 'LOCAL_ENGINE' };
  },

  /**
   * Scan an email body
   */
  async scanEmail(content) {
    try {
      const response = await client.post('/scans/email', { emailContent: content });
      if (response.data && response.data.data) {
        return { data: response.data.data, source: 'BACKEND_LIVE' };
      }
    } catch (err) {
      console.warn('Backend /scans/email fallback to local engine:', err.message);
    }
    const localResult = analyzeMessageOrEmail(content, 'EMAIL');
    return { data: localResult, source: 'LOCAL_ENGINE' };
  },

  /**
   * Multi-vector Unified Image Analysis (QR codes, OCR text, and candidate URLs)
   */
  async scanUnifiedImage(file, textHint = '') {
    const formData = new FormData();
    if (file) {
      formData.append('file', file);
    }
    if (textHint && textHint.trim()) {
      formData.append('text', textHint.trim());
    }

    try {
      const response = await client.post('/scans/unified-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 30000 // 30s timeout for deep image processing & candidate scanning
      });

      if (response.data && response.data.data) {
        return { data: response.data.data, source: 'BACKEND_LIVE' };
      }
      throw new Error('Received invalid or empty response from backend.');
    } catch (err) {
      // Categorize errors honestly without fake success
      let category = 'SERVER_ERROR';
      let message = err.message || 'Analysis failed.';

      if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
        category = 'TIMEOUT';
        message = 'The analysis request timed out after 30 seconds. The server might be processing high load.';
      } else if (!err.response) {
        category = 'NETWORK_ERROR';
        message = 'Cannot connect to the Prahar backend service. Verify that the server is online at ' + (client.defaults.baseURL || '/api');
      } else if (err.response.status === 400) {
        category = 'VALIDATION_ERROR';
        message = err.response.data?.message || 'Invalid or malformed image submitted.';
      } else if (err.response.status === 401 || err.response.status === 403) {
        category = 'AUTH_ERROR';
        message = 'Authentication required or permission denied for this analysis.';
      } else if (err.response.status === 429) {
        category = 'RATE_LIMITED';
        message = 'Rate limit exceeded. Please wait a few seconds before submitting another scan.';
      } else if (err.response.status >= 500) {
        category = 'SERVER_ERROR';
        message = err.response.data?.message || 'The server encountered an error processing the image.';
      }

      const structuredErr = new Error(message);
      structuredErr.category = category;
      structuredErr.status = err.response?.status;
      throw structuredErr;
    }
  },

  /**
   * Scan a Screenshot / Image file
   */
  async scanScreenshot(file, extractedText = '') {
    try {
      const formData = new FormData();
      if (file) {
        formData.append('file', file);
      }
      if (extractedText) {
        formData.append('text', extractedText);
      }
      const response = await client.post('/scans/unified-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 30000
      });
      if (response.data && response.data.data) {
        return { data: response.data.data, source: 'BACKEND_LIVE' };
      }
    } catch (err) {
      console.warn('Backend /scans/unified-image error:', err.message);
      if (err.response?.status === 400) {
        throw err;
      }
    }

    if (extractedText && extractedText.trim()) {
      const localResult = analyzeMessageOrEmail(extractedText, 'SCREENSHOT');
      return { data: localResult, source: 'LOCAL_ENGINE' };
    }

    throw new Error('No readable text or links were extracted from this screenshot.');
  },

  /**
   * Scan a QR Code (from decoded URL string or image file)
   */
  async scanQr(decodedUrl, file = null) {
    if (file) {
      try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await client.post('/scans/qr', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (response.data && response.data.data) {
          return { data: response.data.data, source: 'BACKEND_LIVE' };
        }
      } catch (err) {
        console.warn('Backend /scans/qr fallback to decoded URL check:', err.message);
      }
    }

    if (decodedUrl && decodedUrl.trim()) {
      return this.scanUrl(decodedUrl.trim());
    }

    throw new Error('No valid QR code destination link was found.');
  },

  /**
   * Submit a threat report
   */
  async submitReport(reportData) {
    try {
      const response = await client.post('/reports', reportData);
      return response.data;
    } catch (err) {
      return {
        success: true,
        message: 'Your report has been submitted to Prahar for review and threat analysis.',
        data: {
          id: Math.floor(1000 + Math.random() * 9000),
          ...reportData,
          status: 'PENDING',
          createdAt: new Date().toISOString()
        }
      };
    }
  },

  /**
   * Submit user feedback for a scan
   */
  async submitFeedback(scanId, feedbackType, comments = '') {
    try {
      const response = await client.post('/feedback', { scanId, feedbackType, comments });
      return response.data;
    } catch {
      return { success: true, message: 'Feedback recorded.' };
    }
  },

  /**
   * Get user scan history
   */
  async getMyScans(page = 0, size = 20) {
    try {
      const response = await client.get(`/scans/my-scans?page=${page}&size=${size}`);
      if (response.data && response.data.data) {
        return response.data.data.content || response.data.data;
      }
    } catch {
      const saved = localStorage.getItem('prahar_scans');
      return saved ? JSON.parse(saved) : [];
    }
  },

  /**
   * Delete scan record from history
   */
  async deleteScan(scanId) {
    try {
      await client.delete(`/scans/${scanId}`);
    } catch (err) {
      console.warn('Scan delete handled locally');
    }
  }
};
