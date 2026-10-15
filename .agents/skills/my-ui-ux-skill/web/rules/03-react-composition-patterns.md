# Rule 03: React Architecture & Composition Patterns

When React components grow, developers often add boolean flags: `isModal`, `hasHeader`, `isCompact`, `showFooter`, `isEditing`, `isAdmin`. With $N$ booleans, you create $2^N$ combinatorial states, resulting in brittle spaghetti code and hard-to-maintain components.

Vercel Engineering enforces **Composition over Configuration** using the **Compound Component Pattern** and a clean **Three-Pillar Context Interface**.

---

## 1. The Anti-Pattern: Boolean Prop Explosion

```tsx
// ❌ WRONG: The Monolithic Boolean Monster
<UserProfileCard
  user={user}
  isCompact={false}
  showActions={true}
  hasBadge={true}
  isAdmin={false}
  renderHeader={() => <CustomHeader />}
/>
```
Every new feature request adds another prop, cluttering the API and leading to impossible UI states.

---

## 2. The Solution: Compound Components

Instead of one monolithic component with 20 configuration props, export cohesive subcomponents that communicate implicitly via React Context:

```tsx
// ✅ CORRECT: Composable Compound Primitives
<Card>
  <Card.Header>
    <Card.Title>Account Settings</Card.Title>
    <Card.Badge variant="success">Verified</Card.Badge>
  </Card.Header>
  <Card.Content>
    <UserForm user={user} />
  </Card.Content>
  <Card.Footer>
    <Button variant="ghost">Cancel</Button>
    <Button variant="primary">Save Changes</Button>
  </Card.Footer>
</Card>
```

---

## 3. The Three-Pillar Context Interface (`state`, `actions`, `meta`)

To keep compound subcomponents completely decoupled and testable, standardize the Context shape into three distinct pillars:

```tsx
interface ComponentContextType<T> {
  // 1. STATE: Pure reactive data
  state: {
    isOpen: boolean;
    selectedId: string | null;
    isLoading: boolean;
  };
  // 2. ACTIONS: Stable dispatchers/mutators
  actions: {
    open: () => void;
    close: () => void;
    select: (id: string) => void;
  };
  // 3. META: Auxiliary DOM nodes, refs, or environmental metadata
  meta: {
    triggerRef: React.RefObject<HTMLButtonElement | null>;
    id: string;
  };
}
```

---

## 4. Complete Modern Implementation (React 19 Ready)

Notice how in modern React 19, `forwardRef` is no longer required (props include `ref` directly) and `use()` can be called conditionally:

```tsx
// components/Dialog.tsx
import React, { createContext, use, useState, useRef, useId } from 'react';

interface DialogContextValue {
  state: { isOpen: boolean };
  actions: { open: () => void; close: () => void };
  meta: { id: string; triggerRef: React.RefObject<HTMLButtonElement | null> };
}

const DialogContext = createContext<DialogContextValue | null>(null);

function useDialogContext() {
  const context = use(DialogContext);
  if (!context) {
    throw new Error('Dialog compound subcomponents must be used within <Dialog>');
  }
  return context;
}

// 1. Root Provider
export function Dialog({ children, defaultOpen = false }: { children: React.ReactNode; defaultOpen?: boolean }) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const id = useId();

  const value: DialogContextValue = {
    state: { isOpen },
    actions: {
      open: () => setIsOpen(true),
      close: () => {
        setIsOpen(false);
        triggerRef.current?.focus(); // Return focus to trigger upon close
      },
    },
    meta: { id, triggerRef },
  };

  return <DialogContext.Provider value={value}>{children}</DialogContext.Provider>;
}

// 2. Trigger
Dialog.Trigger = function DialogTrigger({ children, className }: { children: React.ReactNode; className?: string }) {
  const { actions, meta } = useDialogContext();
  return (
    <button
      ref={meta.triggerRef}
      type="button"
      onClick={actions.open}
      aria-haspopup="dialog"
      className={className}
    >
      {children}
    </button>
  );
};

// 3. Content Panel
Dialog.Content = function DialogContent({ children, className }: { children: React.ReactNode; className?: string }) {
  const { state, actions, meta } = useDialogContext();
  if (!state.isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity" 
        onClick={actions.close} 
      />
      {/* Modal Surface */}
      <div
        role="dialog"
        id={meta.id}
        aria-modal="true"
        className={`relative z-10 w-full max-w-lg rounded-2xl bg-neutral-900 border border-neutral-800 p-6 shadow-2xl ${className}`}
      >
        {children}
      </div>
    </div>
  );
};
```

---

## 5. Architectural Benefits

1. **No prop drilling**: Subcomponents consume state directly from context.
2. **Zero impossible states**: You cannot accidentally render a card footer without a card root.
3. **Flexible visual hierarchy**: Consumers can place the close button inside the header, in the footer, or as an absolute floating icon without rewriting internal component logic.
