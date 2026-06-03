import { forwardRef } from 'react';
import { cn } from '../../lib/cn';

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  error?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { className, error, ...props },
  ref,
) {
  return (
    <textarea
      ref={ref}
      className={cn(
        'min-h-[120px] w-full rounded-md border bg-input-bg px-3 py-2 text-sm text-content placeholder:text-[rgb(var(--color-input-placeholder))] shadow-sm transition-colors',
        error ? 'border-danger' : 'border-input-border',
        className,
      )}
      {...props}
    />
  );
});
