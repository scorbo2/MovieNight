import { useState } from 'react'
const ARTISTS_API = '/api/artists'

export default function ArtistList({ artists, onEdit, onDelete, onArtistClick, readOnly = false }) {
  if (artists.length === 0) {
    return (
      <div className="text-center text-gray-500 py-16">
        <p className="text-5xl mb-4" aria-hidden="true">🎤</p>
        <p className="text-xl">No artists found.</p>
        <p className="text-sm mt-2">
          {readOnly ? 'Try adjusting your filters.' : 'Add an artist to get started!'}
        </p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
      {artists.map((artist) => (
        <ArtistCard
          key={artist.id}
          artist={artist}
          onEdit={onEdit}
          onDelete={onDelete}
          onArtistClick={onArtistClick}
          readOnly={readOnly}
        />
      ))}
    </div>
  )
}

function ArtistCard({ artist, onEdit, onDelete, onArtistClick, readOnly }) {
  const [isLandscapeThumb, setIsLandscapeThumb] = useState(null) // null = not loaded yet
  const isClickable = typeof onArtistClick === 'function'

  return (
    <div
      className={`bg-gray-900 border border-gray-800 rounded-xl overflow-hidden flex flex-col transition-colors ${
        isClickable ? 'cursor-pointer hover:border-indigo-500 hover:bg-gray-800/60' : 'hover:border-gray-600'
      }`}
      onClick={isClickable ? () => onArtistClick(artist) : undefined}
    >
      {artist.hasThumbnail && (
        <img
          src={`${ARTISTS_API}/${artist.id}/thumbnail`}
          alt={`${artist.name} thumbnail`}
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

      <div className="p-5 flex flex-col gap-3 flex-1">
        <div className="flex items-start justify-between gap-2">
          <h2 className="text-lg font-semibold text-white leading-tight">{artist.name}</h2>
          {artist.videoCount > 0 && (
            <span
              className="shrink-0 text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full"
              aria-label={`${artist.videoCount} ${artist.videoCount === 1 ? 'video' : 'videos'} by this artist`}
            >
              {artist.videoCount} {artist.videoCount === 1 ? 'video' : 'videos'}
            </span>
          )}
        </div>

        {artist.description ? (
          <p className="text-sm text-gray-400 line-clamp-3">{artist.description}</p>
        ) : (
          <p className="text-sm text-gray-500 italic">No description.</p>
        )}

        {!readOnly && (
          <div className="flex gap-2 mt-auto pt-2" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => onEdit(artist)}
              className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-200 text-sm px-3 py-1.5 rounded-lg transition-colors"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete(artist.id)}
              className="flex-1 bg-red-900/40 hover:bg-red-800/60 text-red-300 text-sm px-3 py-1.5 rounded-lg transition-colors"
            >
              Delete
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
