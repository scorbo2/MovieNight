import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { getGroup, getGroupAncestorChain } from '../../api/groups';
import { getItem } from '../../api/items';
import { buildApiUrl } from '../../api/client';
import { getThumbnailUrl } from '../../api/thumbnails';
import { Breadcrumbs } from '../../components/shared/Breadcrumbs';
import { ErrorState } from '../../components/shared/ErrorState';
import { TagPills } from '../../components/shared/TagPills';
import { Thumbnail } from '../../components/shared/Thumbnail';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';
import { addPlaylistLocalQuery } from '../../lib/playlist';

export function ItemDetailPage(): JSX.Element {
  const { itemId: itemIdParam } = useParams();
  const navigate = useNavigate();
  const [showPlayer, setShowPlayer] = useState(false);
  const itemId = Number(itemIdParam);

  const itemQuery = useQuery({ queryKey: ['item', itemId], queryFn: () => getItem(itemId) });
  const groupQuery = useQuery({
    queryKey: ['group', itemQuery.data?.mediaGroupId ?? 0],
    queryFn: () => getGroup(itemQuery.data?.mediaGroupId ?? 0),
    enabled: Boolean(itemQuery.data?.mediaGroupId),
  });
  const breadcrumbQuery = useQuery({
    queryKey: ['group-ancestors', groupQuery.data?.id ?? 0, groupQuery.data?.parentGroupId ?? null],
    queryFn: () => getGroupAncestorChain(groupQuery.data?.parentGroupId ?? null),
    enabled: Boolean(groupQuery.data),
  });

  if (itemQuery.isError) {
    return <ErrorState message={itemQuery.error instanceof Error ? itemQuery.error.message : 'Could not load item'} onRetry={() => void itemQuery.refetch()} />;
  }

  if (itemQuery.isLoading || !itemQuery.data) {
    return <Skeleton className="h-96 w-full rounded-xl" />;
  }

  const item = itemQuery.data;

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Browse', to: '/browse' },
          ...(breadcrumbQuery.data ?? []).map((group) => ({ label: group.title, to: `/browse/groups/${group.id}` })),
          ...(groupQuery.data ? [{ label: groupQuery.data.title, to: `/browse/groups/${groupQuery.data.id}` }] : []),
          { label: item.title },
        ]}
      />

      <Card>
        <div className="grid gap-6 lg:grid-cols-[360px_minmax(0,1fr)]">
          <Thumbnail alt={item.title} src={item.hasThumbnail ? getThumbnailUrl('media-items', item.id) : undefined} />
          <div className="space-y-4">
            <div>
              <h1 className="text-3xl font-bold text-content">{item.title}</h1>
              <p className="mt-2 text-content-secondary">{item.description ?? 'No description available for this item.'}</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-lg bg-bg-subtle p-4">
                <p className="text-xs uppercase tracking-[0.2em] text-content-muted">Media file</p>
                <p className="mt-2 break-all text-sm text-content">{item.mediaFilePath}</p>
              </div>
              <div className="rounded-lg bg-bg-subtle p-4">
                <p className="text-xs uppercase tracking-[0.2em] text-content-muted">Last watched</p>
                <p className="mt-2 text-sm text-content">{item.lastWatchedDate ?? 'Not yet streamed'}</p>
              </div>
            </div>
            <TagPills tags={item.tags} onTagClick={(tag) => navigate(`/browse/search?tagContains=${encodeURIComponent(tag)}`)} />
            <div className="flex flex-wrap gap-3">
              <Button onClick={() => setShowPlayer((current) => !current)}>
                {showPlayer ? 'Hide player' : 'Watch now'}
              </Button>
              <Button variant="secondary" onClick={() => window.location.assign(buildApiUrl(addPlaylistLocalQuery(`playlist/media-item/${item.id}`)))}>
                Watch in VLC
              </Button>
            </div>
          </div>
        </div>
      </Card>

      {showPlayer ? (
        <Card>
          <video controls className="aspect-video w-full rounded-lg bg-slate-950" src={buildApiUrl(`stream/${item.id}`)}>
            Your browser does not support inline playback.
          </video>
        </Card>
      ) : null}
    </div>
  );
}
