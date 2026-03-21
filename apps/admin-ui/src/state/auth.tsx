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

interface AuthContextValue {
  session: AdminSession | null;
  bootstrapping: boolean;
  login: (credentials: Credentials) => Promise<void>;
  refresh: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

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

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
