import axios from 'axios';
import { API_URL } from '../utils/constants';
import { tokenStore } from './tokenStore';

/** Axios instance: base URL, Bearer token, one silent refresh on 401, then logout. */
const api = axios.create({ baseURL: API_URL, timeout: 30000 });

api.interceptors.request.use((config) => {
  const token = tokenStore.accessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshPromise = null;
const onSessionExpired = new Set();
export const addSessionExpiredListener = (fn) => {
  onSessionExpired.add(fn);
  return () => onSessionExpired.delete(fn);
};

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const isAuthCall = original?.url?.includes('/auth/login') || original?.url?.includes('/auth/refresh');
    const session = tokenStore.get();
    if (status === 401 && !original._retry && !isAuthCall && session?.refreshToken) {
      original._retry = true;
      try {
        refreshPromise = refreshPromise || axios.post(`${API_URL}/auth/refresh`, { refreshToken: session.refreshToken })
          .finally(() => { setTimeout(() => { refreshPromise = null; }, 0); });
        const { data } = await refreshPromise;
        tokenStore.update({ accessToken: data.data.accessToken });
        original.headers.Authorization = `Bearer ${data.data.accessToken}`;
        return api(original);
      } catch (e) {
        tokenStore.clear();
        onSessionExpired.forEach((fn) => fn());
        return Promise.reject(e);
      }
    }
    if (status === 401 && !isAuthCall && session) {
      tokenStore.clear();
      onSessionExpired.forEach((fn) => fn());
    }
    return Promise.reject(error);
  },
);

/** Unwraps { success, message, data } */
export const unwrap = (promise) => promise.then((res) => res.data.data);

/** Unwraps but keeps the server message (for toasts). */
export const unwrapWithMessage = (promise) => promise.then((res) => ({ data: res.data.data, message: res.data.message }));

/** Builds query params, skipping empty values. */
export const params = (obj = {}) =>
  Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== '' && v !== null && v !== undefined));

/** Downloads a binary endpoint as a file. */
export const download = async (url, query, fallbackName) => {
  const res = await api.get(url, { params: params(query), responseType: 'blob' });
  const disposition = res.headers['content-disposition'] || '';
  const match = disposition.match(/filename="?([^"]+)"?/);
  const href = URL.createObjectURL(res.data);
  const a = document.createElement('a');
  a.href = href;
  a.download = match ? match[1] : fallbackName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(href);
};

export default api;
