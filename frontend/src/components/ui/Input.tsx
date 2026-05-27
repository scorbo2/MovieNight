import { forwardRef } from 'react';
import { cn } from '../../lib/cn';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input({ className, error, ...props }, ref) {
  return (
    <input
      ref={ref}
      className={cn(
        'h-10 w-full rounded-md border bg-input-bg px-3 text-sm text-content placeholder:text-[rgb(var(--color-input-placeholder))] shadow-sm transition-colors',
        error ? 'border-danger' : 'border-input-border',
        className,
      )}
      {...props}
    />
  );
});
