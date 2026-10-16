import type { ReactNode } from 'react';
import { statusBadgeBase } from '../lib/ui';

export type StatusTone = 'success' | 'warning' | 'error' | 'neutral';

/**
 * Status text plus container color from the design-system status semantics
 * table (docs/DESIGN_SYSTEM.md): success #256B4B/#E8F3ED, warning
 * #8A5300/#FBF1E3, error #B3261E/#FCEEEE, neutral #505050/#EFF0F2, with the
 * documented high-contrast dark variants. The text label is always rendered;
 * color never carries meaning alone.
 */
const TONE_CLASSES: Record<StatusTone, string> = {
  success: 'bg-[#E8F3ED] text-[#256B4B] dark:bg-[#0F2A1E] dark:text-[#7FD3A8]',
  warning: 'bg-[#FBF1E3] text-[#8A5300] dark:bg-[#33240F] dark:text-[#E8B768]',
  error: 'bg-[#FCEEEE] text-[#B3261E] dark:bg-[#3A1512] dark:text-[#FFB4AB]',
  neutral: 'bg-[#EFF0F2] text-[#505050] dark:bg-[#1A1C20] dark:text-[#C4C6D0]',
};

export function StatusBadge({ tone, children }: { tone: StatusTone; children: ReactNode }) {
  return <span className={`${statusBadgeBase} ${TONE_CLASSES[tone]}`}>{children}</span>;
}
