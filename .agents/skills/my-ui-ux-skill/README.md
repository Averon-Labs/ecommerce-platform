# Comprehensive UI/UX Skill for Android and Web (My UI/UX Skill)

This skill suite synthesizes and distills best practices from four major agentic design systems:
1. **`material-3-skill`** (Google's official design system)
2. **`ui-ux-pro-max-skill`** (Trending Dribbble styles and modern palettes)
3. **`VERCELL-agent-skills`** (Frontend engineering standards and component architecture)
4. **`all-skills / frontend-design`** (Authentic typography principles and anti-AI-slop design)

---

## Package Structure:

### 1. Android Specialist Skill (`android/`)
* **Target Audience:** Android developers using **Java** and **XML Layouts**.
* **Aesthetics:** Modern Bento Grid cards, Glassmorphism effects, floating navigation bar, and subtle gradients.
* **Engineering & UX:**
  * Strict adherence to all 29 Material Design 3 (Material You) color roles.
  * Full Android 15 Edge-to-Edge support with keyboard and status bar insets handling.
  * Mandatory 48x48dp minimum touch target sizing per WCAG 2.2.
  * Prevention of frame drops by eliminating nested hierarchies and utilizing `ConstraintLayout Flow`.
  * Physics-based spring animations (`SpringAnimation`) in Java.
* **Pre-built Assets:** Includes ready-to-use templates for `colors.xml`, `dimens.xml`, `type.xml`, and vector shapes in `drawables/`.

---

### 2. Web Specialist Skill (`web/`)
* **Target Audience:** Web developers using **React**, **Tailwind CSS**, and **shadcn/ui**.
* **Aesthetics:** Asymmetric Bento Grid design, deep OLED dark mode (no muddy grays), 1px crisp crystal borders, and anti-AI slop design.
* **Engineering & UX:**
  * Zero Cumulative Layout Shift (Zero CLS with reserved aspect ratios and matched skeletons).
  * Accessible keyboard focus rings (`focus-visible`) without stripping outlines.
  * Tabular numbers (`tabular-nums`) to prevent jitter in prices, counters, and tables.
  * Compound component architecture with triple pattern `{ state, actions, meta }`.
  * Smooth 60fps animations utilizing browser native View Transitions.
* **Pre-built Assets:** Includes TypeScript component templates `BentoGrid.tsx`, `GlassCard.tsx`, `FloatingNav.tsx`, and `CompoundModal.tsx` alongside `tailwind.config.ts` and `globals.css` variables.

---

## Agent Usage Instructions:
Register this folder as a skill in your agentic coding environment (e.g. Antigravity, Claude Code, Cursor). The root `SKILL.md` automatically routes requests to the Android or Web skill modules based on task context.
