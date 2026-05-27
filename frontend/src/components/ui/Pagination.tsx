import { Button } from './Button';

interface PaginationProps {
  totalCount: number;
  pageNumber: number;
  pageSize: number;
  onChange: (pageNumber: number) => void;
}

function getVisiblePages(totalPages: number, currentPage: number): number[] {
  const start = Math.max(1, currentPage - 1);
  const end = Math.min(totalPages, currentPage + 1);
  const pages = new Set<number>([1, totalPages]);

  for (let page = start; page <= end; page += 1) {
    pages.add(page);
  }

  return Array.from(pages).sort((a, b) => a - b);
}

export function Pagination({ totalCount, pageNumber, pageSize, onChange }: PaginationProps): JSX.Element | null {
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  const startResult = totalCount === 0 ? 0 : (pageNumber - 1) * pageSize + 1;
  const endResult = Math.min(pageNumber * pageSize, totalCount);

  if (totalPages <= 1 && totalCount <= pageSize) {
    return <p className="text-sm text-content-secondary">Showing {startResult}-{endResult} of {totalCount} results</p>;
  }

  const pages = getVisiblePages(totalPages, pageNumber);

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-content-secondary">
        Showing {startResult}-{endResult} of {totalCount} results
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="secondary" size="sm" onClick={() => onChange(pageNumber - 1)} disabled={pageNumber <= 1}>
          Previous
        </Button>
        {pages.map((page, index) => (
          <span key={page} className="flex items-center gap-2">
            {index > 0 && page - pages[index - 1] > 1 ? <span className="text-content-muted">…</span> : null}
            <Button
              variant={page === pageNumber ? 'primary' : 'secondary'}
              size="sm"
              onClick={() => onChange(page)}
            >
              {page}
            </Button>
          </span>
        ))}
        <Button variant="secondary" size="sm" onClick={() => onChange(pageNumber + 1)} disabled={pageNumber >= totalPages}>
          Next
        </Button>
      </div>
    </div>
  );
}
