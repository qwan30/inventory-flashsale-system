import {
  createContext,
  useEffect,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  loginRequest,
  logoutRequest,
  refreshRequest,
  type AdminSession,
  type Credentials,
} from "../lib/api";

/**
 * Represents the authentication context value.
 * Tracks user session details, loading/bootstrapping state,
 * and exposes actions for logging in, refreshing sessions, or logging out.
 */
interface AuthContextValue {
  session: AdminSession | null;
  bootstrapping: boolean;
  login: (credentials: Credentials) => Promise<void>;
  refresh: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * Provider component supplying auth context value to its children.
 * Automatically triggers session bootstrapping on mount to check for refresh tokens in cookies.
 * 
 * @param children the nested React nodes
 * @param initialSession optional pre-restored session (for tests or server-side renders)
 * @param skipBootstrap if true, skips initial refresh request (bootstrapping)
 */
export function AuthProvider({
  children,
  initialSession = null,
  skipBootstrap = false,
}: {
  children: ReactNode;
  initialSession?: AdminSession | null;
  skipBootstrap?: boolean;
}) {
  const [session, setSession] = useState<AdminSession | null>(initialSession);
  const [bootstrapping, setBootstrapping] = useState(!skipBootstrap);

  useEffect(() => {
    let active = true;

    if (skipBootstrap) {
      setBootstrapping(false);
      return () => {
        active = false;
      };
    }

    // Try to silently restore session using cookie-based refresh token
    refreshRequest()
      .then((next) => {
        if (active) {
          setSession(next);
        }
      })
      .catch(() => {
        if (active) {
          setSession(null);
        }
      })
      .finally(() => {
        if (active) {
          setBootstrapping(false);
        }
      });

    return () => {
      active = false;
    };
  }, [skipBootstrap]);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      bootstrapping,
      async login(credentials) {
        const next = await loginRequest(credentials);
        setSession(next);
        setBootstrapping(false);
      },
      async refresh() {
        const next = await refreshRequest();
        setSession(next);
        setBootstrapping(false);
      },
      async logout() {
        try {
          await logoutRequest(session?.accessToken);
        } finally {
          setSession(null);
          setBootstrapping(false);
        }
      },
    }),
    [bootstrapping, session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Custom hook to access current authentication state and actions.
 * Must be used within an AuthProvider.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
