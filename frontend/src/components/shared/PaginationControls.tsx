import { Pagination } from '../ui/Pagination';
import { Select } from '../ui/Select';

interface PaginationControlsProps {
  totalCount: number;
  pageNumber: number;
  pageSize: number;
  onPageChange: (pageNumber: number) => void;
  onPageSizeChange: (pageSize: number) => void;
}

const PAGE_SIZES = [10, 12, 24, 48];

export function PaginationControls({
  totalCount,
  pageNumber,
  pageSize,
  onPageChange,
  onPageSizeChange,
}: PaginationControlsProps): JSX.Element {
  return (
    <div className="flex flex-col gap-4 rounded-lg border border-border-subtle bg-surface p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 text-sm text-content-secondary">
          <span>Results per page</span>
          <Select value={pageSize} className="w-28" onChange={(event) => onPageSizeChange(Number(event.target.value))}>
            {PAGE_SIZES.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </Select>
        </div>
      </div>
      <Pagination totalCount={totalCount} pageNumber={pageNumber} pageSize={pageSize} onChange={onPageChange} />
    </div>
  );
}
