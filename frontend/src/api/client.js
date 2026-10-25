import axios from 'axios';

/**
 * Resolves and normalizes the backend API base URL.
 * Guarantees that:
 * 1. Falls back to '/api' if VITE_API_URL is omitted (for local proxy or Vercel rewrites).
 * 2. Strips trailing slashes cleanly.
 * 3. Automatically appends '/api' if an absolute HTTP/HTTPS URL is supplied without it,
 *    matching Spring Boot's @RequestMapping("/api/...") controllers.
 */
export function getApiBaseUrl() {
  let url = (import.meta.env.VITE_API_URL || '/api').trim();
  if (!url) return '/api';
  url = url.replace(/\/+$/, '');
  if (/^https?:\/\//i.test(url) && !/\/api$/i.test(url)) {
    url += '/api';
  }
  return url;
}

export const API_BASE_URL = getApiBaseUrl();

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Attach JWT token if available (supports prahar_token and phishshield_token)
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('prahar_token') || localStorage.getItem('phishshield_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export default client;
