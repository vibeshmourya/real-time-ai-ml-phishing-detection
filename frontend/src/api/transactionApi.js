import client from './client';

export const transactionApi = {
  getTransactions: async ({ page = 0, size = 15, status, category, direction } = {}) => {
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', size);
    if (status) params.append('status', status);
    if (category) params.append('category', category);
    if (direction) params.append('direction', direction);

    const response = await client.get(`/transactions?${params.toString()}`);
    return response.data?.data || response.data;
  },

  getSummary: async () => {
    const response = await client.get('/transactions/summary');
    return response.data?.data || response.data;
  },

  createManual: async (transactionData) => {
    const response = await client.post('/transactions/manual', transactionData);
    return response.data?.data || response.data;
  },

  uploadCsv: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await client.post('/transactions/upload-csv', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data?.data || response.data;
  },

  parseNotification: async (text) => {
    const response = await client.post('/transactions/parse-notification', { text });
    return response.data?.data || response.data;
  },

  verifyTransaction: async (id) => {
    const response = await client.post(`/transactions/${id}/verify`);
    return response.data?.data || response.data;
  },

  updateCategory: async (id, category) => {
    const response = await client.patch(`/transactions/${id}/category`, null, {
      params: { category },
    });
    return response.data?.data || response.data;
  },

  getExportUrl: () => {
    const baseUrl = client.defaults.baseURL || '/api';
    return `${baseUrl}/transactions/export-csv`;
  },
};

export default transactionApi;
