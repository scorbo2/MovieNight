export interface MediaGroup {
  id: number;
  parentGroupId: number | null;
  title: string;
  description: string | null;
  hasThumbnail: boolean;
}

export interface MediaGroupListResponse {
  items: MediaGroup[];
  totalCount: number;
  pageNumber: number;
  pageSize: number;
}

export interface TrackMetadata {
  index: number;
  language: string;
  languageName: string;
  codec: string;
  title: string | null;
}

export interface MediaItem {
  id: number;
  mediaGroupId: number;
  title: string;
  description: string | null;
  lastWatchedDate: string | null;
  mediaFilePath: string;
  tags: string[];
  hasThumbnail: boolean;
  recentlyWatched: boolean;
  audioTracks: TrackMetadata[];
  subtitleTracks: TrackMetadata[];
}

export interface MediaItemListResponse {
  items: MediaItem[];
  totalCount: number;
  pageNumber: number;
  pageSize: number;
}

export interface GroupListParams {
  pageNumber?: number;
  pageSize?: number;
  parentGroupId?: number;
  topLevelOnly?: boolean;
  titleContains?: string;
  descriptionContains?: string;
}

export interface MediaItemListParams {
  pageNumber?: number;
  pageSize?: number;
  titleContains?: string;
  descriptionContains?: string;
  tagContains?: string;
}

export interface GroupUpsertPayload {
  parentGroupId?: number | null;
  title: string;
  description?: string | null;
}

export interface ItemUpsertPayload {
  mediaGroupId: number;
  title: string;
  description?: string | null;
  lastWatchedDate?: string | null;
  mediaFilePath: string;
  tags?: string[];
}

export interface ThumbnailActionResponse {
  success: boolean;
  message: string;
  id: number;
}
