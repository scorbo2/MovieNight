import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { getThumbnailUrl } from '../../api/thumbnails';
import { listGroups } from '../../api/groups';
import { EmptyState } from '../../components/shared/EmptyState';
import { ErrorState } from '../../components/shared/ErrorState';
import { PaginationControls } from '../../components/shared/PaginationControls';
import { SearchBar } from '../../components/shared/SearchBar';
import { Thumbnail } from '../../components/shared/Thumbnail';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';
import { getNullableStringParam, getPositiveIntParam, setParam } from '../../lib/url';

export function BrowseHomePage(): JSX.Element {
  const [searchParams, setSearchParams] = useSearchParams();
  const pageNumber = getPositiveIntParam(searchParams.get('pageNumber'), 1);
  const pageSize = getPositiveIntParam(searchParams.get('pageSize'), 12);
  const filters = {
    pageNumber,
    pageSize,
    topLevelOnly: true,
    titleContains: getNullableStringParam(searchParams.get('titleContains')),
    descriptionContains: getNullableStringParam(searchParams.get('descriptionContains')),
  };

  const groupsQuery = useQuery({
    queryKey: ['groups', filters],
    queryFn: () => listGroups(filters),
  });

  const updateParams = (updater: (next: URLSearchParams) => void): void => {
    const next = new URLSearchParams(searchParams);
    updater(next);
    setSearchParams(next);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-content">Browse the catalog</h1>
        <p className="mt-2 max-w-2xl text-content-secondary">Discover media groups, drill into collections, and jump straight into playback.</p>
      </div>

      <Card>
        <SearchBar
          values={filters}
          showDescription
          onSearch={(values) =>
            updateParams((next) => {
              setParam(next, 'titleContains', values.titleContains?.trim());
              setParam(next, 'descriptionContains', values.descriptionContains?.trim());
              next.set('pageNumber', '1');
              next.set('pageSize', String(pageSize));
            })
          }
        />
      </Card>

      {groupsQuery.isError ? (
        <ErrorState message={groupsQuery.error instanceof Error ? groupsQuery.error.message : 'Could not load groups'} onRetry={() => void groupsQuery.refetch()} />
      ) : groupsQuery.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <Card key={index}>
              <Skeleton className="aspect-video w-full rounded-lg" />
              <Skeleton className="mt-4 h-6 w-2/3" />
              <Skeleton className="mt-2 h-4 w-full" />
            </Card>
          ))}
        </div>
      ) : groupsQuery.data && groupsQuery.data.items.length > 0 ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {groupsQuery.data.items.map((group) => (
              <Link key={group.id} className="no-underline" to={`/browse/groups/${group.id}`}>
                <Card clickable className="h-full">
                  <Thumbnail
                    alt={group.title}
                    src={group.hasThumbnail ? getThumbnailUrl('media-groups', group.id) : undefined}
                  />
                  <h2 className="mt-4 text-xl font-semibold text-content">{group.title}</h2>
                  <p className="mt-2 text-sm text-content-secondary">{group.description ?? 'No description available.'}</p>
                </Card>
              </Link>
            ))}
          </div>
          <PaginationControls
            totalCount={groupsQuery.data.totalCount}
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
        </>
      ) : (
        <EmptyState title="No groups found" description="Try adjusting the title or description filters." />
      )}
    </div>
  );
}
