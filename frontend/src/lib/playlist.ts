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

/**
 * Appends audioTrackId and subtitleTrackId query parameters to a playlist path,
 * but only when the values are defined (i.e. more than one track is available and the user selected one).
 */
export function addPlaylistTrackQuery(
  path: string,
  audioTrackId?: number,
  subtitleTrackId?: number,
): string {
  const params = new URLSearchParams();

  if (audioTrackId !== undefined) {
    params.set('audioTrackId', String(audioTrackId));
  }
  if (subtitleTrackId !== undefined) {
    params.set('subtitleTrackId', String(subtitleTrackId));
  }

  const queryString = params.toString();
  if (!queryString) {
    return path;
  }

  return path.includes('?') ? `${path}&${queryString}` : `${path}?${queryString}`;
}

