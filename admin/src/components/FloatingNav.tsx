export interface NavItem {
  label: string;
  href: string;
  active?: boolean;
}

export function FloatingNav({ items }: { items: NavItem[] }) {
  return (
    <nav
      aria-label="Main Navigation"
      className="fixed bottom-6 inset-x-0 mx-auto w-fit z-50 px-3 py-2 rounded-full bg-neutral-900/80 dark:bg-neutral-950/80 backdrop-blur-2xl border border-white/10 shadow-2xl flex items-center gap-1"
    >
      {items.map((item) => (
        <a
          key={item.href}
          href={item.href}
          className={`relative px-4 py-2 rounded-full text-xs sm:text-sm font-medium transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
            item.active
              ? 'text-white bg-white/10 shadow-inner'
              : 'text-neutral-400 hover:text-white hover:bg-white/5'
          }`}
        >
          {item.label}
          {item.active && (
            <span className="absolute bottom-1 inset-x-0 mx-auto w-1 h-1 rounded-full bg-primary" />
          )}
        </a>
      ))}
    </nav>
  );
}
