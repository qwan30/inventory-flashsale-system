import { useEffect, useState } from "react";

interface AsyncState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

export function useAsyncResource<T>(
  loader: (() => Promise<T>) | null,
  deps: readonly unknown[],
) {
  const [reloadToken, setReloadToken] = useState(0);
  const [state, setState] = useState<AsyncState<T>>({
    data: null,
    loading: Boolean(loader),
    error: null,
  });

  useEffect(() => {
    let active = true;
    if (!loader) {
      setState({ data: null, loading: false, error: null });
      return;
    }

    setState((current) => ({ ...current, loading: true, error: null }));
    loader()
      .then((data) => {
        if (active) {
          setState({ data, loading: false, error: null });
        }
      })
      .catch((error: Error) => {
        if (active) {
          setState({ data: null, loading: false, error: error.message });
        }
      });

    return () => {
      active = false;
    };
  }, [...deps, reloadToken]);

  return {
    ...state,
    reload() {
      setReloadToken((current) => current + 1);
    },
  };
}
