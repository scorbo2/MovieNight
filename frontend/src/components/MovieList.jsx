import { useState } from 'react'

const MOVIES_API = '/api/movies'
const STREAM_API = '/api/stream'

export default function MovieList({ movies, onEdit, onDelete, onTagClick, readOnly = false }) {
  if (movies.length === 0) {
    return (
      <div className="text-center text-gray-500 py-16">
        <p className="text-5xl mb-4" aria-hidden="true">🎥</p>
        <p className="text-xl">No movies found.</p>
        <p className="text-sm mt-2">
          {readOnly ? 'Try adjusting your filters.' : 'Add a movie to get started!'}
        </p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
      {movies.map((movie) => (
        <MovieCard key={movie.id} movie={movie} onEdit={onEdit} onDelete={onDelete} onTagClick={onTagClick} readOnly={readOnly} />
      ))}
    </div>
  )
}

function MovieCard({ movie, onEdit, onDelete, onTagClick, readOnly }) {
  const [showPlayer, setShowPlayer] = useState(false)
  const genreLabel = typeof movie.genre === 'string' ? movie.genre : movie.genre?.name

  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden flex flex-col hover:border-gray-600 transition-colors">
      {movie.hasThumbnail && !showPlayer && (
        <img
          src={`${MOVIES_API}/${movie.id}/thumbnail`}
          alt={`${movie.title} thumbnail`}
          className="w-full h-48 object-cover"
        />
      )}
      {showPlayer && (
        <video
          controls
          className="w-full bg-black"
          src={`${STREAM_API}/M${movie.id}`}
        />
      )}
      <div className="p-5 flex flex-col gap-3 flex-1">
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-lg font-semibold text-white leading-tight">{movie.title}</h2>
        {movie.watchedRecently && (
          <span className="shrink-0 text-xs bg-green-800/60 text-green-300 px-2 py-0.5 rounded-full">
            Watched recently
          </span>
        )}
      </div>

      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-400">
        {movie.year && <span>📅 {movie.year}</span>}
        {genreLabel && <span>🎭 {genreLabel}</span>}
      </div>

      {movie.description && (
        <p className="text-sm text-gray-400 line-clamp-2">{movie.description}</p>
      )}

      {movie.tags && movie.tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {movie.tags.map((tag) => (
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
          onClick={() => setShowPlayer((prev) => !prev)}
          aria-pressed={showPlayer}
          className="flex-1 bg-indigo-700 hover:bg-indigo-600 text-white text-sm px-3 py-1.5 rounded-lg transition-colors"
        >
          {showPlayer ? 'Hide' : '▶ Watch'}
        </button>
        {!readOnly && (
          <>
            <button
              onClick={() => onEdit(movie)}
              className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-200 text-sm px-3 py-1.5 rounded-lg transition-colors"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete(movie.id)}
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
