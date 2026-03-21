import { useCallback, useState } from "react";

interface AsyncActionState {
  pending: boolean;
  error: string | null;
  success: string | null;
}

export function useAsyncAction() {
  const [state, setState] = useState<AsyncActionState>({
    pending: false,
    error: null,
    success: null,
  });

  const run = useCallback(
    async <T>(action: () => Promise<T>, successMessage?: string): Promise<T> => {
      setState({ pending: true, error: null, success: null });

      try {
        const result = await action();
        setState({
          pending: false,
          error: null,
          success: successMessage ?? null,
        });
        return result;
      } catch (error) {
        setState({
          pending: false,
          error: error instanceof Error ? error.message : "Request failed",
          success: null,
        });
        throw error;
      }
    },
    [],
  );

  const reset = useCallback(() => {
    setState({ pending: false, error: null, success: null });
  }, []);

  return {
    ...state,
    run,
    reset,
  };
}
