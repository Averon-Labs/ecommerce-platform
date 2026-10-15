# Rule 05: Fluid Motion & Native View Transitions

Adding heavy animation libraries like Framer Motion or GSAP often adds 40KB+ to JavaScript bundle sizes and causes frame stutter on mobile devices. Modern React web development leverages **Browser-Native View Transitions** and **CSS Compositor-Only Springs**.

---

## 1. Native W3C View Transitions in React

Modern browsers (Chrome 111+, Safari 18+, Firefox 144+) support native View Transitions. This allows browser-rendered shared-element morphing at 60–120fps with zero JavaScript runtime overhead.

### Safe Cross-Browser Wrapper Utility
```tsx
// lib/view-transition.ts
export function executeViewTransition(updateCallback: () => void | Promise<void>) {
  if (typeof document !== 'undefined' && 'startViewTransition' in document) {
    // Native browser snapshot transition
    (document as any).startViewTransition(updateCallback);
  } else {
    // Fallback for older browsers: execute update synchronously
    updateCallback();
  }
}
```

### Route & Shared Element Transition CSS
```css
/* globals.css */
@supports (view-transition-name: none) {
  /* Prevent full-page white flashes */
  ::view-transition-old(root),
  ::view-transition-new(root) {
    animation-duration: 200ms;
    animation-timing-function: cubic-bezier(0.16, 1, 0.3, 1);
  }

  /* Shared Image Morph across views */
  .hero-morph-image {
    view-transition-name: hero-product-image;
  }
}

/* Accessibility: Strictly disable on user preference */
@media (prefers-reduced-motion: reduce) {
  ::view-transition-group(*),
  ::view-transition-old(*),
  ::view-transition-new(*) {
    animation: none !important;
  }
}
```

---

## 2. Compositor-Only Animation Rules

Animating layout-triggering properties forces the browser to recalculate layout geometry and repaint pixels across the entire DOM tree:

| Property | Layout Impact | Safe for 60fps Animation? |
| :--- | :--- | :--- |
| `width` / `height` | **Recalculates Layout** (Jank!) | ❌ **FORBIDDEN** |
| `top` / `left` / `margin` | **Forces Reflow** | ❌ **FORBIDDEN** |
| `transform: scale()` | Handled on GPU Compositor | ✅ **SAFE** |
| `transform: translate3d()` | Handled on GPU Compositor | ✅ **SAFE** |
| `opacity` | Handled on GPU Compositor | ✅ **SAFE** |

---

## 3. Spring Physics Curves

Linear animations (`transition: all 0.3s linear`) feel artificial and robotic. Use the signature **fluid decelerating spring curve**:

```css
/* Ultra-smooth Apple/Linear UI Spring Easing */
--ease-spring: cubic-bezier(0.16, 1, 0.3, 1);

/* Interactive Button Scale Feedback */
.btn-spring {
  transition: transform 180ms cubic-bezier(0.16, 1, 0.3, 1), background-color 150ms ease;
}

.btn-spring:hover {
  transform: translateY(-1px) scale(1.01);
}

.btn-spring:active {
  transform: translateY(1px) scale(0.98);
}
```

---

## 4. Staggered Entrance Animations

When rendering lists or bento grids, stagger item appearances by 40ms to create organic visual choreography:

```tsx
export function StaggerList({ items }: { items: string[] }) {
  return (
    <div className="grid gap-4">
      {items.map((item, index) => (
        <div
          key={item}
          style={{ animationDelay: `${index * 45}ms` }}
          className="animate-fade-in-up fill-mode-backwards"
        >
          {item}
        </div>
      ))}
    </div>
  );
}
```
In Tailwind CSS: define `@keyframes fadeInUp` translating from `translateY(12px)` to `0` with `opacity: 0` to `1`.
