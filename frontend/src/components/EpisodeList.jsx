import { useState } from 'react'
import { useAppConfig } from '../context/AppConfigContext'

const EPISODES_API = '/api/episodes'
const STREAM_API = '/api/stream'

export default function EpisodeList({ episodes, onEdit, onDelete, onTagClick, readOnly = false }) {
  if (episodes.length === 0) {
    return (
      <div className="text-center text-gray-500 py-16">
        <p className="text-5xl mb-4" aria-hidden="true">📺</p>
        <p className="text-xl">No episodes found.</p>
        <p className="text-sm mt-2">
          {readOnly ? 'Try adjusting your filters.' : 'Add an episode to get started!'}
        </p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
      {episodes.map((episode) => (
        <EpisodeCard
          key={episode.id}
          episode={episode}
          onEdit={onEdit}
          onDelete={onDelete}
          onTagClick={onTagClick}
          readOnly={readOnly}
        />
      ))}
    </div>
  )
}

function EpisodeCard({ episode, onEdit, onDelete, onTagClick, readOnly }) {
  const [showPlayer, setShowPlayer] = useState(false)
  const [isLandscapeThumb, setIsLandscapeThumb] = useState(null) // null = not loaded yet
  const { vlcEnabled } = useAppConfig()
  const seriesLabel = typeof episode.series === 'string' ? episode.series : episode.series?.name

  const seasonEpisodeLabel = [
    episode.season != null ? `S${episode.season}` : null,
    episode.episode != null ? `E${episode.episode}` : null,
  ].filter(Boolean).join('')

  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden flex flex-col hover:border-gray-600 transition-colors">
      {episode.hasThumbnail && !showPlayer && (
        <img
          src={`${EPISODES_API}/${episode.id}/thumbnail`}
          alt={`${seriesLabel} thumbnail`}
          onLoad={(e) => {
            const { naturalWidth, naturalHeight } = e.currentTarget
            setIsLandscapeThumb(naturalWidth > naturalHeight)
          }}
          className={`w-full h-48 ${
            isLandscapeThumb === null
              ? 'object-contain invisible' // safe default before load
              : isLandscapeThumb
                ? 'object-fill'
                : 'object-contain'
          }`}
        />
      )}
      {showPlayer && (
        <video
          controls
          className="w-full bg-black"
          src={`${STREAM_API}/E${episode.id}`}
        />
      )}
      <div className="p-5 flex flex-col gap-3 flex-1">
      <div className="flex items-start justify-between gap-2">
        <div className="flex flex-col gap-0.5">
          <h2 className="text-lg font-semibold text-white leading-tight">{seriesLabel}</h2>
          {episode.episodeTitle && (
            <p className="text-sm text-gray-300 leading-tight">{episode.episodeTitle}</p>
          )}
        </div>
        {episode.watchedRecently && (
          <span className="shrink-0 text-xs bg-green-800/60 text-green-300 px-2 py-0.5 rounded-full">
            Watched recently
          </span>
        )}
      </div>

      {seasonEpisodeLabel && (
        <div className="text-sm text-gray-400">
          <span>📺 {seasonEpisodeLabel}</span>
        </div>
      )}

      {episode.description && (
        <p className="text-sm text-gray-400 line-clamp-2">{episode.description}</p>
      )}

      {episode.tags && episode.tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {episode.tags.map((tag) => (
            <button
              key={tag}
              type="button"
              onClick={() => onTagClick && onTagClick(tag)}
              aria-label={`Filter by tag ${tag}`}
              className="text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full hover:bg-indigo-700/60 hover:text-indigo-100 transition-colors cursor-pointer"
            >
              {tag}
            </button>
          ))}
        </div>
      )}

      <div className="flex gap-2 mt-auto pt-2">
        <button
          type="button"
          onClick={() => setShowPlayer((prev) => !prev)}
          aria-pressed={showPlayer}
          aria-label={showPlayer ? `Hide player for ${episode.title}` : `Watch ${episode.title}`}
          className="flex-1 bg-indigo-700 hover:bg-indigo-600 text-white text-sm px-3 py-1.5 rounded-lg transition-colors"
        >
          {showPlayer ? 'Hide' : '▶ Watch'}
        </button>
        {vlcEnabled && (
          <button
            type="button"
            aria-label="Watch in VLC"
            onClick={() => {
              window.location.href = `${STREAM_API}/E${episode.id}/playlist`
            }}
            className="flex-1 bg-indigo-700 hover:bg-indigo-600 text-white text-sm px-3 py-1.5 rounded-lg transition-colors"
          >
            ▶ Watch in VLC
          </button>
        )}
        {!readOnly && (
          <>
            <button
              onClick={() => onEdit(episode)}
              className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-200 text-sm px-3 py-1.5 rounded-lg transition-colors"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete(episode.id)}
              className="flex-1 bg-red-900/40 hover:bg-red-800/60 text-red-300 text-sm px-3 py-1.5 rounded-lg transition-colors"
            >
              Delete
            </button>
          </>
        )}
      </div>
      </div>
    </div>
  )
}
