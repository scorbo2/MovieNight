import { useEffect, useState } from 'react'
import MovieList from '../components/MovieList'
import MovieForm from '../components/MovieForm'
import EpisodeList from '../components/EpisodeList'
import EpisodeForm from '../components/EpisodeForm'

const MOVIES_API = '/api/movies'
const EPISODES_API = '/api/episodes'

export default function MediaLibraryPage({ mode }) {
  const isAdmin = mode === 'admin'
  const [activeTab, setActiveTab] = useState('movies')

  const [movies, setMovies] = useState([])
  const [moviesLoading, setMoviesLoading] = useState(true)
  const [moviesError, setMoviesError] = useState(null)
  const [showMovieForm, setShowMovieForm] = useState(false)
  const [editingMovie, setEditingMovie] = useState(null)
  const [movieSearchQuery, setMovieSearchQuery] = useState('')
  const [filterWatched, setFilterWatched] = useState('')

  const [episodes, setEpisodes] = useState([])
  const [episodesLoading, setEpisodesLoading] = useState(true)
  const [episodesError, setEpisodesError] = useState(null)
  const [showEpisodeForm, setShowEpisodeForm] = useState(false)
  const [editingEpisode, setEditingEpisode] = useState(null)
  const [episodeSeriesQuery, setEpisodeSeriesQuery] = useState('')
  const [episodeSeasonQuery, setEpisodeSeasonQuery] = useState('')
  const [filterEpisodeWatched, setFilterEpisodeWatched] = useState('')

  const fetchMovies = async () => {
    try {
      setMoviesLoading(true)
      const params = new URLSearchParams()
      if (movieSearchQuery) params.append('title', movieSearchQuery)
      if (filterWatched !== '') params.append('watched', filterWatched)
      const res = await fetch(`${MOVIES_API}?${params}`)
      if (!res.ok) throw new Error('Failed to fetch movies')
      setMovies(await res.json())
      setMoviesError(null)
    } catch (err) {
      setMoviesError(err.message)
    } finally {
      setMoviesLoading(false)
    }
  }

  useEffect(() => {
    fetchMovies()
  }, [movieSearchQuery, filterWatched])

  const fetchEpisodes = async () => {
    try {
      setEpisodesLoading(true)
      const params = new URLSearchParams()
      if (episodeSeriesQuery) params.append('seriesName', episodeSeriesQuery)
      if (episodeSeasonQuery !== '') params.append('season', episodeSeasonQuery)
      if (filterEpisodeWatched !== '') params.append('watched', filterEpisodeWatched)
      const res = await fetch(`${EPISODES_API}?${params}`)
      if (!res.ok) throw new Error('Failed to fetch episodes')
      setEpisodes(await res.json())
      setEpisodesError(null)
    } catch (err) {
      setEpisodesError(err.message)
    } finally {
      setEpisodesLoading(false)
    }
  }

  useEffect(() => {
    if (activeTab !== 'episodes') return
    fetchEpisodes()
  }, [activeTab, episodeSeriesQuery, episodeSeasonQuery, filterEpisodeWatched])

  const handleSaveMovie = async (movieData) => {
    try {
      const { _thumbnail, _clearThumbnail, ...data } = movieData
      const isEdit = data.id != null
      const url = isEdit ? `${MOVIES_API}/${data.id}` : MOVIES_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error('Failed to save movie')
      const saved = await res.json()
      if (_clearThumbnail) {
        const thumbnailRes = await fetch(`${MOVIES_API}/${saved.id}/thumbnail`, { method: 'DELETE' })
        if (!thumbnailRes.ok) throw new Error('Failed to delete movie thumbnail')
      } else if (_thumbnail) {
        const formData = new FormData()
        formData.append('file', _thumbnail)
        const thumbnailRes = await fetch(`${MOVIES_API}/${saved.id}/thumbnail`, { method: 'POST', body: formData })
        if (!thumbnailRes.ok) throw new Error('Failed to upload movie thumbnail')
      }
      setShowMovieForm(false)
      setEditingMovie(null)
      fetchMovies()
    } catch (err) {
      setMoviesError(err.message)
    }
  }

  const handleDeleteMovie = async (id) => {
    if (!confirm('Delete this movie?')) return
    try {
      const res = await fetch(`${MOVIES_API}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error('Failed to delete movie')
      fetchMovies()
    } catch (err) {
      setMoviesError(err.message)
    }
  }

  const handleSaveEpisode = async (episodeData) => {
    try {
      const { _thumbnail, _clearThumbnail, ...data } = episodeData
      const isEdit = data.id != null
      const url = isEdit ? `${EPISODES_API}/${data.id}` : EPISODES_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error('Failed to save episode')
      const saved = await res.json()
      if (_clearThumbnail) {
        const thumbnailRes = await fetch(`${EPISODES_API}/${saved.id}/thumbnail`, { method: 'DELETE' })
        if (!thumbnailRes.ok) throw new Error('Failed to clear episode thumbnail')
      } else if (_thumbnail) {
        const formData = new FormData()
        formData.append('file', _thumbnail)
        const thumbnailRes = await fetch(`${EPISODES_API}/${saved.id}/thumbnail`, { method: 'POST', body: formData })
        if (!thumbnailRes.ok) throw new Error('Failed to upload episode thumbnail')
      }
      setShowEpisodeForm(false)
      setEditingEpisode(null)
      fetchEpisodes()
    } catch (err) {
      setEpisodesError(err.message)
    }
  }

  const handleDeleteEpisode = async (id) => {
    if (!confirm('Delete this episode?')) return
    try {
      const res = await fetch(`${EPISODES_API}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error('Failed to delete episode')
      fetchEpisodes()
    } catch (err) {
      setEpisodesError(err.message)
    }
  }

  const adminActionLabel = activeTab === 'movies' ? '+ Add Movie' : '+ Add Episode'

  return (
    <>
      <section className="mb-6 rounded-xl border border-gray-800 bg-gray-900 px-5 py-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-indigo-300">
              {isAdmin ? 'Admin interface' : 'Read-only interface'}
            </p>
            <h2 className="mt-1 text-xl font-semibold text-white">
              {isAdmin ? 'Manage your media library' : 'Browse your media library'}
            </h2>
            <p className="mt-1 text-sm text-gray-400">
              {isAdmin
                ? 'Create, edit, and delete entries from this machine only. Basic auth is required for admin access.'
                : 'Browse movies and episodes without any write controls.'}
            </p>
          </div>
          {isAdmin && (
            <button
              onClick={() => {
                if (activeTab === 'movies') {
                  setEditingMovie(null)
                  setShowMovieForm(true)
                } else {
                  setEditingEpisode(null)
                  setShowEpisodeForm(true)
                }
              }}
              className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-lg font-medium transition-colors"
            >
              {adminActionLabel}
            </button>
          )}
        </div>
      </section>

      <div className="flex gap-1 mb-6 bg-gray-900 border border-gray-800 rounded-lg p-1 w-fit">
        <button
          onClick={() => {
            setActiveTab('movies')
            setShowEpisodeForm(false)
            setEditingEpisode(null)
          }}
          className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'movies'
              ? 'bg-indigo-600 text-white'
              : 'text-gray-400 hover:text-gray-200'
          }`}
        >
          🎬 Movies
        </button>
        <button
          onClick={() => {
            setActiveTab('episodes')
            setShowMovieForm(false)
            setEditingMovie(null)
          }}
          className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'episodes'
              ? 'bg-indigo-600 text-white'
              : 'text-gray-400 hover:text-gray-200'
          }`}
        >
          📺 Episodes
        </button>
      </div>

      {activeTab === 'movies' && (
        <>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="text"
              placeholder="Search by title…"
              value={movieSearchQuery}
              onChange={(e) => setMovieSearchQuery(e.target.value)}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
            <select
              value={filterWatched}
              onChange={(e) => setFilterWatched(e.target.value)}
              className="bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
            >
              <option value="">All movies</option>
              <option value="true">Watched</option>
              <option value="false">Unwatched</option>
            </select>
          </div>

          {moviesError && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {moviesError}
            </div>
          )}

          {isAdmin && showMovieForm && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg">
                <MovieForm
                  movie={editingMovie}
                  onSave={handleSaveMovie}
                  onCancel={() => {
                    setShowMovieForm(false)
                    setEditingMovie(null)
                  }}
                />
              </div>
            </div>
          )}

          {moviesLoading ? (
            <div className="text-center text-gray-400 py-16">Loading…</div>
          ) : (
            <MovieList
              movies={movies}
              onEdit={(movie) => {
                setEditingMovie(movie)
                setShowMovieForm(true)
              }}
              onDelete={handleDeleteMovie}
              readOnly={!isAdmin}
            />
          )}
        </>
      )}

      {activeTab === 'episodes' && (
        <>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="text"
              placeholder="Search by series name…"
              value={episodeSeriesQuery}
              onChange={(e) => setEpisodeSeriesQuery(e.target.value)}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
            <input
              type="number"
              placeholder="Season"
              value={episodeSeasonQuery}
              onChange={(e) => setEpisodeSeasonQuery(e.target.value)}
              min="1"
              className="w-28 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
            <select
              value={filterEpisodeWatched}
              onChange={(e) => setFilterEpisodeWatched(e.target.value)}
              className="bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
            >
              <option value="">All episodes</option>
              <option value="true">Watched</option>
              <option value="false">Unwatched</option>
            </select>
          </div>

          {episodesError && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {episodesError}
            </div>
          )}

          {isAdmin && showEpisodeForm && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg overflow-y-auto max-h-[90vh]">
                <EpisodeForm
                  episode={editingEpisode}
                  onSave={handleSaveEpisode}
                  onCancel={() => {
                    setShowEpisodeForm(false)
                    setEditingEpisode(null)
                  }}
                />
              </div>
            </div>
          )}

          {episodesLoading ? (
            <div className="text-center text-gray-400 py-16">Loading…</div>
          ) : (
            <EpisodeList
              episodes={episodes}
              onEdit={(episode) => {
                setEditingEpisode(episode)
                setShowEpisodeForm(true)
              }}
              onDelete={handleDeleteEpisode}
              readOnly={!isAdmin}
            />
          )}
        </>
      )}
    </>
  )
}
