import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { fetchAdminSession, loginAdmin, logoutAdmin } from '../api/auth';
import { ADMIN_UNAUTHORIZED_EVENT, ApiError } from '../api/http';
import type { AdminUser } from '../api/types';

export type AuthStatus = 'checking' | 'guest' | 'authenticated' | 'unreachable';

interface AuthContextValue {
  status: AuthStatus;
  user: AdminUser | null;
  /** Sign-in screen message (expired session, non-admin account). */
  notice: string | null;
  /** Message shown when the initial session check could not reach the API. */
  bootError: string | null;
  retryBoot: () => void;
  signIn: (email: string, password: string) => Promise<AdminUser>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within <AuthProvider>');
  }
  return context;
}

const SESSION_EXPIRED_NOTICE = 'Your session expired. Sign in again.';

function describeBootError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status >= 500) {
      return 'The API reported a server error. Try again in a moment.';
    }
    return error.message;
  }
  return 'The API could not be reached. Check that the backend is running and try again.';
}

/**
 * Session state for the admin SPA.
 *
 * The session is established from the HttpOnly cookie via
 * `GET /admin/auth/session` on boot; there is no token in browser storage.
 * When an authenticated request is rejected with 401 anywhere in the app,
 * the local session ends and the admin is routed back to sign-in.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('checking');
  const [user, setUser] = useState<AdminUser | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [bootError, setBootError] = useState<string | null>(null);
  const userRef = useRef<AdminUser | null>(null);

  useEffect(() => {
    userRef.current = user;
  }, [user]);

  const boot = useCallback(async () => {
    setStatus('checking');
    setBootError(null);

    try {
      const sessionUser = await fetchAdminSession();
      setUser(sessionUser);
      setStatus('authenticated');
    } catch (error) {
      setUser(null);

      if (error instanceof ApiError && error.status === 401) {
        setStatus('guest');
        return;
      }

      if (error instanceof ApiError && error.status === 403) {
        setNotice('This account is not an administrator.');
        setStatus('guest');
        return;
      }

      setBootError(describeBootError(error));
      setStatus('unreachable');
    }
  }, []);

  useEffect(() => {
    void boot();
  }, [boot]);

  useEffect(() => {
    const handleUnauthorized = () => {
      if (userRef.current !== null) {
        setNotice(SESSION_EXPIRED_NOTICE);
      }
      setUser(null);
      setStatus('guest');
    };

    window.addEventListener(ADMIN_UNAUTHORIZED_EVENT, handleUnauthorized);
    return () => window.removeEventListener(ADMIN_UNAUTHORIZED_EVENT, handleUnauthorized);
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const signedInUser = await loginAdmin(email, password);
    setUser(signedInUser);
    setNotice(null);
    setStatus('authenticated');
    return signedInUser;
  }, []);

  const signOut = useCallback(async () => {
    try {
      await logoutAdmin();
    } catch {
      // Ending the local session must not depend on the API round-trip;
      // a dead server session expires on its own.
    }
    setUser(null);
    setNotice(null);
    setStatus('guest');
  }, []);

  const retryBoot = useCallback(() => {
    void boot();
  }, [boot]);

  const value = useMemo(
    () => ({ status, user, notice, bootError, retryBoot, signIn, signOut }),
    [status, user, notice, bootError, retryBoot, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
