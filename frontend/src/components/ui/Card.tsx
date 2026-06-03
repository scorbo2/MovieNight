import { cn } from '../../lib/cn';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  clickable?: boolean;
}

export function Card({ className, clickable = false, ...props }: CardProps): JSX.Element {
  return (
    <div
      className={cn(
        'rounded-lg border border-card-border bg-card-bg p-5 shadow-sm',
        clickable && 'transition-transform hover:-translate-y-px hover:shadow-md',
        className,
      )}
      {...props}
    />
  );
}
