import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { listAllGroups } from '../../api/groups';
import { listGroupItems } from '../../api/items';
import type { MediaItem } from '../../api/types';
import { EmptyState } from '../../components/shared/EmptyState';
import { ErrorState } from '../../components/shared/ErrorState';
import { PaginationControls } from '../../components/shared/PaginationControls';
import { SearchBar } from '../../components/shared/SearchBar';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Select } from '../../components/ui/Select';
import { Skeleton } from '../../components/ui/Skeleton';
import { Table, TableBody, TableHead, TableRow, TableWrapper, Td, Th } from '../../components/ui/Table';
import { getNullableStringParam, getPositiveIntParam, setParam } from '../../lib/url';
import { ItemDeleteDialog } from '../features/ItemDeleteDialog';

export function AdminItemsPage(): JSX.Element {
  const [searchParams, setSearchParams] = useSearchParams();
  const [deleteTarget, setDeleteTarget] = useState<MediaItem | null>(null);
  const groupId = searchParams.get('groupId') ? Number(searchParams.get('groupId')) : NaN;
  const hasGroup = Number.isInteger(groupId) && groupId > 0;
  const pageNumber = getPositiveIntParam(searchParams.get('pageNumber'), 1);
  const pageSize = getPositiveIntParam(searchParams.get('pageSize'), 10);
  const filters = {
    pageNumber,
    pageSize,
    titleContains: getNullableStringParam(searchParams.get('titleContains')),
    descriptionContains: getNullableStringParam(searchParams.get('descriptionContains')),
    tagContains: getNullableStringParam(searchParams.get('tagContains')),
  };

  const groupsQuery = useQuery({ queryKey: ['groups', 'all'], queryFn: listAllGroups });
  const itemsQuery = useQuery({
    queryKey: ['group-items', groupId, filters],
    queryFn: () => listGroupItems(groupId, filters),
    enabled: hasGroup,
  });

  const selectedGroupName = useMemo(
    () => groupsQuery.data?.find((group) => group.id === groupId)?.title,
    [groupId, groupsQuery.data],
  );

  const updateParams = (updater: (next: URLSearchParams) => void): void => {
    const next = new URLSearchParams(searchParams);
    updater(next);
    setSearchParams(next);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="section-title">Items</h2>
          <p className="section-copy mt-1">Choose a group, then manage items, metadata, and thumbnails.</p>
        </div>
        {hasGroup ? (
          <Link to={`/admin/items/new?groupId=${groupId}`}>
            <Button>New item</Button>
          </Link>
        ) : null}
      </div>

      <Card className="space-y-4">
        <div className="space-y-2">
          <label className="text-sm font-medium text-content">Media group</label>
          <Select
            value={hasGroup ? String(groupId) : ''}
            onChange={(event) =>
              updateParams((next) => {
                setParam(next, 'groupId', event.target.value || null);
                next.set('pageNumber', '1');
                next.set('pageSize', String(pageSize));
              })
            }
          >
            <option value="">Choose a group</option>
            {(groupsQuery.data ?? []).map((group) => (
              <option key={group.id} value={group.id}>
                {group.title}
              </option>
            ))}
          </Select>
        </div>
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

      {!hasGroup ? (
        <EmptyState title="Choose a group" description="Select a media group above to view and manage its items." />
      ) : itemsQuery.isError ? (
        <ErrorState message={itemsQuery.error instanceof Error ? itemsQuery.error.message : 'Could not load items'} onRetry={() => void itemsQuery.refetch()} />
      ) : itemsQuery.isLoading ? (
        <Card>
          <Skeleton className="h-12 w-full" />
        </Card>
      ) : (
        <>
          <Card className="py-4">
            <p className="text-sm text-content-secondary">Showing items in <span className="font-medium text-content">{selectedGroupName ?? 'selected group'}</span>.</p>
          </Card>
          <TableWrapper>
            <Table>
              <TableHead>
                <tr>
                  <Th>Title</Th>
                  <Th>Tags</Th>
                  <Th>Last watched</Th>
                  <Th className="text-right">Actions</Th>
                </tr>
              </TableHead>
              <TableBody>
                {itemsQuery.data?.items.map((item) => (
                  <TableRow key={item.id}>
                    <Td>
                      <div>
                        <p className="font-medium">{item.title}</p>
                        {item.description ? <p className="mt-1 text-xs text-content-secondary">{item.description}</p> : null}
                      </div>
                    </Td>
                    <Td>
                      <div className="flex flex-wrap gap-2">
                        {item.tags.length === 0 ? <span className="text-content-secondary">—</span> : item.tags.map((tag) => <Badge key={tag}>{tag}</Badge>)}
                      </div>
                    </Td>
                    <Td>{item.lastWatchedDate ?? 'Never'}</Td>
                    <Td>
                      <div className="flex justify-end gap-2">
                        <Link to={`/admin/items/${item.id}/edit`}>
                          <Button variant="secondary" size="sm">
                            Edit
                          </Button>
                        </Link>
                        <Button variant="danger" size="sm" onClick={() => setDeleteTarget(item)}>
                          Delete
                        </Button>
                      </div>
                    </Td>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableWrapper>
          <PaginationControls
            totalCount={itemsQuery.data?.totalCount ?? 0}
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
      )}

      <ItemDeleteDialog
        itemId={deleteTarget?.id ?? null}
        itemTitle={deleteTarget?.title}
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
      />
    </div>
  );
}
