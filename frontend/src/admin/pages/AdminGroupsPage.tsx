import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { listAllGroups, listGroups } from '../../api/groups';
import type { MediaGroup } from '../../api/types';
import { ErrorState } from '../../components/shared/ErrorState';
import { PaginationControls } from '../../components/shared/PaginationControls';
import { SearchBar } from '../../components/shared/SearchBar';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';
import { Table, TableBody, TableHead, TableRow, TableWrapper, Td, Th } from '../../components/ui/Table';
import { getNullableStringParam, getPositiveIntParam, setParam } from '../../lib/url';
import { GroupDeleteDialog } from '../features/GroupDeleteDialog';

export function AdminGroupsPage(): JSX.Element {
  const [searchParams, setSearchParams] = useSearchParams();
  const [deleteTarget, setDeleteTarget] = useState<MediaGroup | null>(null);
  const pageNumber = getPositiveIntParam(searchParams.get('pageNumber'), 1);
  const pageSize = getPositiveIntParam(searchParams.get('pageSize'), 10);
  const filters = {
    pageNumber,
    pageSize,
    titleContains: getNullableStringParam(searchParams.get('titleContains')),
    descriptionContains: getNullableStringParam(searchParams.get('descriptionContains')),
  };

  const groupsQuery = useQuery({
    queryKey: ['groups', filters],
    queryFn: () => listGroups(filters),
  });
  const allGroupsQuery = useQuery({
    queryKey: ['groups', 'all'],
    queryFn: listAllGroups,
  });

  const parentMap = useMemo(
    () => new Map((allGroupsQuery.data ?? []).map((group) => [group.id, group.title])),
    [allGroupsQuery.data],
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
          <h2 className="section-title">Groups</h2>
          <p className="section-copy mt-1">Search, create, and maintain media groups.</p>
        </div>
        <Link to="/admin/groups/new">
          <Button>New group</Button>
        </Link>
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
        <Card>
          <Skeleton className="h-12 w-full" />
        </Card>
      ) : (
        <>
          <TableWrapper>
            <Table>
              <TableHead>
                <tr>
                  <Th>Title</Th>
                  <Th>Parent</Th>
                  <Th>Thumbnail</Th>
                  <Th className="text-right">Actions</Th>
                </tr>
              </TableHead>
              <TableBody>
                {groupsQuery.data?.items.map((group) => (
                  <TableRow key={group.id}>
                    <Td>
                      <div>
                        <p className="font-medium">{group.title}</p>
                        {group.description ? <p className="mt-1 text-xs text-content-secondary">{group.description}</p> : null}
                      </div>
                    </Td>
                    <Td>{group.parentGroupId ? parentMap.get(group.parentGroupId) ?? 'Unknown' : 'Top-level'}</Td>
                    <Td>
                      <Badge tone={group.hasThumbnail ? 'success' : 'warning'}>
                        {group.hasThumbnail ? 'Available' : 'Missing'}
                      </Badge>
                    </Td>
                    <Td>
                      <div className="flex justify-end gap-2">
                        <Link to={`/admin/groups/${group.id}/edit`}>
                          <Button variant="secondary" size="sm">
                            Edit
                          </Button>
                        </Link>
                        <Button variant="danger" size="sm" onClick={() => setDeleteTarget(group)}>
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
            totalCount={groupsQuery.data?.totalCount ?? 0}
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

      <GroupDeleteDialog
        groupId={deleteTarget?.id ?? null}
        groupTitle={deleteTarget?.title}
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
      />
    </div>
  );
}
