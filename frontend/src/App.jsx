import { useState, useEffect } from 'react'
import MovieList from './components/MovieList'
import MovieForm from './components/MovieForm'

const API_BASE = '/api/movies'

export default function App() {
  const [movies, setMovies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [editingMovie, setEditingMovie] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [filterWatched, setFilterWatched] = useState('')

  const fetchMovies = async () => {
    try {
      setLoading(true)
      const params = new URLSearchParams()
      if (searchQuery) params.append('title', searchQuery)
      if (filterWatched !== '') params.append('watched', filterWatched)
      const res = await fetch(`${API_BASE}?${params}`)
      if (!res.ok) throw new Error('Failed to fetch movies')
      const data = await res.json()
      setMovies(data)
      setError(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchMovies()
  }, [searchQuery, filterWatched])

  const handleSave = async (movieData) => {
    try {
      const isEdit = movieData.id != null
      const url = isEdit ? `${API_BASE}/${movieData.id}` : API_BASE
      const method = isEdit ? 'PUT' : 'POST'
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(movieData),
      })
      if (!res.ok) throw new Error('Failed to save movie')
      setShowForm(false)
      setEditingMovie(null)
      fetchMovies()
    } catch (err) {
      setError(err.message)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this movie?')) return
    try {
      const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error('Failed to delete movie')
      fetchMovies()
    } catch (err) {
      setError(err.message)
    }
  }

  const handleEdit = (movie) => {
    setEditingMovie(movie)
    setShowForm(true)
  }

  const handleCancel = () => {
    setShowForm(false)
    setEditingMovie(null)
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      {/* Header */}
      <header className="bg-gray-900 border-b border-gray-800 shadow-lg">
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-3xl">🎬</span>
            <h1 className="text-2xl font-bold text-white tracking-tight">MovieNight</h1>
          </div>
          <button
            onClick={() => { setEditingMovie(null); setShowForm(true) }}
            className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-lg font-medium transition-colors"
          >
            + Add Movie
          </button>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-8">
        {/* Search & Filter bar */}
        <div className="flex flex-col sm:flex-row gap-3 mb-6">
          <input
            type="text"
            placeholder="Search by title…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
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

        {/* Error banner */}
        {error && (
          <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
            {error}
          </div>
        )}

        {/* Modal form */}
        {showForm && (
          <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
            <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg">
              <MovieForm
                movie={editingMovie}
                onSave={handleSave}
                onCancel={handleCancel}
              />
            </div>
          </div>
        )}

        {/* Movie list */}
        {loading ? (
          <div className="text-center text-gray-400 py-16">Loading…</div>
        ) : (
          <MovieList movies={movies} onEdit={handleEdit} onDelete={handleDelete} />
        )}
      </main>
    </div>
  )
}
