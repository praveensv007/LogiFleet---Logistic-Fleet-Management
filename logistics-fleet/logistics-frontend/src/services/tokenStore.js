// Session lives in localStorage when "remember me" is ticked, otherwise in sessionStorage.
const KEY = 'logifleet_auth';

export const tokenStore = {
  get() {
    const raw = localStorage.getItem(KEY) || sessionStorage.getItem(KEY);
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },
  set(session, remember) {
    const target = remember ?? Boolean(localStorage.getItem(KEY)) ? localStorage : sessionStorage;
    localStorage.removeItem(KEY);
    sessionStorage.removeItem(KEY);
    target.setItem(KEY, JSON.stringify(session));
  },
  update(patch) {
    const current = this.get();
    if (!current) return;
    const remember = Boolean(localStorage.getItem(KEY));
    this.set({ ...current, ...patch }, remember);
  },
  clear() {
    localStorage.removeItem(KEY);
    sessionStorage.removeItem(KEY);
  },
  accessToken() {
    return this.get()?.accessToken || null;
  },
};
