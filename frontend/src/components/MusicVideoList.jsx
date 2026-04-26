import { useState } from 'react'

const MUSIC_VIDEOS_API = '/api/music-videos'
const STREAM_API = '/api/stream'

export default function MusicVideoList({ musicVideos, onEdit, onDelete, onTagClick, readOnly = false }) {
  if (musicVideos.length === 0) {
    return (
      <div className="text-center text-gray-500 py-16">
        <p className="text-5xl mb-4" aria-hidden="true">🎵</p>
        <p className="text-xl">No music videos found.</p>
        <p className="text-sm mt-2">
          {readOnly ? 'Try adjusting your filters.' : 'Add a music video to get started!'}
        </p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
      {musicVideos.map((mv) => (
        <MusicVideoCard
          key={mv.id}
          musicVideo={mv}
          onEdit={onEdit}
          onDelete={onDelete}
          onTagClick={onTagClick}
          readOnly={readOnly}
        />
      ))}
    </div>
  )
}

function MusicVideoCard({ musicVideo, onEdit, onDelete, onTagClick, readOnly }) {
  const [showPlayer, setShowPlayer] = useState(false)
  const artistLabel = typeof musicVideo.artist === 'string' ? musicVideo.artist : musicVideo.artist?.name

  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden flex flex-col hover:border-gray-600 transition-colors">
      {musicVideo.hasThumbnail && !showPlayer && (
        <img
          src={`${MUSIC_VIDEOS_API}/${musicVideo.id}/thumbnail`}
          alt={`${musicVideo.title} thumbnail`}
          className="w-full h-48 object-cover object-top"
        />
      )}
      {showPlayer && (
        <video
          controls
          className="w-full bg-black"
          src={`${STREAM_API}/V${musicVideo.id}`}
        />
      )}
      <div className="p-5 flex flex-col gap-3 flex-1">
        <div className="flex items-start justify-between gap-2">
          <div className="flex flex-col gap-0.5">
            <h2 className="text-lg font-semibold text-white leading-tight">{musicVideo.title}</h2>
            {artistLabel && (
              <p className="text-sm text-gray-300 leading-tight">🎤 {artistLabel}</p>
            )}
          </div>
            {musicVideo.watchedRecently && (
              <span className="shrink-0 text-xs bg-green-800/60 text-green-300 px-2 py-0.5 rounded-full">
                Watched recently
              </span>
            )}
        </div>

        <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-400">
          {musicVideo.album && <span>💿 {musicVideo.album}</span>}
          {musicVideo.year && <span>📅 {musicVideo.year}</span>}
        </div>

        {musicVideo.description && (
          <p className="text-sm text-gray-400 line-clamp-2">{musicVideo.description}</p>
        )}

        {musicVideo.tags && musicVideo.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {musicVideo.tags.map((tag) => (
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
            aria-label={showPlayer ? `Hide player for ${musicVideo.title}` : `Watch ${musicVideo.title}`}
            className="flex-1 bg-indigo-700 hover:bg-indigo-600 text-white text-sm px-3 py-1.5 rounded-lg transition-colors"
          >
            {showPlayer ? 'Hide' : '▶ Watch'}
          </button>
          {!readOnly && (
            <>
              <button
                onClick={() => onEdit(musicVideo)}
                className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-200 text-sm px-3 py-1.5 rounded-lg transition-colors"
              >
                Edit
              </button>
              <button
                onClick={() => onDelete(musicVideo.id)}
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
