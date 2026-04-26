const SERIES_API = '/api/series'

export default function SeriesList({ series, onEdit, onDelete, onSeriesClick, readOnly = false }) {
  if (series.length === 0) {
    return (
      <div className="text-center text-gray-500 py-16">
        <p className="text-5xl mb-4" aria-hidden="true">🏷️</p>
        <p className="text-xl">No series found.</p>
        <p className="text-sm mt-2">
          {readOnly ? 'Try adjusting your filters.' : 'Add a series to get started!'}
        </p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
      {series.map((series) => (
        <SeriesCard
          key={series.id}
          series={series}
          onEdit={onEdit}
          onDelete={onDelete}
          onSeriesClick={onSeriesClick}
          readOnly={readOnly}
        />
      ))}
    </div>
  )
}

function SeriesCard({ series, onEdit, onDelete, onSeriesClick, readOnly }) {
  const isClickable = typeof onSeriesClick === 'function'

  return (
    <div
      className={`bg-gray-900 border border-gray-800 rounded-xl overflow-hidden flex flex-col transition-colors ${
        isClickable ? 'cursor-pointer hover:border-indigo-500 hover:bg-gray-800/60' : 'hover:border-gray-600'
      }`}
      onClick={isClickable ? () => onSeriesClick(series) : undefined}
    >
      {series.hasThumbnail && (
        <img
          src={`${SERIES_API}/${series.id}/thumbnail`}
          alt={`${series.name} thumbnail`}
          className="w-full h-48 object-cover"
        />
      )}

      <div className="p-5 flex flex-col gap-3 flex-1">
        <div className="flex items-start justify-between gap-2">
          <h2 className="text-lg font-semibold text-white leading-tight">{series.name}</h2>
          {series.episodeCount > 0 && (
            <span
              className="shrink-0 text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full"
              aria-label={`${series.episodeCount} ${series.episodeCount === 1 ? 'episode' : 'episodes'} in this series`}
            >
              {series.episodeCount} {series.episodeCount === 1 ? 'episode' : 'episodes'}
            </span>
          )}
        </div>

      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-400">
        {series.year && <span>📅 {series.year}</span>}
      </div>

        {series.description ? (
          <p className="text-sm text-gray-400 line-clamp-3">{series.description}</p>
        ) : (
          <p className="text-sm text-gray-500 italic">No description.</p>
        )}

        {!readOnly && (
          <div className="flex gap-2 mt-auto pt-2" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => onEdit(series)}
              className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-200 text-sm px-3 py-1.5 rounded-lg transition-colors"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete(series.id)}
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

