import { API_ORIGIN } from './constants';

export const pretty = (v) => {
  if (v === null || v === undefined || v === '') return '—';
  const s = String(v).replace(/_/g, ' ').toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1);
};

export const toOptions = (list) => list.map((v) => ({ value: v, label: pretty(v) }));

export const formatDate = (v) => {
  if (!v) return '—';
  const d = new Date(v);
  return Number.isNaN(d.getTime()) ? v : d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
};

export const formatDateTime = (v) => {
  if (!v) return '—';
  const d = new Date(v);
  return Number.isNaN(d.getTime()) ? v
    : d.toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
};

export const formatTime = (v) => (v ? String(v).slice(0, 5) : '—');

export const formatMoney = (v) => (v === null || v === undefined ? '—'
  : '₹' + Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));

export const formatKg = (v) => (v === null || v === undefined ? '—' : `${Number(v).toLocaleString('en-IN')} kg`);

/** /api/files/x.png -> http://localhost:8080/api/files/x.png */
export const fileUrl = (url) => (!url ? null : url.startsWith('http') ? url : API_ORIGIN + url);

export const timeAgo = (v) => {
  if (!v) return '';
  const diff = (Date.now() - new Date(v).getTime()) / 1000;
  if (diff < 60) return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)} min ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} h ago`;
  return formatDate(v);
};

/** yyyy-mm-dd for <input type=date> */
export const isoDate = (d = new Date()) => {
  const z = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return z.toISOString().slice(0, 10);
};

/** yyyy-mm-ddThh:mm for <input type=datetime-local> */
export const isoDateTime = (d = new Date()) => {
  const z = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return z.toISOString().slice(0, 16);
};

/** Removes empty strings so optional fields go to the API as null. */
export const clean = (obj) => {
  if (Array.isArray(obj)) return obj.map(clean);
  if (obj && typeof obj === 'object' && !(obj instanceof Date)) {
    return Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, v === '' ? null : clean(v)]));
  }
  return obj;
};

export const errorMessage = (err, fallback = 'Something went wrong') =>
  err?.response?.data?.message || err?.message || fallback;
