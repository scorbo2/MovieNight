import { cn } from '../../lib/cn';

export type ToastTone = 'success' | 'error' | 'info' | 'warning';

export interface ToastMessage {
  id: string;
  title: string;
  description?: string;
  tone: ToastTone;
}

const toneStyles: Record<ToastTone, string> = {
  success: 'border-success/40 bg-success/10 text-content',
  error: 'border-danger/40 bg-danger/10 text-content',
  info: 'border-brand/40 bg-brand/10 text-content',
  warning: 'border-warning/40 bg-warning/10 text-content',
};

interface ToastProps {
  toast: ToastMessage;
  onDismiss: (id: string) => void;
}

export function Toast({ toast, onDismiss }: ToastProps): JSX.Element {
  return (
    <div className={cn('w-full rounded-lg border p-4 shadow-lg', toneStyles[toast.tone])}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-semibold">{toast.title}</p>
          {toast.description ? <p className="mt-1 text-sm text-content-secondary">{toast.description}</p> : null}
        </div>
        <button type="button" className="text-sm text-content-secondary hover:text-content" onClick={() => onDismiss(toast.id)}>
          Dismiss
        </button>
      </div>
    </div>
  );
}
