import { useEffect, useRef, useState } from 'react';
import type { MouseEvent as ReactMouseEvent } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ConfirmDialog } from './ConfirmDialog';
import { NavigationGuardProvider, useNavigationGuard } from './NavigationGuard';

function ProductsIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5 shrink-0"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M21 8.5 12 3 3 8.5v7L12 21l9-5.5v-7Z" />
      <path d="M3 8.5 12 14l9-5.5M12 14v7" />
    </svg>
  );
}

function CategoriesIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5 shrink-0"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M20.6 13.4 13.4 20.6a2 2 0 0 1-2.8 0L3 13V3h10l7.6 7.6a2 2 0 0 1 0 2.8Z" />
      <circle cx="7.5" cy="7.5" r="1.5" />
    </svg>
  );
}

function InventoryIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5 shrink-0"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="m12 2 9 5-9 5-9-5 9-5Z" />
      <path d="m3 12 9 5 9-5" />
      <path d="m3 17 9 5 9-5" />
    </svg>
  );
}

function MenuIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-6 w-6"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M4 7h16M4 12h16M4 17h16" />
    </svg>
  );
}

const NAV_ITEMS = [
  { to: '/products', label: 'Products', icon: <ProductsIcon /> },
  { to: '/categories', label: 'Categories', icon: <CategoriesIcon /> },
  { to: '/inventory', label: 'Inventory', icon: <InventoryIcon /> },
];

type NavClickHandler = (event: ReactMouseEvent<HTMLAnchorElement>, to: string) => void;

function navLinkClasses({ isActive }: { isActive: boolean }): string {
  return [
    'flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm font-semibold transition-colors',
    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
    isActive
      ? 'bg-primary/10 text-primary'
      : 'text-muted-foreground hover:bg-muted hover:text-foreground',
  ].join(' ');
}

function SectionNav({
  onNavigate,
  onItemClick,
}: {
  onNavigate?: () => void;
  onItemClick?: NavClickHandler;
}) {
  return (
    <nav aria-label="Admin sections" className="space-y-1 p-3">
      {NAV_ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={navLinkClasses}
          onClick={(event) => {
            onNavigate?.();
            onItemClick?.(event, item.to);
          }}
        >
          {item.icon}
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}

/**
 * Admin shell: static 200–248px sidebar on tablet and desktop, drawer below
 * 768px, top bar with the account menu, and a content area capped at 1440px
 * (docs/DESIGN_SYSTEM.md — Admin Web).
 *
 * The shell also asks before leaving a page with unsaved changes: editing
 * pages report their dirty state through the navigation guard.
 */
export function AppShell() {
  return (
    <NavigationGuardProvider>
      <AppShellLayout />
    </NavigationGuardProvider>
  );
}

function AppShellLayout() {
  const { user, signOut } = useAuth();
  const { isBlocked, setBlocked } = useNavigationGuard();
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [pendingPath, setPendingPath] = useState<string | null>(null);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const drawerButtonRef = useRef<HTMLButtonElement>(null);
  const drawerLinkRef = useRef<HTMLAnchorElement>(null);

  useEffect(() => {
    if (drawerOpen) {
      drawerLinkRef.current?.focus();
    }
  }, [drawerOpen]);

  useEffect(() => {
    if (!drawerOpen) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setDrawerOpen(false);
        drawerButtonRef.current?.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [drawerOpen]);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!menuButtonRef.current?.parentElement?.contains(target)) {
        setMenuOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMenuOpen(false);
        menuButtonRef.current?.focus();
      }
    };

    document.addEventListener('pointerdown', handlePointerDown);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [menuOpen]);

  const displayName = user ? `${user.firstName} ${user.lastName}` : 'Administrator';

  async function handleSignOut() {
    setMenuOpen(false);
    await signOut();
    navigate('/login', { replace: true });
  }

  function handleNavClick(event: ReactMouseEvent<HTMLAnchorElement>, to: string) {
    if (!isBlocked || event.defaultPrevented) {
      return;
    }

    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }

    event.preventDefault();
    setPendingPath(to);
  }

  return (
    <div className="min-h-screen bg-background">
      <a
        href="#admin-main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-card focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:shadow-glass"
      >
        Skip to content
      </a>

      <aside className="fixed inset-y-0 left-0 z-40 hidden w-[200px] flex-col border-r border-border bg-card md:flex xl:w-[248px]">
        <div className="flex h-16 items-center border-b border-border px-5">
          <span className="text-base font-bold tracking-tight">EShop Verse Admin</span>
        </div>
        <div className="flex-1 overflow-y-auto">
          <SectionNav onItemClick={handleNavClick} />
        </div>
      </aside>

      <div className="md:pl-[200px] xl:pl-[248px]">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between gap-4 border-b border-border bg-card/80 px-4 backdrop-blur sm:px-6">
          <div className="flex min-w-0 items-center gap-3">
            <button
              ref={drawerButtonRef}
              type="button"
              className="-ml-2 flex h-11 w-11 items-center justify-center rounded-xl text-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:hidden"
              aria-expanded={drawerOpen}
              aria-controls="admin-mobile-nav"
              onClick={() => setDrawerOpen(true)}
            >
              <span className="sr-only">Open navigation</span>
              <MenuIcon />
            </button>
            <span className="truncate text-sm font-semibold md:hidden">EShop Verse Admin</span>
          </div>

          <div className="relative">
            <button
              ref={menuButtonRef}
              type="button"
              className="flex min-h-11 items-center gap-3 rounded-full border border-border bg-card px-3 py-1.5 text-sm font-semibold hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <span
                aria-hidden="true"
                className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/20 text-xs font-bold text-foreground"
              >
                {user ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}` : 'A'}
              </span>
              <span className="hidden max-w-[16rem] truncate sm:inline">{displayName}</span>
              <span className="sr-only">Account menu</span>
            </button>

            {menuOpen && (
              <div
                role="menu"
                aria-label="Account"
                className="absolute right-0 top-[calc(100%+0.5rem)] z-50 w-64 animate-fade-in rounded-2xl border border-border bg-card p-2 shadow-glass"
              >
                <div className="px-3 py-2">
                  <p className="text-sm font-semibold">{displayName}</p>
                  {user && <p className="truncate text-xs text-muted-foreground">{user.email}</p>}
                </div>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => void handleSignOut()}
                  className="flex min-h-11 w-full items-center rounded-xl px-3 text-left text-sm font-semibold text-destructive hover:bg-destructive/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  Sign out
                </button>
              </div>
            )}
          </div>
        </header>

        <main id="admin-main" className="mx-auto w-full max-w-[1440px] p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>

      {drawerOpen && (
        <div className="fixed inset-0 z-50 md:hidden">
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setDrawerOpen(false)}
            aria-hidden="true"
          />
          <div
            id="admin-mobile-nav"
            role="dialog"
            aria-modal="true"
            aria-label="Admin navigation"
            className="absolute inset-y-0 left-0 w-72 max-w-[85vw] animate-fade-in overflow-y-auto border-r border-border bg-card shadow-glass"
          >
            <div className="flex h-16 items-center justify-between border-b border-border px-4">
              <span className="text-base font-bold tracking-tight">EShop Verse Admin</span>
              <button
                type="button"
                className="flex h-11 w-11 items-center justify-center rounded-xl text-sm font-semibold hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                onClick={() => {
                  setDrawerOpen(false);
                  drawerButtonRef.current?.focus();
                }}
              >
                Close
              </button>
            </div>
            <div
              ref={(node) => {
                drawerLinkRef.current = node?.querySelector('a') ?? null;
              }}
            >
              <SectionNav
                onNavigate={() => setDrawerOpen(false)}
                onItemClick={handleNavClick}
              />
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={pendingPath !== null}
        title="Discard unsaved changes?"
        description="This page has unsaved changes. Leave the page and discard them?"
        confirmLabel="Discard changes"
        onCancel={() => setPendingPath(null)}
        onConfirm={() => {
          const target = pendingPath;
          setPendingPath(null);
          setBlocked(false);
          if (target) {
            navigate(target);
          }
        }}
      />
    </div>
  );
}
