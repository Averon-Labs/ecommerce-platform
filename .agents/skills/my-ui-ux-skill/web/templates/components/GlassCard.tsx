import React from 'react';

interface GlassCardProps extends React.HTMLAttributes<HTMLDivElement> {
  glowColor?: string;
  children: React.ReactNode;
}

export function GlassCard({ glowColor = 'rgba(99, 102, 241, 0.15)', className = '', children, ...props }: GlassCardProps) {
  return (
    <div
      {...props}
      className={`group relative rounded-3xl bg-white/70 dark:bg-neutral-900/60 backdrop-blur-2xl border border-white/20 dark:border-white/10 p-6 sm:p-8 shadow-glass transition-all duration-300 hover:shadow-glow hover:-translate-y-0.5 active:translate-y-0 ${className}`}
    >
      {/* Dynamic ambient background glow */}
      <div
        className="absolute inset-0 rounded-3xl opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"
        style={{
          background: `radial-gradient(600px circle at var(--mouse-x, 50%) var(--mouse-y, 50%), ${glowColor}, transparent 40%)`,
        }}
      />
      <div className="relative z-10">{children}</div>
    </div>
  );
}
