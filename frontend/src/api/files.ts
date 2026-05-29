import { apiFetch, buildQueryString } from './client';

export interface FileBrowserEntry {
  name: string;
  type: 'file' | 'directory';
  path: string;
}

export interface FileBrowserResponse {
  path: string;
  parent?: string;
  entries: FileBrowserEntry[];
}

export function listFiles(path?: string): Promise<FileBrowserResponse> {
  return apiFetch<FileBrowserResponse>(`files/${buildQueryString({ path })}`);
}
