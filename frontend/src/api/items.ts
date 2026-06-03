import { apiFetch, buildQueryString } from './client';
import type { ItemUpsertPayload, MediaItem, MediaItemListParams, MediaItemListResponse } from './types';

export async function listItems(params: MediaItemListParams = {}): Promise<MediaItemListResponse> {
  return apiFetch<MediaItemListResponse>(`media-items${buildQueryString(params)}`);
}

export async function listGroupItems(groupId: number, params: MediaItemListParams = {}): Promise<MediaItemListResponse> {
  return apiFetch<MediaItemListResponse>(`media-groups/${groupId}/items${buildQueryString(params)}`);
}

export async function getItem(itemId: number): Promise<MediaItem> {
  return apiFetch<MediaItem>(`media-items/${itemId}`);
}

export async function createItem(groupId: number, payload: Omit<ItemUpsertPayload, 'mediaGroupId'>): Promise<MediaItem> {
  return apiFetch<MediaItem>(`media-groups/${groupId}/items`, {
    method: 'POST',
    body: JSON.stringify({
      title: payload.title,
      description: payload.description ?? null,
      lastWatchedDate: payload.lastWatchedDate ?? null,
      mediaFilePath: payload.mediaFilePath,
      tags: payload.tags ?? [],
    }),
  });
}

export async function updateItem(itemId: number, payload: ItemUpsertPayload): Promise<MediaItem> {
  return apiFetch<MediaItem>(`media-items/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify({
      mediaGroupId: payload.mediaGroupId,
      title: payload.title,
      description: payload.description ?? null,
      lastWatchedDate: payload.lastWatchedDate ?? null,
      mediaFilePath: payload.mediaFilePath,
      tags: payload.tags ?? [],
    }),
  });
}

export async function deleteItem(itemId: number): Promise<void> {
  return apiFetch<void>(`media-items/${itemId}`, { method: 'DELETE' });
}
