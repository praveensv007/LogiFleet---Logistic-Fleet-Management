import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_URL } from '../utils/constants';
import { tokenStore } from './tokenStore';

/**
 * One shared STOMP client over SockJS. The JWT is read before every (re)connect so a refreshed
 * token is picked up. Subscriptions made before the connection is ready are queued and re-attached
 * after every reconnect.
 */
let client = null;
let mode = null; // 'auth' | 'anon'
let nextId = 1;
const subscriptions = new Map(); // id -> { destination, callback, handle }

const attach = (entry) => {
  if (client?.connected) {
    entry.handle = client.subscribe(entry.destination, (msg) => {
      let body = msg.body;
      try {
        body = JSON.parse(msg.body);
      } catch {
        /* plain text */
      }
      entry.callback(body);
    });
  }
};

/** authenticated=true sends the stored JWT in the STOMP CONNECT frame. */
export const connect = (authenticated = Boolean(tokenStore.accessToken())) => {
  const wanted = authenticated ? 'auth' : 'anon';
  if (client && mode === wanted) return;
  if (client) client.deactivate();
  mode = wanted;
  const c = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {},
    beforeConnect: () => {
      const token = mode === 'auth' ? tokenStore.accessToken() : null;
      c.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
    },
    onConnect: () => subscriptions.forEach((entry) => attach(entry)),
    onStompError: (frame) => console.warn('STOMP error', frame.headers?.message),
  });
  client = c;
  client.activate();
};

export const disconnect = () => {
  if (client) client.deactivate();
  client = null;
  mode = null;
};

/** Returns an unsubscribe function. */
export const subscribe = (destination, callback) => {
  if (!client) connect();
  const id = nextId++;
  const entry = { destination, callback, handle: null };
  subscriptions.set(id, entry);
  attach(entry);
  return () => {
    try {
      entry.handle?.unsubscribe();
    } catch {
      /* connection already closed */
    }
    subscriptions.delete(id);
  };
};
