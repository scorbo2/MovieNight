import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { createGroup, getGroup, listAllGroups, updateGroup } from '../../api/groups';
import type { GroupUpsertPayload } from '../../api/types';
import { ErrorState } from '../../components/shared/ErrorState';
import { Breadcrumbs } from '../../components/shared/Breadcrumbs';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';
import { Tabs } from '../../components/ui/Tabs';
import { useToast } from '../../components/ui/ToastProvider';
import { GroupDeleteDialog } from '../features/GroupDeleteDialog';
import { GroupForm } from '../features/GroupForm';
import { ThumbnailPanel } from '../features/ThumbnailPanel';

export function AdminGroupEditPage(): JSX.Element {
  const params = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { pushToast } = useToast();
  const groupId = params.groupId ? Number(params.groupId) : null;
  const isEdit = Boolean(groupId);
  const [activeTab, setActiveTab] = useState<'details' | 'thumbnail'>('details');
  const [deleteOpen, setDeleteOpen] = useState(false);

  useEffect(() => {
    if (!isEdit) {
      setActiveTab('details');
    }
  }, [isEdit]);

  const groupQuery = useQuery({
    queryKey: ['group', groupId ?? 0],
    queryFn: () => getGroup(groupId ?? 0),
    enabled: isEdit,
  });
  const allGroupsQuery = useQuery({
    queryKey: ['groups', 'all'],
    queryFn: listAllGroups,
  });

  const mutation = useMutation({
    mutationFn: async (payload: GroupUpsertPayload) => {
      if (groupId) {
        return updateGroup(groupId, payload);
      }
      return createGroup(payload);
    },
    onSuccess: async (group) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['groups'] }),
        queryClient.invalidateQueries({ queryKey: ['group', group.id] }),
      ]);
      pushToast({ title: groupId ? 'Group saved' : 'Group created', tone: 'success' });
      if (!groupId) {
        navigate(`/admin/groups/${group.id}/edit`, { replace: true });
      }
    },
    onError: (error) => {
      pushToast({ title: 'Save failed', description: error instanceof Error ? error.message : 'Unknown error', tone: 'error' });
    },
  });

  if (groupQuery.isError) {
    return <ErrorState message={groupQuery.error instanceof Error ? groupQuery.error.message : 'Could not load group'} onRetry={() => void groupQuery.refetch()} />;
  }

  const isLoading = isEdit && groupQuery.isLoading;
  const group = groupQuery.data;

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Dashboard', to: '/admin' },
          { label: 'Groups', to: '/admin/groups' },
          { label: isEdit ? group?.title ?? 'Edit group' : 'New group' },
        ]}
      />

      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="section-title">{isEdit ? 'Edit group' : 'Create group'}</h2>
          <p className="section-copy mt-1">Update group metadata and manage its thumbnail.</p>
        </div>
        <div className="flex gap-3">
          {groupId ? (
            <Button variant="danger" onClick={() => setDeleteOpen(true)}>
              Delete group
            </Button>
          ) : null}
          <Link to="/admin/groups">
            <Button variant="secondary">Back to groups</Button>
          </Link>
        </div>
      </div>

      {isLoading ? (
        <Card>
          <Skeleton className="h-56 w-full" />
        </Card>
      ) : (
        <Card className="space-y-6">
          {groupId ? (
            <Tabs
              tabs={[
                { id: 'details', label: 'Details' },
                { id: 'thumbnail', label: 'Thumbnail' },
              ]}
              activeTab={activeTab}
              onChange={(value) => setActiveTab(value as 'details' | 'thumbnail')}
            />
          ) : null}

          {!groupId || activeTab === 'details' ? (
            <GroupForm
              groups={allGroupsQuery.data ?? []}
              initialValues={group}
              loading={mutation.isPending}
              currentGroupId={groupId ?? undefined}
              onSubmit={async (payload) => {
                await mutation.mutateAsync(payload);
              }}
            />
          ) : null}

          {groupId && activeTab === 'thumbnail' && group ? (
            <ThumbnailPanel entityType="media-groups" entityId={groupId} title={group.title} hasThumbnail={group.hasThumbnail} />
          ) : null}
        </Card>
      )}

      <GroupDeleteDialog
        groupId={groupId}
        groupTitle={group?.title}
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onDeleted={() => navigate('/admin/groups')}
      />
    </div>
  );
}
