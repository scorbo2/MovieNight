import { forwardRef } from 'react';
import { Button, type ButtonProps } from './Button';
import { cn } from '../../lib/cn';

export interface IconButtonProps extends Omit<ButtonProps, 'children'> {
  icon: React.ReactNode;
  srLabel: string;
}

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton(
  { icon, srLabel, className, size = 'md', variant = 'ghost', ...props },
  ref,
) {
  const dimension = size === 'sm' ? 'h-9 w-9' : size === 'lg' ? 'h-11 w-11' : 'h-10 w-10';

  return (
    <Button
      ref={ref}
      className={cn('p-0', dimension, className)}
      size={size}
      variant={variant}
      {...props}
    >
      <span aria-hidden="true">{icon}</span>
      <span className="sr-only">{srLabel}</span>
    </Button>
  );
});
