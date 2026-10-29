import client from './client';

export const transactionApi = {
  getTransactions: async ({ page = 0, size = 15, status, category, direction, search } = {}) => {
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', size);
    if (status) params.append('status', status);
    if (category) params.append('category', category);
    if (direction) params.append('direction', direction);
    if (search) params.append('search', search);

    const response = await client.get(`/transactions?${params.toString()}`);
    return response.data?.data || response.data;
  },

  getSummary: async () => {
    const response = await client.get('/transactions/summary');
    return response.data?.data || response.data;
  },

  createManual: async (transactionData) => {
    const response = await client.post('/transactions', transactionData);
    return response.data?.data || response.data;
  },

  previewImport: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    // Note: Do not set manual multipart Content-Type header to allow browser to generate boundary
    const response = await client.post('/transactions/import/preview', formData);
    return response.data?.data || response.data;
  },

  confirmImport: async (confirmData) => {
    const response = await client.post('/transactions/import/confirm', confirmData);
    return response.data?.data || response.data;
  },

  uploadFile: async (file, options = {}) => {
    const formData = new FormData();
    formData.append('file', file);
    if (options.confirmAmbiguous) {
      formData.append('confirmAmbiguous', 'true');
    }
    const response = await client.post('/transactions/import/unified', formData);
    return response.data?.data || response.data;
  },

  uploadCsv: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await client.post('/transactions/import/unified', formData);
    return response.data?.data || response.data;
  },

  parseNotification: async (text) => {
    const response = await client.post('/transactions/parse-text', { text });
    return response.data?.data || response.data;
  },

  analyzeSmsFraud: async (text) => {
    const response = await client.post('/transactions/analyze-sms', { text });
    return response.data?.data || response.data;
  },

  verifyTransaction: async (id) => {
    const response = await client.patch(`/transactions/${id}/verify`);
    return response.data?.data || response.data;
  },

  updateCategory: async (id, category) => {
    const response = await client.patch(`/transactions/${id}/category`, null, {
      params: { category },
    });
    return response.data?.data || response.data;
  },

  deleteTransaction: async (id) => {
    const response = await client.delete(`/transactions/${id}`);
    return response.data?.data || response.data;
  },

  getExportUrl: () => {
    const baseUrl = client.defaults.baseURL || '/api';
    return `${baseUrl}/transactions/export/csv`;
  },
};

export default transactionApi;
