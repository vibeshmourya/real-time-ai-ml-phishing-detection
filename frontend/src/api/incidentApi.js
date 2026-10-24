import client from './client';

export const incidentApi = {
  getIncidents: async ({ page = 0, size = 10, status, type } = {}) => {
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', size);
    if (status) params.append('status', status);
    if (type) params.append('type', type);

    const response = await client.get(`/incidents?${params.toString()}`);
    return response.data?.data || response.data;
  },

  createIncident: async (incidentData) => {
    const response = await client.post('/incidents', incidentData);
    return response.data?.data || response.data;
  },

  updateStatus: async (id, status, internalNotes) => {
    const response = await client.patch(`/incidents/${id}/status`, null, {
      params: { status, internalNotes },
    });
    return response.data?.data || response.data;
  },

  updateChecklist: async (id, checklistItems) => {
    const response = await client.put(`/incidents/${id}/checklist`, checklistItems);
    return response.data?.data || response.data;
  },

  getComplaintDraft: async (id) => {
    const response = await client.get(`/incidents/${id}/complaint-draft`);
    return response.data?.data || response.data;
  },
};

export default incidentApi;
