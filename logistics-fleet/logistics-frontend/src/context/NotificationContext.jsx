import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { toast } from 'react-toastify';
import notificationService from '../services/notificationService';
import { subscribe } from '../services/websocket';
import useAuth from '../hooks/useAuth';

const NotificationContext = createContext(null);

/**
 * Keeps the unread count and the latest notifications in sync over /user/queue/notifications.
 * Pages can listen with onNotification(fn) to refresh their own data when something arrives.
 */
export function NotificationProvider({ children }) {
  const { user } = useAuth();
  const [unread, setUnread] = useState(0);
  const [latest, setLatest] = useState([]);
  const [listeners] = useState(() => new Set());

  const refresh = useCallback(async () => {
    if (!user) return;
    try {
      const [count, page] = await Promise.all([
        notificationService.unreadCount(),
        notificationService.list({ page: 0, size: 8 }),
      ]);
      setUnread(count);
      setLatest(page.content);
    } catch {
      /* ignore: header badge only */
    }
  }, [user]);

  useEffect(() => {
    if (!user) {
      setUnread(0);
      setLatest([]);
      return undefined;
    }
    refresh();
    return subscribe('/user/queue/notifications', (n) => {
      setUnread((c) => c + 1);
      setLatest((list) => [n, ...list].slice(0, 8));
      toast.info(<div><strong>{n.title}</strong><div className="small">{n.message}</div></div>);
      listeners.forEach((fn) => fn(n));
    });
  }, [user, refresh, listeners]);

  const markRead = useCallback(async (id) => {
    await notificationService.markRead(id);
    setLatest((list) => list.map((n) => (n.id === id ? { ...n, read: true } : n)));
    setUnread((c) => Math.max(0, c - 1));
  }, []);

  const markAllRead = useCallback(async () => {
    await notificationService.markAllRead();
    setLatest((list) => list.map((n) => ({ ...n, read: true })));
    setUnread(0);
  }, []);

  const onNotification = useCallback((fn) => {
    listeners.add(fn);
    return () => listeners.delete(fn);
  }, [listeners]);

  const value = useMemo(() => ({ unread, latest, refresh, markRead, markAllRead, onNotification }),
    [unread, latest, refresh, markRead, markAllRead, onNotification]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export const useNotifications = () => useContext(NotificationContext);
