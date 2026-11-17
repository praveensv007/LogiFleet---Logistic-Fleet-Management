import { useEffect, useRef } from 'react';
import { subscribe } from '../services/websocket';

/**
 * Subscribes to a STOMP destination while the component is mounted.
 * Pass a falsy destination to skip. The handler can change between renders.
 */
export default function useWebSocket(destination, handler) {
  const handlerRef = useRef(handler);
  handlerRef.current = handler;

  useEffect(() => {
    if (!destination) return undefined;
    return subscribe(destination, (msg) => handlerRef.current?.(msg));
  }, [destination]);
}
