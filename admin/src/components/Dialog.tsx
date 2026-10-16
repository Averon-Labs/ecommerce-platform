import { useEffect, useId, useRef } from 'react';
import type { ReactNode } from 'react';

interface DialogProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  widthClassName?: string;
}

const FOCUSABLE_SELECTOR =
  'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

/**
 * Controlled modal dialog (docs/DESIGN_SYSTEM.md — dialogs).
 *
 * Escape and the backdrop close the dialog, focus moves into the dialog when
 * it opens and returns to the trigger when it closes, and Tab stays inside
 * the dialog while it is open. The dialog is a UI affordance only — the API
 * revalidates every action it confirms.
 */
export function Dialog({
  open,
  title,
  onClose,
  children,
  footer,
  widthClassName = 'max-w-lg',
}: DialogProps) {
  const titleId = useId();
  const surfaceRef = useRef<HTMLDivElement>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);
  const onCloseRef = useRef(onClose);

  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    if (!open) {
      return;
    }

    previouslyFocused.current =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const surface = surfaceRef.current;
    const firstFocusable = surface?.querySelector<HTMLElement>(FOCUSABLE_SELECTOR);
    (firstFocusable ?? surface)?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onCloseRef.current();
        return;
      }

      if (event.key !== 'Tab' || !surface) {
        return;
      }

      const items = Array.from(surface.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
        (element) => !element.hasAttribute('disabled'),
      );

      if (items.length === 0) {
        event.preventDefault();
        return;
      }

      const first = items[0];
      const last = items[items.length - 1];

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      previouslyFocused.current?.focus();
    };
  }, [open]);

  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="fixed inset-0 animate-fade-in bg-black/70 backdrop-blur-md"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        ref={surfaceRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className={`relative z-10 max-h-[90vh] w-full overflow-y-auto ${widthClassName} animate-scale-in rounded-3xl border border-border bg-card p-6 shadow-glass focus-visible:outline-none sm:p-8`}
      >
        <h2 id={titleId} className="text-lg font-bold tracking-tight">
          {title}
        </h2>
        <div className="mt-4">{children}</div>
        {footer && <div className="mt-6 flex flex-wrap items-center justify-end gap-3">{footer}</div>}
      </div>
    </div>
  );
}
