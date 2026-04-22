const GENRES_API = '/api/genres'

export default function GenreList({ genres, onEdit, onDelete, readOnly = false }) {
  if (genres.length === 0) {
    return (
      <div className="text-center text-gray-500 py-16">
        <p className="text-5xl mb-4" aria-hidden="true">🏷️</p>
        <p className="text-xl">No genres found.</p>
        <p className="text-sm mt-2">
          {readOnly ? 'Try adjusting your filters.' : 'Add a genre to get started!'}
        </p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
      {genres.map((genre) => (
        <GenreCard
          key={genre.id}
          genre={genre}
          onEdit={onEdit}
          onDelete={onDelete}
          readOnly={readOnly}
        />
      ))}
    </div>
  )
}

function GenreCard({ genre, onEdit, onDelete, readOnly }) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden flex flex-col hover:border-gray-600 transition-colors">
      {genre.hasThumbnail && (
        <img
          src={`${GENRES_API}/${genre.id}/thumbnail`}
          alt={`${genre.name} thumbnail`}
          className="w-full h-48 object-cover"
        />
      )}

      <div className="p-5 flex flex-col gap-3 flex-1">
        <h2 className="text-lg font-semibold text-white leading-tight">{genre.name}</h2>

        {genre.description ? (
          <p className="text-sm text-gray-400 line-clamp-3">{genre.description}</p>
        ) : (
          <p className="text-sm text-gray-500 italic">No description.</p>
        )}

        {!readOnly && (
          <div className="flex gap-2 mt-auto pt-2">
            <button
              onClick={() => onEdit(genre)}
              className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-200 text-sm px-3 py-1.5 rounded-lg transition-colors"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete(genre.id)}
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

