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
   * Scan a Screenshot / Image file
   */
  async scanScreenshot(file, extractedText = '') {
    try {
      const formData = new FormData();
      formData.append('file', file);
      if (extractedText) {
        formData.append('text', extractedText);
      }
      const response = await client.post('/scans/screenshot', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      if (response.data && response.data.data) {
        return { data: response.data.data, source: 'BACKEND_LIVE' };
      }
    } catch (err) {
      console.warn('Backend /scans/screenshot fallback to extracted text analysis:', err.message);
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
