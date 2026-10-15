# Rule 01: Dribbble & Modern Aesthetic Systems

Modern web design has evolved past flat 2D templates. Award-winning Dribbble, Awwwards, and Linear-style interfaces achieve their signature feel through **four core techniques**:
1. **Asymmetric Bento Grids**
2. **Layered Glassmorphism & Micro-Borders**
3. **Deep OLED & Luminous Dark Mode**
4. **Subtle Ambient Aurora Glows**

---

## 1. Asymmetric Bento Grid Architecture

A Bento Grid groups diverse functionalities into a cohesive visual container with a strong visual anchor (Hero item) and complementary satellite items.

```
┌────────────────────────────────────────────────────────┐
│                      BENTO GRID                        │
├───────────────────────────────┬────────────────────────┤
│ [1] HERO FEATURE CARD         │ [2] LIVE DATA STAT     │
│ (col-span-8, row-span-2)      │ (col-span-4)           │
│ • Large headline              ├────────────────────────┤
│ • Interactive visualization   │ [3] MINI QUICK ACTION  │
│ • Subtle inner gradient       │ (col-span-4)           │
├───────────────────────────────┴────────────────────────┤
│ [4] SPAN-4 CARD   │ [5] SPAN-4 CARD   │ [6] SPAN-4     │
└───────────────────┴───────────────────┴────────────────┘
```

### Production Tailwind Bento Grid Implementation
```tsx
export function ModernBentoGrid() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-12 gap-4 max-w-7xl mx-auto p-4 sm:p-6 lg:p-8">
      {/* 1. Large Hero Bento Card (Spans 8 cols on desktop) */}
      <div className="md:col-span-8 relative overflow-hidden rounded-3xl bg-gradient-to-b from-neutral-900/90 to-neutral-900/50 p-8 border border-neutral-800 shadow-2xl backdrop-blur-xl group hover:border-neutral-700 transition-all duration-300">
        <div className="absolute top-0 right-0 -mr-16 -mt-16 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none group-hover:bg-indigo-500/20 transition-all" />
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
          Core Performance
        </span>
        <h3 className="text-2xl sm:text-3xl font-semibold text-white tracking-tight mt-4">
          Real-Time Streaming Intelligence
        </h3>
        <p className="text-neutral-400 mt-2 text-sm max-w-lg leading-relaxed">
          Process millions of data events per second with sub-millisecond edge latency.
        </p>
        <div className="mt-8 h-48 w-full bg-neutral-950/60 rounded-2xl border border-neutral-800/80 flex items-center justify-center text-neutral-500 text-sm">
          [Interactive Metric Sparkline]
        </div>
      </div>

      {/* 2. Top-Right Satellite Bento Card (Spans 4 cols) */}
      <div className="md:col-span-4 rounded-3xl bg-neutral-900/60 p-6 border border-neutral-800 backdrop-blur-lg flex flex-col justify-between hover:border-neutral-700 transition-all duration-300">
        <div>
          <span className="text-xs uppercase tracking-wider text-neutral-500 font-semibold">Active Nodes</span>
          <div className="text-4xl font-bold text-white mt-2 font-mono tracking-tight">
            1,429<span className="text-emerald-400 text-lg font-normal ml-1">/1,500</span>
          </div>
        </div>
        <div className="mt-6 flex items-center gap-2 text-xs text-emerald-400">
          <span className="inline-block w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          99.99% Global Uptime
        </div>
      </div>

      {/* 3. Bottom Card 1 */}
      <div className="md:col-span-4 rounded-3xl bg-neutral-900/60 p-6 border border-neutral-800 backdrop-blur-lg hover:border-neutral-700 transition-all">
        <h4 className="text-lg font-medium text-white">Instant Sync</h4>
        <p className="text-neutral-400 text-xs mt-1">Zero configuration required</p>
      </div>

      {/* 4. Bottom Card 2 */}
      <div className="md:col-span-4 rounded-3xl bg-neutral-900/60 p-6 border border-neutral-800 backdrop-blur-lg hover:border-neutral-700 transition-all">
        <h4 className="text-lg font-medium text-white">End-to-End Cryptography</h4>
        <p className="text-neutral-400 text-xs mt-1">Hardware security modules</p>
      </div>

      {/* 5. Bottom Card 3 */}
      <div className="md:col-span-4 rounded-3xl bg-neutral-900/60 p-6 border border-neutral-800 backdrop-blur-lg hover:border-neutral-700 transition-all">
        <h4 className="text-lg font-medium text-white">Automated Backups</h4>
        <p className="text-neutral-400 text-xs mt-1">Hourly point-in-time recovery</p>
      </div>
    </div>
  );
}
```

---

## 2. Glassmorphism & The 1px Hairline Highlight

Genuine frosted glass requires two visual cues: **optical blur** and **light reflection at the border edge**.

```tsx
// Modern Frosted Glass Card
<div className="relative rounded-2xl bg-white/70 dark:bg-neutral-900/70 backdrop-blur-xl border border-white/20 dark:border-white/10 shadow-[0_8px_32px_0_rgba(0,0,0,0.12)] p-6">
  {/* Content */}
</div>
```

### The Layered Glow Trick
To create the subtle luminous border seen on top Linear/Vercel cards, add a pseudo-gradient wrapper:
```tsx
<div className="relative p-[1px] rounded-2xl bg-gradient-to-b from-white/20 via-neutral-800/40 to-transparent">
  <div className="rounded-[15px] bg-neutral-950 p-6">
    {/* Inner Card Content */}
  </div>
</div>
```

---

## 3. Deep Space & OLED Dark Mode Mastery

Never use muddy gray (`#444444`) as a dark mode background.
- **Root Screen Background**: Pitch-Black (`#000000`) or Deep Space Zinc (`#09090b` / `#0B0F19`).
- **Elevated Surface / Card**: Tinted Zinc (`#121215` / `#18181b`).
- **Card Borders**: Crisp dark border (`border-neutral-800` or `rgba(255, 255, 255, 0.08)`).
- **Foreground Primary Text**: Pure high-contrast white (`#FFFFFF` or `#F8FAFC`).
- **Secondary Caption Text**: Slate/Zinc muted (`#94A3B8` / `#A1A1AA`) ensuring a minimum 4.5:1 contrast against the surface.

---

## 4. The Anti-AI Slop Manifesto

AI-generated web interfaces have developed predictable, sterile clichés that users instantly recognize and distrust. **Strictly avoid the following**:

1. **NO Generic Purple-to-Cyan Gradients**: Avoid default `#8B5CF6` to `#06B6D4` washes across every title. Use monochromatic elegance, warm copper, emerald accents, or subtle amber blooms.
2. **NO Identical "SaaS Pricing Cards" with Muddy Shadows**: Never build 3 identical rounded boxes with gray `box-shadow: 0 4px 20px rgba(0,0,0,0.1)`. Use differentiated border accents, distinct visual weights, and clean hairline strokes.
3. **NO Cliché Hero Copy**: Ban phrases like *"Supercharge your workflow with AI-driven synergy"*. Use concrete, user-centric, active-voice descriptions.
4. **NO Emojis as System Navigation Icons**: Emojis (🚀, 💡, 🔥) look amateurish in navigation. Always use crisp, consistent vector SVG icons (Phosphor, Lucide, or Heroicons).
