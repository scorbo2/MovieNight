import { forwardRef } from 'react';
import { cn } from '../../lib/cn';

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  error?: boolean;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { className, error, children, ...props },
  ref,
) {
  return (
    <select
      ref={ref}
      className={cn(
        'h-10 w-full rounded-md border bg-input-bg px-3 text-sm text-content shadow-sm transition-colors',
        error ? 'border-danger' : 'border-input-border',
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
});
