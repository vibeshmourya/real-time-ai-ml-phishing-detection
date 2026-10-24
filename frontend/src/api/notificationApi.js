import client from './client';

export const notificationApi = {
  /**
   * Fetch paginated notifications for the authenticated user
   */
  getNotifications: async (params = {}) => {
    const { unreadOnly = false, page = 0, size = 20 } = params;
    const response = await client.get('/notifications', {
      params: { unreadOnly, page, size }
    });
    return response.data;
  },

  /**
   * Get total unread count for badge display
   */
  getUnreadCount: async () => {
    const response = await client.get('/notifications/unread-count');
    return response.data;
  },

  /**
   * Mark a single notification as read
   */
  markAsRead: async (id) => {
    const response = await client.patch(`/notifications/${id}/read`);
    return response.data;
  },

  /**
   * Mark all notifications as read for current user
   */
  markAllAsRead: async () => {
    const response = await client.post('/notifications/mark-all-read');
    return response.data;
  },

  /**
   * Delete a notification record
   */
  deleteNotification: async (id) => {
    const response = await client.delete(`/notifications/${id}`);
    return response.data;
  },

  /**
   * Fetch current user notification preferences
   */
  getPreferences: async () => {
    const response = await client.get('/notifications/preferences');
    return response.data;
  },

  /**
   * Update notification preferences
   */
  updatePreferences: async (preferences) => {
    const response = await client.put('/notifications/preferences', preferences);
    return response.data;
  },

  /**
   * Create an EventSource connection for real-time SSE stream
   */
  createEventSource: (token) => {
    const baseUrl = import.meta.env.VITE_API_URL || '/api';
    const streamUrl = `${baseUrl}/notifications/stream?token=${encodeURIComponent(token)}`;
    return new EventSource(streamUrl);
  }
};

export default notificationApi;
