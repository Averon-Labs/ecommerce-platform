import { createContext, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

interface NavigationGuardValue {
  /** True while a page holds unsaved changes. */
  isBlocked: boolean;
  setBlocked: (blocked: boolean) => void;
}

const NavigationGuardContext = createContext<NavigationGuardValue | null>(null);

/**
 * Lets an editing page (product form) mark itself dirty so the shell can
 * confirm before following a navigation link. The guard is UX-only: the API
 * remains the source of truth for what was saved.
 */
export function NavigationGuardProvider({ children }: { children: ReactNode }) {
  const [isBlocked, setBlocked] = useState(false);
  const value = useMemo(() => ({ isBlocked, setBlocked }), [isBlocked]);

  return <NavigationGuardContext.Provider value={value}>{children}</NavigationGuardContext.Provider>;
}

export function useNavigationGuard(): NavigationGuardValue {
  const context = useContext(NavigationGuardContext);

  if (!context) {
    throw new Error('useNavigationGuard must be used within <NavigationGuardProvider>');
  }

  return context;
}
