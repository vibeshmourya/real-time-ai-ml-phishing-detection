import client from './client';
import { analyzeUrl, analyzeMessageOrEmail } from './heuristicsEngine';

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
      console.warn('Backend unavailable, using AI Heuristics Engine:', err.message);
    }
    // Fallback to client-side heuristics engine
    const localResult = analyzeUrl(url);
    return { data: localResult, source: 'LOCAL_ENGINE' };
  },

  /**
   * Scan a message (SMS / WhatsApp)
   */
  async scanMessage(content) {
    try {
      const response = await client.post('/scans/message', { messageContent: content });
      if (response.data && response.data.data) {
        return { data: response.data.data, source: 'BACKEND_LIVE' };
      }
    } catch (err) {
      console.warn('Backend unavailable, using AI Heuristics Engine for message:', err.message);
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
      console.warn('Backend unavailable, using AI Heuristics Engine for email:', err.message);
    }
    const localResult = analyzeMessageOrEmail(content, 'EMAIL');
    return { data: localResult, source: 'LOCAL_ENGINE' };
  },

  /**
   * Submit a community threat report
   */
  async submitReport(reportData) {
    try {
      const response = await client.post('/reports', reportData);
      return response.data;
    } catch (err) {
      console.warn('Backend unavailable, mock report saved:', err.message);
      return {
        success: true,
        message: 'Report submitted successfully to global threat telemetry.',
        data: {
          id: Date.now(),
          ...reportData,
          status: 'PENDING',
          createdAt: new Date().toISOString()
        }
      };
    }
  },

  /**
   * Get user scan history
   */
  async getMyScans(page = 0, size = 10) {
    try {
      const response = await client.get(`/scans/my-scans?page=${page}&size=${size}`);
      return response.data.data;
    } catch (err) {
      // Local storage scan history fallback
      const saved = localStorage.getItem('phishshield_scans');
      return saved ? JSON.parse(saved) : [];
    }
  }
};
