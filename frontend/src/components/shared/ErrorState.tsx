import { Button } from '../ui/Button';
import { Card } from '../ui/Card';

interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

export function ErrorState({ title = 'Something went wrong', message, onRetry }: ErrorStateProps): JSX.Element {
  return (
    <Card className="border-danger/30 bg-danger/5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-content">{title}</h3>
          <p className="mt-1 text-sm text-content-secondary">{message}</p>
        </div>
        {onRetry ? (
          <Button variant="danger" onClick={onRetry}>
            Retry
          </Button>
        ) : null}
      </div>
    </Card>
  );
}
