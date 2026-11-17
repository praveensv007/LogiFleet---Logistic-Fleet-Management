import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import { addSessionExpiredListener } from '../services/api';
import { tokenStore } from '../services/tokenStore';
import { connect, disconnect } from '../services/websocket';
import { ROLE_HOME } from '../utils/constants';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const [user, setUser] = useState(() => tokenStore.get()?.user || null);

  useEffect(() => {
    connect(Boolean(user));
  }, [user]);

  useEffect(() => addSessionExpiredListener(() => {
    setUser(null);
    disconnect();
    navigate('/login', { replace: true, state: { expired: true } });
  }), [navigate]);

  const login = useCallback(async ({ usernameOrEmail, password, rememberMe }) => {
    const res = await authService.login({ usernameOrEmail, password, rememberMe });
    const { accessToken, refreshToken, ...profile } = res;
    tokenStore.set({ accessToken, refreshToken, user: profile }, rememberMe);
    disconnect();
    setUser(profile);
    return profile;
  }, []);

  /** After self-registration the API already returns tokens. */
  const loginWithResponse = useCallback((res) => {
    const { accessToken, refreshToken, ...profile } = res;
    tokenStore.set({ accessToken, refreshToken, user: profile }, false);
    disconnect();
    setUser(profile);
    return profile;
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    tokenStore.clear();
    disconnect();
    setUser(null);
    navigate('/login', { replace: true });
  }, [navigate]);

  /** Merge fresh profile fields (name, photo, mustChangePassword...) into the session. */
  const updateUser = useCallback((patch) => {
    setUser((prev) => {
      const next = { ...prev, ...patch };
      tokenStore.update({ user: next });
      return next;
    });
  }, []);

  const hasRole = useCallback((...roles) => Boolean(user && roles.includes(user.role)), [user]);
  const can = useCallback((permission) => Boolean(user?.permissions?.includes(permission)), [user]);
  const homePath = user ? ROLE_HOME[user.role] || '/' : '/login';

  const value = useMemo(() => ({ user, login, loginWithResponse, logout, updateUser, hasRole, can, homePath }),
    [user, login, loginWithResponse, logout, updateUser, hasRole, can, homePath]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
