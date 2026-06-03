import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { getGroup, getGroupAncestorChain } from '../../api/groups';
import { getItem } from '../../api/items';
import { buildApiUrl } from '../../api/client';
import { getThumbnailUrl } from '../../api/thumbnails';
import { Breadcrumbs } from '../../components/shared/Breadcrumbs';
import { ErrorState } from '../../components/shared/ErrorState';
import { TagPills } from '../../components/shared/TagPills';
import { Thumbnail } from '../../components/shared/Thumbnail';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Select } from '../../components/ui/Select';
import { Skeleton } from '../../components/ui/Skeleton';
import { addPlaylistLocalQuery, addPlaylistTrackQuery } from '../../lib/playlist';

/**
 * We may either receive ISO 639-1 (2-letter) or ISO 639-2 (3-letter) language codes from the backend.
 * We will do our best to map these to country codes, with a safe fallback for unrecognized codes.
 */
function languageCodeToCountryCode(languageCode: string): string {
    const SAFE_FALLBACK = 'UN'; // United Nations flag as a generic fallback
    if (!languageCode) return SAFE_FALLBACK;

    // If we get a 2-letter code, it's likely ISO 639-1 (language), which doesn't map 1:1 to a country flag.
    // Use a safe fallback unless we explicitly add a mapping for it.
    if (languageCode.length === 2) {
        return SAFE_FALLBACK;
    }

    // Otherwise, you might think we could just take the first 2 characters of the language code,
    // but ISO 639-2 is very weird, and some codes make no sense (like "GER" for Germany).
    // So, we'll just have a little map of the most likely language codes to their corresponding country codes.
    const mapping: Record<string, string> = {
        'eng': 'CA', // English → Canada (because I'm Canadian and we deserve the maple leaf flag 🍁)
        'ger': 'DE', // German → Germany
        'deu': 'DE', // German (alternative code) → Germany
        'fre': 'FR', // French → France
        'fra': 'FR', // French (alternative code) → France
        'spa': 'ES', // Spanish → Spain
        'ita': 'IT', // Italian → Italy
        'jpn': 'JP', // Japanese → Japan
        'chi': 'CN', // Chinese → China
        'rus': 'RU', // Russian → Russia
        // Add more mappings as needed
    }

    return mapping[languageCode.toLowerCase()] || SAFE_FALLBACK;
}

/**
 * Takes a language code, converts it to a country code, and then converts
 * that to a flag emoji using Unicode regional indicator symbols.
 */
function codeToFlagEmoji(code: string): string {
  const OFFSET = 0x1F1E6 - 0x41; // maps 'A' → U+1F1E6 (regional indicator)
  const normalized = languageCodeToCountryCode(code).toUpperCase();
  if (normalized.length !== 2) return '';
  const first = String.fromCodePoint(normalized.charCodeAt(0) + OFFSET);
  const second = String.fromCodePoint(normalized.charCodeAt(1) + OFFSET);
  return first + second;
}

/**
 * Returns a human-readable label for a track, preferring title, then languageName, then index.
 */
function getTrackLabel(track: { title: string | null; languageName: string; index: number }): string {
  return track.title || track.languageName || String(track.index);
}

export function ItemDetailPage(): JSX.Element {
  const { itemId: itemIdParam } = useParams();
  const navigate = useNavigate();
  const [showPlayer, setShowPlayer] = useState(false);
  const [selectedAudioTrack, setSelectedAudioTrack] = useState<number | undefined>(undefined);
  const [selectedSubtitleTrack, setSelectedSubtitleTrack] = useState<number | undefined>(undefined);
  const itemId = Number(itemIdParam);

  const itemQuery = useQuery({ queryKey: ['item', itemId], queryFn: () => getItem(itemId) });
  const groupQuery = useQuery({
    queryKey: ['group', itemQuery.data?.mediaGroupId ?? 0],
    queryFn: () => getGroup(itemQuery.data?.mediaGroupId ?? 0),
    enabled: Boolean(itemQuery.data?.mediaGroupId),
  });
  const breadcrumbQuery = useQuery({
    queryKey: ['group-ancestors', groupQuery.data?.id ?? 0, groupQuery.data?.parentGroupId ?? null],
    queryFn: () => getGroupAncestorChain(groupQuery.data?.parentGroupId ?? null),
    enabled: Boolean(groupQuery.data),
  });

  if (itemQuery.isError) {
    return <ErrorState message={itemQuery.error instanceof Error ? itemQuery.error.message : 'Could not load item'} onRetry={() => void itemQuery.refetch()} />;
  }

  if (itemQuery.isLoading || !itemQuery.data) {
    return <Skeleton className="h-96 w-full rounded-xl" />;
  }

  const item = itemQuery.data;
  const showAudioDropdown = item.audioTracks.length > 1;
  const showSubtitleDropdown = item.subtitleTracks.length > 1;

  const handleWatchInVlc = () => {
    const audioTrackId = showAudioDropdown ? selectedAudioTrack : undefined;
    const subtitleTrackId = showSubtitleDropdown ? selectedSubtitleTrack : undefined;
    const playlistPath = addPlaylistLocalQuery(`playlist/media-item/${item.id}`);
    const vlcUrl = addPlaylistTrackQuery(playlistPath, audioTrackId, subtitleTrackId);
    window.location.assign(buildApiUrl(vlcUrl));
  };

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Browse', to: '/browse' },
          ...(breadcrumbQuery.data ?? []).map((group) => ({ label: group.title, to: `/browse/groups/${group.id}` })),
          ...(groupQuery.data ? [{ label: groupQuery.data.title, to: `/browse/groups/${groupQuery.data.id}` }] : []),
          { label: item.title },
        ]}
      />

      <Card>
        <div className="grid gap-6 lg:grid-cols-[360px_minmax(0,1fr)]">
          <Thumbnail alt={item.title} src={item.hasThumbnail ? getThumbnailUrl('media-items', item.id) : undefined} />
          <div className="space-y-4">
            <div>
              <h1 className="text-3xl font-bold text-content">{item.title}</h1>
              {item.description && <p className="mt-2 text-content-secondary">{item.description}</p>}
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-lg bg-bg-subtle p-4">
                <p className="text-xs uppercase tracking-[0.2em] text-content-muted">Media file</p>
                <p className="mt-2 break-all text-sm text-content">{item.mediaFilePath}</p>
              </div>
              <div className="rounded-lg bg-bg-subtle p-4">
                <p className="text-xs uppercase tracking-[0.2em] text-content-muted">Last watched</p>
                <p className="mt-2 text-sm text-content">{item.lastWatchedDate ?? 'Not yet streamed'}</p>
              </div>
            </div>
            <TagPills tags={item.tags} onTagClick={(tag) => navigate(`/browse/search?tagContains=${encodeURIComponent(tag)}`)} />

            {showAudioDropdown || showSubtitleDropdown ? (
              <div className="grid gap-3 sm:grid-cols-2">
                {showAudioDropdown ? (
                  <div className="rounded-lg bg-bg-subtle p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-content-muted">Audio Track</p>
                    <Select
                      className="mt-2 h-9 w-full"
                      value={selectedAudioTrack ?? ''}
                      onChange={(e) => setSelectedAudioTrack(Number(e.target.value))}
                    >
                      <option value="" disabled>
                        Select audio track
                      </option>
                      {item.audioTracks.map((track) => (
                        <option key={track.index} value={track.index}>
                          {codeToFlagEmoji(track.language)} {getTrackLabel(track)}
                        </option>
                      ))}
                    </Select>
                  </div>
                ) : null}
                {showSubtitleDropdown ? (
                  <div className="rounded-lg bg-bg-subtle p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-content-muted">Subtitle Track</p>
                    <Select
                      className="mt-2 h-9 w-full"
                      value={selectedSubtitleTrack ?? ''}
                      onChange={(e) => setSelectedSubtitleTrack(Number(e.target.value))}
                    >
                      <option value="" disabled>
                        Select subtitle track
                      </option>
                      {item.subtitleTracks.map((track) => (
                        <option key={track.index} value={track.index}>
                          {codeToFlagEmoji(track.language)} {getTrackLabel(track)}
                        </option>
                      ))}
                    </Select>
                  </div>
                ) : null}
              </div>
            ) : null}

            <div className="flex flex-wrap gap-3">
              <Button onClick={() => setShowPlayer((current) => !current)}>
                {showPlayer ? 'Hide player' : 'Watch now'}
              </Button>
              <Button variant="secondary" onClick={handleWatchInVlc}>
                Watch in VLC
              </Button>
            </div>
          </div>
        </div>
      </Card>

      {showPlayer ? (
        <Card>
          <video controls className="aspect-video w-full rounded-lg bg-slate-950" src={buildApiUrl(`stream/${item.id}`)}>
            Your browser does not support inline playback.
          </video>
        </Card>
      ) : null}
    </div>
  );
}
