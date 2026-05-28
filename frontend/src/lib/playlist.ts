const PLAYLIST_LOCAL_STORAGE_KEY = 'movienight-playlist-local';

export function getPlaylistLocalPreference(): boolean {
  return window.localStorage.getItem(PLAYLIST_LOCAL_STORAGE_KEY) === 'true';
}

export function setPlaylistLocalPreference(isLocal: boolean): void {
  window.localStorage.setItem(PLAYLIST_LOCAL_STORAGE_KEY, String(isLocal));
}

export function addPlaylistLocalQuery(path: string): string {
  if (!getPlaylistLocalPreference()) {
    return path;
  }

  return path.includes('?') ? `${path}&local=true` : `${path}?local=true`;
}

