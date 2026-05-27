import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { listAllGroups } from '../../api/groups';
import { createItem, getItem, updateItem } from '../../api/items';
import type { ItemUpsertPayload } from '../../api/types';
import { Breadcrumbs } from '../../components/shared/Breadcrumbs';
import { ErrorState } from '../../components/shared/ErrorState';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';
import { Tabs } from '../../components/ui/Tabs';
import { useToast } from '../../components/ui/ToastProvider';
import { ItemDeleteDialog } from '../features/ItemDeleteDialog';
import { ItemForm } from '../features/ItemForm';
import { ThumbnailPanel } from '../features/ThumbnailPanel';

export function AdminItemEditPage(): JSX.Element {
  const params = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { pushToast } = useToast();
  const itemId = params.itemId ? Number(params.itemId) : null;
  const isEdit = Boolean(itemId);
  const prefillGroupId = searchParams.get('groupId') ? Number(searchParams.get('groupId')) : undefined;
  const [activeTab, setActiveTab] = useState<'details' | 'thumbnail'>('details');
  const [deleteOpen, setDeleteOpen] = useState(false);

  useEffect(() => {
    if (!isEdit) {
      setActiveTab('details');
    }
  }, [isEdit]);

  const itemQuery = useQuery({
    queryKey: ['item', itemId ?? 0],
    queryFn: () => getItem(itemId ?? 0),
    enabled: isEdit,
  });
  const groupsQuery = useQuery({ queryKey: ['groups', 'all'], queryFn: listAllGroups });

  const mutation = useMutation({
    mutationFn: async (payload: ItemUpsertPayload) => {
      if (itemId) {
        return updateItem(itemId, payload);
      }
      return createItem(payload.mediaGroupId, payload);
    },
    onSuccess: async (item) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['items'] }),
        queryClient.invalidateQueries({ queryKey: ['group-items'] }),
        queryClient.invalidateQueries({ queryKey: ['item', item.id] }),
      ]);
      pushToast({ title: itemId ? 'Item saved' : 'Item created', tone: 'success' });
      if (!itemId) {
        navigate(`/admin/items/${item.id}/edit`, { replace: true });
      }
    },
    onError: (error) => {
      pushToast({ title: 'Save failed', description: error instanceof Error ? error.message : 'Unknown error', tone: 'error' });
    },
  });

  if (itemQuery.isError) {
    return <ErrorState message={itemQuery.error instanceof Error ? itemQuery.error.message : 'Could not load item'} onRetry={() => void itemQuery.refetch()} />;
  }

  const isLoading = isEdit && itemQuery.isLoading;
  const item = itemQuery.data;

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Dashboard', to: '/admin' },
          { label: 'Items', to: '/admin/items' },
          { label: isEdit ? item?.title ?? 'Edit item' : 'New item' },
        ]}
      />

      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="section-title">{isEdit ? 'Edit item' : 'Create item'}</h2>
          <p className="section-copy mt-1">Maintain item metadata, file locations, tags, and thumbnails.</p>
        </div>
        <div className="flex gap-3">
          {itemId ? (
            <Button variant="danger" onClick={() => setDeleteOpen(true)}>
              Delete item
            </Button>
          ) : null}
          <Link to={prefillGroupId ? `/admin/items?groupId=${prefillGroupId}` : '/admin/items'}>
            <Button variant="secondary">Back to items</Button>
          </Link>
        </div>
      </div>

      {isLoading ? (
        <Card>
          <Skeleton className="h-56 w-full" />
        </Card>
      ) : (
        <Card className="space-y-6">
          {itemId ? (
            <Tabs
              tabs={[
                { id: 'details', label: 'Details' },
                { id: 'thumbnail', label: 'Thumbnail' },
              ]}
              activeTab={activeTab}
              onChange={(value) => setActiveTab(value as 'details' | 'thumbnail')}
            />
          ) : null}

          {!itemId || activeTab === 'details' ? (
            <ItemForm
              groups={groupsQuery.data ?? []}
              initialValues={
                item ?? {
                  mediaGroupId: prefillGroupId,
                }
              }
              loading={mutation.isPending}
              onSubmit={async (payload) => {
                await mutation.mutateAsync(payload);
              }}
            />
          ) : null}

          {itemId && activeTab === 'thumbnail' && item ? (
            <ThumbnailPanel entityType="media-items" entityId={itemId} title={item.title} hasThumbnail={item.hasThumbnail} />
          ) : null}
        </Card>
      )}

      <ItemDeleteDialog
        itemId={itemId}
        itemTitle={item?.title}
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onDeleted={() => navigate(prefillGroupId ? `/admin/items?groupId=${prefillGroupId}` : '/admin/items')}
      />
    </div>
  );
}
