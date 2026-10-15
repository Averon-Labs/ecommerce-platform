# Rule 04: Tailwind CSS & shadcn/ui Mastery

Modern UI engineering in React is built on **Tailwind CSS** and headless accessible primitives like **Radix UI** (popularized by **shadcn/ui**). To achieve a Dribbble-grade look, you must elevate standard Tailwind from raw utility classes into a **structured design token engine**.

---

## 1. The Three-Layer Token Architecture

Never use raw arbitrary Tailwind classes like `bg-[#1a1b1e]` or `text-[#f1f3f5]` across components. Structure tokens into three layers:

```
┌────────────────────────────────────────────────────────┐
│ 1. PRIMITIVE LAYER (Raw Palette Hex Values)            │
│    zinc-950: #09090b, indigo-500: #6366f1, etc.        │
├────────────────────────────────────────────────────────┤
│ 2. SEMANTIC LAYER (CSS Variables)                      │
│    --background, --foreground, --card, --primary       │
├────────────────────────────────────────────────────────┤
│ 3. COMPONENT LAYER (Tailwind Utility Mapping)          │
│    bg-background, text-foreground, border-border       │
└────────────────────────────────────────────────────────┘
```

---

## 2. Universal Semantic CSS Variable Setup (`globals.css`)

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 240 10% 98%;      /* #F8FAFC Soft Crisp Light */
    --foreground: 222 47% 11%;      /* #0F172A */
    --card: 0 0% 100%;              /* #FFFFFF */
    --card-foreground: 222 47% 11%;
    --primary: 243 75% 59%;         /* #4F46E5 Modern Indigo */
    --primary-foreground: 0 0% 100%;
    --muted: 210 40% 96%;           /* #F1F5F9 */
    --muted-foreground: 215 16% 47%;/* #64748B */
    --border: 214 32% 91%;          /* #E2E8F0 */
    --ring: 243 75% 59%;
    --radius: 1rem;
  }

  /* OLED Deep Space Dark Mode */
  .dark {
    --background: 240 10% 4%;       /* #09090B Pitch-Black Tint */
    --foreground: 0 0% 98%;         /* #FAFAFA High-Contrast White */
    --card: 240 10% 7%;             /* #121215 Tinted Zinc Surface */
    --card-foreground: 0 0% 98%;
    --primary: 239 84% 67%;         /* #6366F1 Luminous Indigo */
    --primary-foreground: 0 0% 100%;
    --muted: 240 4% 16%;            /* #27272A */
    --muted-foreground: 240 5% 65%; /* #A1A1AA */
    --border: 240 4% 16%;           /* #27272A Clean Hairline Stroke */
    --ring: 239 84% 67%;
  }
}
```

---

## 3. High-Craft Dribbble Utilities in Tailwind

Extend `tailwind.config.ts` with custom utilities that produce signature luxury visuals:

```ts
import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        border: "hsl(var(--border))",
        ring: "hsl(var(--ring))",
      },
      borderRadius: {
        "2xl": "1rem",
        "3xl": "1.5rem",
        "4xl": "2rem",
      },
      boxShadow: {
        glow: "0 0 40px -10px rgba(99, 102, 241, 0.3)",
        glass: "0 8px 32px 0 rgba(0, 0, 0, 0.12)",
      },
      animation: {
        "pulse-subtle": "pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite",
      },
    },
  },
  plugins: [],
};

export default config;
```

---

## 4. Elevating shadcn/ui Primitives

When installing or modifying shadcn/ui components (such as Button, Dialog, Card), make these three adjustments to convert standard enterprise UI into Dribbble-level craft:

1. **Increase Border Radius**: Shift default `rounded-md` to `rounded-2xl` on cards and `rounded-full` on pill buttons.
2. **Subtle Gradient Borders**: Replace flat 1px borders with a gradient overlay (`from-white/20 via-white/5 to-transparent` in dark mode).
3. **Hover Depth Lifts**: Add `hover:-translate-y-0.5 transition-transform duration-200 active:translate-y-0`.
