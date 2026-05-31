import { useQuery } from '@tanstack/react-query';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { listItems } from '../../api/items';
import { getThumbnailUrl } from '../../api/thumbnails';
import { EmptyState } from '../../components/shared/EmptyState';
import { ErrorState } from '../../components/shared/ErrorState';
import { PaginationControls } from '../../components/shared/PaginationControls';
import { SearchBar } from '../../components/shared/SearchBar';
import { RecentlyWatchedBadge } from '../../components/shared/RecentlyWatchedBadge';
import { TagPills } from '../../components/shared/TagPills';
import { Thumbnail } from '../../components/shared/Thumbnail';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';
import { getNullableStringParam, getPositiveIntParam, setParam } from '../../lib/url';

export function SearchResultsPage(): JSX.Element {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const pageNumber = getPositiveIntParam(searchParams.get('pageNumber'), 1);
  const pageSize = getPositiveIntParam(searchParams.get('pageSize'), 12);
  const filters = {
    pageNumber,
    pageSize,
    titleContains: getNullableStringParam(searchParams.get('titleContains')),
    descriptionContains: getNullableStringParam(searchParams.get('descriptionContains')),
    tagContains: getNullableStringParam(searchParams.get('tagContains')),
  };

  const itemsQuery = useQuery({ queryKey: ['items', filters], queryFn: () => listItems(filters) });

  const updateParams = (updater: (next: URLSearchParams) => void): void => {
    const next = new URLSearchParams(searchParams);
    updater(next);
    setSearchParams(next);
  };

  return (
    <div className="space-y-6">
      <Card>
        <SearchBar
          values={filters}
          showDescription
          showTag
          onSearch={(values) =>
            updateParams((next) => {
              setParam(next, 'titleContains', values.titleContains?.trim());
              setParam(next, 'descriptionContains', values.descriptionContains?.trim());
              setParam(next, 'tagContains', values.tagContains?.trim());
              next.set('pageNumber', '1');
              next.set('pageSize', String(pageSize));
            })
          }
        />
      </Card>

      {itemsQuery.isError ? (
        <ErrorState message={itemsQuery.error instanceof Error ? itemsQuery.error.message : 'Could not load search results'} onRetry={() => void itemsQuery.refetch()} />
      ) : itemsQuery.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <Skeleton key={index} className="aspect-video w-full rounded-xl" />
          ))}
        </div>
      ) : itemsQuery.data && itemsQuery.data.items.length > 0 ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {itemsQuery.data.items.map((item) => (
              <Card key={item.id} className="space-y-4">
                <div className="relative">
                  <Thumbnail alt={item.title} src={item.hasThumbnail ? getThumbnailUrl('media-items', item.id) : undefined} />
                  {item.recentlyWatched && <RecentlyWatchedBadge />}
                </div>
                <div>
                  <h2 className="text-lg font-semibold text-content">{item.title}</h2>
                  <p className="mt-2 text-sm text-content-secondary">{item.description ?? 'No description available.'}</p>
                </div>
                <TagPills tags={item.tags} onTagClick={(tag) => navigate(`/browse/search?tagContains=${encodeURIComponent(tag)}`)} />
                <Link to={`/browse/items/${item.id}`} className="block">
                  <span className="inline-flex rounded-md bg-brand px-4 py-2 text-sm font-medium text-white">View item</span>
                </Link>
              </Card>
            ))}
          </div>
          {itemsQuery.data.totalCount > pageSize ? (
            <PaginationControls
              totalCount={itemsQuery.data.totalCount}
              pageNumber={pageNumber}
              pageSize={pageSize}
              onPageChange={(nextPage) => updateParams((next) => next.set('pageNumber', String(nextPage)))}
              onPageSizeChange={(nextPageSize) =>
                updateParams((next) => {
                  next.set('pageNumber', '1');
                  next.set('pageSize', String(nextPageSize));
                })
              }
            />
          ) : null}
        </>
      ) : (
        <EmptyState title="No matching items" description="Try broader search terms or a different tag." />
      )}
    </div>
  );
}
