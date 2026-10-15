import React, { createContext, useContext, useState, useRef, useId, useEffect } from 'react';

interface ModalContextValue {
  isOpen: boolean;
  open: () => void;
  close: () => void;
  triggerRef: React.RefObject<HTMLButtonElement | null>;
  id: string;
}

const ModalContext = createContext<ModalContextValue | null>(null);

function useModal() {
  const context = useContext(ModalContext);
  if (!context) {
    throw new Error('Modal subcomponents must be used within <Modal>');
  }
  return context;
}

export function Modal({ children, defaultOpen = false }: { children: React.ReactNode; defaultOpen?: boolean }) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const id = useId();

  const close = () => {
    setIsOpen(false);
    triggerRef.current?.focus();
  };

  const open = () => setIsOpen(true);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) close();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  return (
    <ModalContext.Provider value={{ isOpen, open, close, triggerRef, id }}>
      {children}
    </ModalContext.Provider>
  );
}

Modal.Trigger = function ModalTrigger({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  const { open, triggerRef } = useModal();
  return (
    <button
      ref={triggerRef}
      type="button"
      onClick={open}
      aria-haspopup="dialog"
      className={`inline-flex items-center justify-center rounded-full px-5 py-2.5 text-sm font-medium transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${className}`}
    >
      {children}
    </button>
  );
};

Modal.Content = function ModalContent({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  const { isOpen, close, id } = useModal();
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/70 backdrop-blur-md transition-opacity animate-fade-in"
        onClick={close}
        aria-hidden="true"
      />
      {/* Modal Surface */}
      <div
        role="dialog"
        id={id}
        aria-modal="true"
        className={`relative z-10 w-full max-w-lg rounded-3xl bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-800 p-6 sm:p-8 shadow-2xl animate-scale-in ${className}`}
      >
        {children}
      </div>
    </div>
  );
};

Modal.Header = function ModalHeader({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return <div className={`space-y-1.5 pb-4 border-b border-neutral-200 dark:border-neutral-800 ${className}`}>{children}</div>;
};

Modal.Title = function ModalTitle({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return <h3 className={`text-xl font-semibold text-neutral-900 dark:text-white tracking-tight ${className}`}>{children}</h3>;
};

Modal.Footer = function ModalFooter({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return <div className={`pt-4 mt-6 flex items-center justify-end gap-3 border-t border-neutral-200 dark:border-neutral-800 ${className}`}>{children}</div>;
};
