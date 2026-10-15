import React from 'react';

export interface BentoItemProps {
  title: string;
  subtitle?: string;
  badge?: string;
  className?: string;
  children?: React.ReactNode;
}

export function BentoGrid({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`grid grid-cols-1 md:grid-cols-12 gap-4 max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 ${className}`}>
      {children}
    </div>
  );
}

export function BentoCard({ title, subtitle, badge, className = '', children }: BentoItemProps) {
  return (
    <div
      className={`group relative overflow-hidden rounded-3xl bg-neutral-900/70 dark:bg-card border border-neutral-800 p-6 sm:p-8 backdrop-blur-xl shadow-glass hover:border-neutral-700 transition-all duration-300 ${className}`}
    >
      {/* Ambient hover glow */}
      <div className="absolute top-0 right-0 -mr-16 -mt-16 w-56 h-56 bg-primary/10 rounded-full blur-3xl pointer-events-none group-hover:bg-primary/20 transition-all duration-500" />

      <div className="relative z-10 flex flex-col justify-between h-full">
        <div>
          {badge && (
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-primary/10 text-primary border border-primary/20 mb-3">
              {badge}
            </span>
          )}
          <h3 className="text-xl sm:text-2xl font-semibold tracking-tight text-white">{title}</h3>
          {subtitle && <p className="text-sm text-neutral-400 mt-2 leading-relaxed">{subtitle}</p>}
        </div>

        {children && <div className="mt-6">{children}</div>}
      </div>
    </div>
  );
}
