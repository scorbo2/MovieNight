import { useState, useEffect } from 'react'
import MovieList from './components/MovieList'
import MovieForm from './components/MovieForm'
import EpisodeList from './components/EpisodeList'
import EpisodeForm from './components/EpisodeForm'

const MOVIES_API = '/api/movies'
const EPISODES_API = '/api/episodes'

export default function App() {
  const [activeTab, setActiveTab] = useState('movies')

  // Movies state
  const [movies, setMovies] = useState([])
  const [moviesLoading, setMoviesLoading] = useState(true)
  const [moviesError, setMoviesError] = useState(null)
  const [showMovieForm, setShowMovieForm] = useState(false)
  const [editingMovie, setEditingMovie] = useState(null)
  const [movieSearchQuery, setMovieSearchQuery] = useState('')
  const [filterWatched, setFilterWatched] = useState('')

  // Episodes state
  const [episodes, setEpisodes] = useState([])
  const [episodesLoading, setEpisodesLoading] = useState(true)
  const [episodesError, setEpisodesError] = useState(null)
  const [showEpisodeForm, setShowEpisodeForm] = useState(false)
  const [editingEpisode, setEditingEpisode] = useState(null)
  const [episodeSeriesQuery, setEpisodeSeriesQuery] = useState('')
  const [episodeSeasonQuery, setEpisodeSeasonQuery] = useState('')
  const [filterEpisodeWatched, setFilterEpisodeWatched] = useState('')

  // --- Movies ---

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

  useEffect(() => { fetchMovies() }, [movieSearchQuery, filterWatched])

  const handleSaveMovie = async (movieData) => {
    try {
      const isEdit = movieData.id != null
      const url = isEdit ? `${MOVIES_API}/${movieData.id}` : MOVIES_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(movieData),
      })
      if (!res.ok) throw new Error('Failed to save movie')
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

  // --- Episodes ---

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

  const handleSaveEpisode = async (episodeData) => {
    try {
      const isEdit = episodeData.id != null
      const url = isEdit ? `${EPISODES_API}/${episodeData.id}` : EPISODES_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(episodeData),
      })
      if (!res.ok) throw new Error('Failed to save episode')
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

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      {/* Header */}
      <header className="bg-gray-900 border-b border-gray-800 shadow-lg">
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-3xl" aria-hidden="true">🎬</span>
            <h1 className="text-2xl font-bold text-white tracking-tight">MovieNight</h1>
          </div>
          {activeTab === 'movies' ? (
            <button
              onClick={() => { setEditingMovie(null); setShowMovieForm(true) }}
              className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-lg font-medium transition-colors"
            >
              + Add Movie
            </button>
          ) : (
            <button
              onClick={() => { setEditingEpisode(null); setShowEpisodeForm(true) }}
              className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-lg font-medium transition-colors"
            >
              + Add Episode
            </button>
          )}
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-8">
        {/* Tabs */}
        <div className="flex gap-1 mb-6 bg-gray-900 border border-gray-800 rounded-lg p-1 w-fit">
          <button
            onClick={() => setActiveTab('movies')}
            className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
              activeTab === 'movies'
                ? 'bg-indigo-600 text-white'
                : 'text-gray-400 hover:text-gray-200'
            }`}
          >
            🎬 Movies
          </button>
          <button
            onClick={() => setActiveTab('episodes')}
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
            {/* Movie Search & Filter */}
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

            {showMovieForm && (
              <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
                <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg">
                  <MovieForm
                    movie={editingMovie}
                    onSave={handleSaveMovie}
                    onCancel={() => { setShowMovieForm(false); setEditingMovie(null) }}
                  />
                </div>
              </div>
            )}

            {moviesLoading ? (
              <div className="text-center text-gray-400 py-16">Loading…</div>
            ) : (
              <MovieList
                movies={movies}
                onEdit={(m) => { setEditingMovie(m); setShowMovieForm(true) }}
                onDelete={handleDeleteMovie}
              />
            )}
          </>
        )}

        {activeTab === 'episodes' && (
          <>
            {/* Episode Search & Filter */}
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

            {showEpisodeForm && (
              <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
                <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg overflow-y-auto max-h-[90vh]">
                  <EpisodeForm
                    episode={editingEpisode}
                    onSave={handleSaveEpisode}
                    onCancel={() => { setShowEpisodeForm(false); setEditingEpisode(null) }}
                  />
                </div>
              </div>
            )}

            {episodesLoading ? (
              <div className="text-center text-gray-400 py-16">Loading…</div>
            ) : (
              <EpisodeList
                episodes={episodes}
                onEdit={(ep) => { setEditingEpisode(ep); setShowEpisodeForm(true) }}
                onDelete={handleDeleteEpisode}
              />
            )}
          </>
        )}
      </main>
    </div>
  )
}
