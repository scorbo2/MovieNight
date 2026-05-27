import { useQuery } from '@tanstack/react-query';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { getGroup, getGroupAncestorChain, listGroups } from '../../api/groups';
import { listGroupItems } from '../../api/items';
import { getThumbnailUrl } from '../../api/thumbnails';
import { buildApiUrl } from '../../api/client';
import { Breadcrumbs } from '../../components/shared/Breadcrumbs';
import { EmptyState } from '../../components/shared/EmptyState';
import { ErrorState } from '../../components/shared/ErrorState';
import { PaginationControls } from '../../components/shared/PaginationControls';
import { SearchBar } from '../../components/shared/SearchBar';
import { TagPills } from '../../components/shared/TagPills';
import { Thumbnail } from '../../components/shared/Thumbnail';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';
import { getNullableStringParam, getPositiveIntParam, setParam } from '../../lib/url';

export function GroupDetailPage(): JSX.Element {
  const { groupId: groupIdParam } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const groupId = Number(groupIdParam);
  const childPage = getPositiveIntParam(searchParams.get('childPage'), 1);
  const childPageSize = getPositiveIntParam(searchParams.get('childPageSize'), 12);
  const itemPage = getPositiveIntParam(searchParams.get('itemPage'), 1);
  const itemPageSize = getPositiveIntParam(searchParams.get('itemPageSize'), 12);
  const childFilters = {
    pageNumber: childPage,
    pageSize: childPageSize,
    parentGroupId: groupId,
    titleContains: getNullableStringParam(searchParams.get('childTitleContains')),
    descriptionContains: getNullableStringParam(searchParams.get('childDescriptionContains')),
  };
  const itemFilters = {
    pageNumber: itemPage,
    pageSize: itemPageSize,
    titleContains: getNullableStringParam(searchParams.get('titleContains')),
    descriptionContains: getNullableStringParam(searchParams.get('descriptionContains')),
    tagContains: getNullableStringParam(searchParams.get('tagContains')),
  };

  const groupQuery = useQuery({ queryKey: ['group', groupId], queryFn: () => getGroup(groupId) });
  const childGroupsQuery = useQuery({ queryKey: ['groups', childFilters], queryFn: () => listGroups(childFilters) });
  const itemsQuery = useQuery({ queryKey: ['group-items', groupId, itemFilters], queryFn: () => listGroupItems(groupId, itemFilters) });
  const itemCountQuery = useQuery({
    queryKey: ['group-items', groupId, { pageNumber: 1, pageSize: 1, scope: 'count-only' }],
    queryFn: () => listGroupItems(groupId, { pageNumber: 1, pageSize: 1 }),
  });
  const breadcrumbQuery = useQuery({
    queryKey: ['group-ancestors', groupId, groupQuery.data?.parentGroupId ?? null],
    queryFn: () => getGroupAncestorChain(groupQuery.data?.parentGroupId ?? null),
    enabled: Boolean(groupQuery.data),
  });

  const updateParams = (updater: (next: URLSearchParams) => void): void => {
    const next = new URLSearchParams(searchParams);
    updater(next);
    setSearchParams(next);
  };

  if (groupQuery.isError) {
    return <ErrorState message={groupQuery.error instanceof Error ? groupQuery.error.message : 'Could not load group'} onRetry={() => void groupQuery.refetch()} />;
  }

  if (groupQuery.isLoading || !groupQuery.data) {
    return <Skeleton className="h-72 w-full rounded-xl" />;
  }

  const breadcrumbItems = [
    { label: 'Browse', to: '/browse' },
    ...(breadcrumbQuery.data ?? []).map((group) => ({ label: group.title, to: `/browse/groups/${group.id}` })),
    { label: groupQuery.data.title },
  ];

  return (
    <div className="space-y-8">
      <Breadcrumbs items={breadcrumbItems} />

      <Card>
        <div className="grid gap-6 lg:grid-cols-[320px_minmax(0,1fr)]">
          <Thumbnail
            alt={groupQuery.data.title}
            src={groupQuery.data.hasThumbnail ? getThumbnailUrl('media-groups', groupQuery.data.id) : undefined}
          />
          <div className="space-y-4">
            <div>
              <h1 className="text-3xl font-bold text-content">{groupQuery.data.title}</h1>
              <p className="mt-2 text-content-secondary">{groupQuery.data.description ?? 'No description available for this group.'}</p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Button
                onClick={() => window.location.assign(buildApiUrl(`playlist/media-group/${groupId}`))}
                disabled={(itemCountQuery.data?.totalCount ?? 0) === 0}
              >
                Watch all in VLC
              </Button>
              {groupQuery.data.parentGroupId ? (
                <Link to={`/browse/groups/${groupQuery.data.parentGroupId}`}>
                  <Button variant="secondary">Back to parent</Button>
                </Link>
              ) : null}
            </div>
          </div>
        </div>
      </Card>

      <section className="space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="section-title">Child groups</h2>
            <p className="section-copy mt-1">Refine the collection tree without losing your place.</p>
          </div>
        </div>
        <Card>
          <SearchBar
            values={{ titleContains: childFilters.titleContains, descriptionContains: childFilters.descriptionContains }}
            showDescription
            onSearch={(values) =>
              updateParams((next) => {
                setParam(next, 'childTitleContains', values.titleContains?.trim());
                setParam(next, 'childDescriptionContains', values.descriptionContains?.trim());
                next.set('childPage', '1');
                next.set('childPageSize', String(childPageSize));
              })
            }
          />
        </Card>
        {childGroupsQuery.isError ? (
          <ErrorState message={childGroupsQuery.error instanceof Error ? childGroupsQuery.error.message : 'Could not load child groups'} onRetry={() => void childGroupsQuery.refetch()} />
        ) : childGroupsQuery.isLoading ? (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {Array.from({ length: 3 }).map((_, index) => (
              <Skeleton key={index} className="aspect-video w-full rounded-xl" />
            ))}
          </div>
        ) : childGroupsQuery.data && childGroupsQuery.data.items.length > 0 ? (
          <>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {childGroupsQuery.data.items.map((childGroup) => (
                <Link key={childGroup.id} className="no-underline" to={`/browse/groups/${childGroup.id}`}>
                  <Card clickable>
                    <Thumbnail
                      alt={childGroup.title}
                      src={childGroup.hasThumbnail ? getThumbnailUrl('media-groups', childGroup.id) : undefined}
                    />
                    <h3 className="mt-4 text-lg font-semibold text-content">{childGroup.title}</h3>
                    <p className="mt-2 text-sm text-content-secondary">{childGroup.description ?? 'No description available.'}</p>
                  </Card>
                </Link>
              ))}
            </div>
            <PaginationControls
              totalCount={childGroupsQuery.data.totalCount}
              pageNumber={childPage}
              pageSize={childPageSize}
              onPageChange={(nextPage) => updateParams((next) => next.set('childPage', String(nextPage)))}
              onPageSizeChange={(nextPageSize) =>
                updateParams((next) => {
                  next.set('childPage', '1');
                  next.set('childPageSize', String(nextPageSize));
                })
              }
            />
          </>
        ) : (
          <EmptyState title="No child groups" description="This group has no matching child groups right now." />
        )}
      </section>

      <section className="space-y-4">
        <div>
          <h2 className="section-title">Items</h2>
          <p className="section-copy mt-1">Filter titles, descriptions, or tags and jump into playback.</p>
        </div>
        <Card>
          <SearchBar
            values={itemFilters}
            showDescription
            showTag
            onSearch={(values) =>
              updateParams((next) => {
                setParam(next, 'titleContains', values.titleContains?.trim());
                setParam(next, 'descriptionContains', values.descriptionContains?.trim());
                setParam(next, 'tagContains', values.tagContains?.trim());
                next.set('itemPage', '1');
                next.set('itemPageSize', String(itemPageSize));
              })
            }
          />
        </Card>
        {itemsQuery.isError ? (
          <ErrorState message={itemsQuery.error instanceof Error ? itemsQuery.error.message : 'Could not load items'} onRetry={() => void itemsQuery.refetch()} />
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
                  <Thumbnail alt={item.title} src={item.hasThumbnail ? getThumbnailUrl('media-items', item.id) : undefined} />
                  <div>
                    <h3 className="text-lg font-semibold text-content">{item.title}</h3>
                    <p className="mt-2 text-sm text-content-secondary">{item.description ?? 'No description available.'}</p>
                  </div>
                  <TagPills tags={item.tags} onTagClick={(tag) => navigate(`/browse/search?tagContains=${encodeURIComponent(tag)}`)} />
                  <Link to={`/browse/items/${item.id}`}>
                    <Button className="w-full">Open item</Button>
                  </Link>
                </Card>
              ))}
            </div>
            <PaginationControls
              totalCount={itemsQuery.data.totalCount}
              pageNumber={itemPage}
              pageSize={itemPageSize}
              onPageChange={(nextPage) => updateParams((next) => next.set('itemPage', String(nextPage)))}
              onPageSizeChange={(nextPageSize) =>
                updateParams((next) => {
                  next.set('itemPage', '1');
                  next.set('itemPageSize', String(nextPageSize));
                })
              }
            />
          </>
        ) : (
          <EmptyState title="No items found" description="Try broadening the item filters or check another group." />
        )}
      </section>
    </div>
  );
}
