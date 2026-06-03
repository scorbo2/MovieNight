import { apiFetch, buildQueryString } from './client';
import type { GroupListParams, GroupUpsertPayload, MediaGroup, MediaGroupListResponse } from './types';

export async function listGroups(params: GroupListParams = {}): Promise<MediaGroupListResponse> {
  return apiFetch<MediaGroupListResponse>(`media-groups${buildQueryString(params)}`);
}

export async function listAllGroups(): Promise<MediaGroup[]> {
  const pageSize = 100;
  const groups: MediaGroup[] = [];
  let pageNumber = 1;
  let totalCount = 0;

  do {
    const response = await listGroups({ pageNumber, pageSize });
    totalCount = response.totalCount;
    groups.push(...response.items);
    pageNumber += 1;
  } while (groups.length < totalCount);

  return groups;
}

export async function getGroup(groupId: number): Promise<MediaGroup> {
  return apiFetch<MediaGroup>(`media-groups/${groupId}`);
}

export async function getGroupAncestorChain(parentGroupId: number | null): Promise<MediaGroup[]> {
  if (!parentGroupId) {
    return [];
  }

  const ancestors: MediaGroup[] = [];
  let currentId: number | null = parentGroupId;

  while (currentId) {
    const group = await getGroup(currentId);
    ancestors.unshift(group);
    currentId = group.parentGroupId;
  }

  return ancestors;
}

export async function createGroup(payload: GroupUpsertPayload): Promise<MediaGroup> {
  return apiFetch<MediaGroup>('media-groups', {
    method: 'POST',
    body: JSON.stringify({
      parentGroupId: payload.parentGroupId ?? null,
      title: payload.title,
      description: payload.description ?? null,
    }),
  });
}

export async function updateGroup(groupId: number, payload: GroupUpsertPayload): Promise<MediaGroup> {
  return apiFetch<MediaGroup>(`media-groups/${groupId}`, {
    method: 'PUT',
    body: JSON.stringify({
      parentGroupId: payload.parentGroupId ?? null,
      title: payload.title,
      description: payload.description ?? null,
    }),
  });
}

export async function deleteGroup(groupId: number): Promise<void> {
  return apiFetch<void>(`media-groups/${groupId}`, { method: 'DELETE' });
}
