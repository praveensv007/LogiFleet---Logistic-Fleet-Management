import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage } from '../utils/formatters';

/**
 * Runs an async loader when deps change.
 * Returns { data, loading, error, reload, setData }. reload(true) refreshes without the spinner.
 */
export default function useFetch(loader, deps = [], { immediate = true, initial = null } = {}) {
  const [data, setData] = useState(initial);
  const [loading, setLoading] = useState(immediate);
  const [error, setError] = useState(null);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;
  const seq = useRef(0);

  const reload = useCallback(async (silent = false) => {
    const id = ++seq.current;
    if (!silent) setLoading(true);
    setError(null);
    try {
      const result = await loaderRef.current();
      if (id === seq.current) setData(result);
      return result;
    } catch (e) {
      if (id === seq.current) setError(errorMessage(e));
      return null;
    } finally {
      if (id === seq.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (immediate) reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, loading, error, reload, setData };
}
