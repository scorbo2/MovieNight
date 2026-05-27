import { apiFetch, buildApiUrl } from './client';
import type { ThumbnailActionResponse } from './types';

export type ThumbnailEntityType = 'media-groups' | 'media-items';

function buildFormData(file: File): FormData {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
}

export function getThumbnailUrl(entityType: ThumbnailEntityType, id: number): string {
  return buildApiUrl(`thumbnails/${entityType}/${id}`);
}

export async function uploadThumbnail(
  entityType: ThumbnailEntityType,
  id: number,
  file: File,
  replace = false,
): Promise<ThumbnailActionResponse> {
  return apiFetch<ThumbnailActionResponse>(`thumbnails/${entityType}/${id}`, {
    method: replace ? 'PUT' : 'POST',
    body: buildFormData(file),
  });
}

export async function deleteThumbnail(entityType: ThumbnailEntityType, id: number): Promise<void> {
  return apiFetch<void>(`thumbnails/${entityType}/${id}`, { method: 'DELETE' });
}
