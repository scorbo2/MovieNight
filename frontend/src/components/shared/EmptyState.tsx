import { Card } from '../ui/Card';

interface EmptyStateProps {
  title: string;
  description: string;
  action?: React.ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps): JSX.Element {
  return (
    <Card className="flex flex-col items-center gap-3 py-10 text-center">
      <div className="rounded-full bg-bg-subtle p-4 text-content-secondary">
        <svg className="h-8 w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M4 7.5a2.5 2.5 0 0 1 2.5-2.5h11A2.5 2.5 0 0 1 20 7.5v9A2.5 2.5 0 0 1 17.5 19h-11A2.5 2.5 0 0 1 4 16.5v-9Z" />
          <path d="M8 10h8M8 14h5" />
        </svg>
      </div>
      <div>
        <h3 className="text-lg font-semibold text-content">{title}</h3>
        <p className="mt-1 text-sm text-content-secondary">{description}</p>
      </div>
      {action}
    </Card>
  );
}
