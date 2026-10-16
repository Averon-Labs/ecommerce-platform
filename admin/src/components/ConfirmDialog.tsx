import { Dialog } from './Dialog';
import { dangerButtonClass, secondaryButtonClass } from '../lib/ui';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/** Destructive-action confirmation following the shared dialog behavior. */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Dialog
      open={open}
      title={title}
      onClose={busy ? () => undefined : onCancel}
      footer={
        <>
          <button
            type="button"
            className={secondaryButtonClass}
            onClick={onCancel}
            disabled={busy}
          >
            Cancel
          </button>
          <button
            type="button"
            className={dangerButtonClass}
            onClick={onConfirm}
            disabled={busy}
            aria-busy={busy}
          >
            {busy ? 'Working…' : confirmLabel}
          </button>
        </>
      }
    >
      <p className="text-sm text-muted-foreground">{description}</p>
    </Dialog>
  );
}
