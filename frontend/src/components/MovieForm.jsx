import { useState, useEffect, useRef } from 'react'
import FileBrowserModal from './FileBrowserModal'

const MOVIES_API = '/api/movies'
const GENRES_API = '/api/genres'
const LAST_DIR_KEY = 'movienight:lastBrowseDir'

const EMPTY_FORM = {
  id: null,
  title: '',
  year: '',
  genreId: '',
  description: '',
  watched: false,
  tags: [],
  videoFilePath: '',
}

export default function MovieForm({ movie, onSave, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [genres, setGenres] = useState([])
  const [genresLoading, setGenresLoading] = useState(true)
  const [genresError, setGenresError] = useState(null)
  const [errors, setErrors] = useState({})
  const [tagInput, setTagInput] = useState('')
  const [thumbnailFile, setThumbnailFile] = useState(null)
  const [thumbnailPreview, setThumbnailPreview] = useState(null)
  const [clearThumbnail, setClearThumbnail] = useState(false)
  const [showFileBrowser, setShowFileBrowser] = useState(false)
  const initialBrowsePath = sessionStorage.getItem(LAST_DIR_KEY) || '/'
  const fileInputRef = useRef(null)
  const objectUrlRef = useRef(null)

  // Revoke any tracked object URL on unmount to avoid memory leaks
  useEffect(() => {
    return () => {
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current)
      }
    }
  }, [])

  useEffect(() => {
    let ignore = false

    const fetchGenres = async () => {
      try {
        setGenresLoading(true)
        const res = await fetch(GENRES_API)
        if (!res.ok) throw new Error('Failed to fetch genres')
        const data = await res.json()
        if (!ignore) {
          const sortedGenres = Array.isArray(data)
            ? [...data].sort((a, b) => a.name.localeCompare(b.name))
            : []
          setGenres(sortedGenres)
          setGenresError(null)
        }
      } catch {
        if (!ignore) setGenresError('Failed to load genres.')
      } finally {
        if (!ignore) setGenresLoading(false)
      }
    }

    fetchGenres()

    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    // Revoke any existing object URL when the movie prop changes (form reset)
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current)
      objectUrlRef.current = null
    }
    if (movie) {
      setForm({
        id: movie.id,
        title: movie.title ?? '',
        year: movie.year ?? '',
        genreId: movie.genre?.id != null ? String(movie.genre.id) : '',
        description: movie.description ?? '',
        watched: Boolean(movie.watched),
        tags: movie.tags ?? [],
        videoFilePath: movie.videoFilePath ?? '',
      })
      setThumbnailPreview(movie.hasThumbnail ? `${MOVIES_API}/${movie.id}/thumbnail` : null)
    } else {
      setForm(EMPTY_FORM)
      setThumbnailPreview(null)
    }
    setErrors({})
    setTagInput('')
    setThumbnailFile(null)
    setClearThumbnail(false)
  }, [movie])

  const validate = () => {
    const errs = {}
    if (!form.title.trim()) errs.title = 'Title is required.'
    if (!form.genreId) errs.genreId = 'Genre is required.'
    if (form.year && (isNaN(form.year) || form.year < 1888 || form.year > 2100)) {
      errs.year = 'Enter a valid year (1888–2100).'
    }
    if (!form.videoFilePath || !form.videoFilePath.trim()) errs.videoFilePath = 'Video file path is required.'
    return errs
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }))
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      return
    }
    // Flush any pending tag input on submit
    const pendingTags = tagInput.trim()
      ? [...form.tags, ...tagInput.split(',').map(t => t.trim()).filter(Boolean)]
      : form.tags
    onSave({
      id: form.id,
      title: form.title,
      year: form.year !== '' ? Number(form.year) : null,
      genre: { id: Number(form.genreId) },
      description: form.description,
      watched: form.watched,
      tags: pendingTags,
      videoFilePath: form.videoFilePath,
      _thumbnail: thumbnailFile,
      _clearThumbnail: clearThumbnail,
    })
  }

  const isEditing = movie != null
  const hasMissingSelectedGenre = form.genreId && !genres.some((g) => String(g.id) === String(form.genreId))
  const noGenresAvailable = !genresLoading && genres.length === 0

  return (
    <>
    <form onSubmit={handleSubmit} className="p-6 flex flex-col gap-4">
      <h2 className="text-xl font-bold text-white">{isEditing ? 'Edit Movie' : 'Add Movie'}</h2>

      {/* Title */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Title *</label>
        <input
          name="title"
          value={form.title}
          onChange={handleChange}
          className={`w-full bg-gray-800 border rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 ${errors.title ? 'border-red-500' : 'border-gray-700'}`}
        />
        {errors.title && <p className="text-red-400 text-xs mt-1">{errors.title}</p>}
      </div>

      {/* Year & Genre */}
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-sm text-gray-400 mb-1">Year</label>
          <input
            name="year"
            type="number"
            value={form.year}
            onChange={handleChange}
            placeholder="e.g. 2024"
            className={`w-full bg-gray-800 border rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 ${errors.year ? 'border-red-500' : 'border-gray-700'}`}
          />
          {errors.year && <p className="text-red-400 text-xs mt-1">{errors.year}</p>}
        </div>
        <div>
          <label className="block text-sm text-gray-400 mb-1">Genre *</label>
          <select
            name="genreId"
            value={form.genreId}
            onChange={handleChange}
            disabled={genresLoading || genres.length === 0}
            className={`w-full bg-gray-800 border rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 ${errors.genreId ? 'border-red-500' : 'border-gray-700'}`}
          >
            <option value="">{genresLoading ? 'Loading genres…' : 'Select a genre'}</option>
            {genres.map((genre) => (
              <option key={genre.id} value={String(genre.id)}>
                {genre.name}
              </option>
            ))}
          </select>
          {errors.genreId && <p className="text-red-400 text-xs mt-1">{errors.genreId}</p>}
          {hasMissingSelectedGenre && (
            <p className="text-red-400 text-xs mt-1">The previously selected genre no longer exists. Please select a valid genre.</p>
          )}
          {genresError && <p className="text-red-400 text-xs mt-1">{genresError}</p>}
          {noGenresAvailable && !genresError && (
            <p className="text-amber-300 text-xs mt-1">No genres exist yet. Create one first, then add movies.</p>
          )}
        </div>
      </div>

      {/* Description */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Description</label>
        <textarea
          name="description"
          value={form.description}
          onChange={handleChange}
          rows={3}
          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 resize-none"
        />
      </div>

      {/* Tags */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Tags</label>
        <div className="flex gap-2">
          <input
            value={tagInput}
            onChange={(e) => setTagInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ',') {
                e.preventDefault()
                const newTags = tagInput.split(',').map(t => t.trim()).filter(Boolean)
                if (newTags.length) {
                  setForm(prev => ({ ...prev, tags: [...prev.tags, ...newTags] }))
                  setTagInput('')
                }
              }
            }}
            placeholder="Type a tag and press Enter"
            className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
          />
          <button
            type="button"
            onClick={() => {
              const newTags = tagInput.split(',').map(t => t.trim()).filter(Boolean)
              if (newTags.length) {
                setForm(prev => ({ ...prev, tags: [...prev.tags, ...newTags] }))
                setTagInput('')
              }
            }}
            className="bg-gray-700 hover:bg-gray-600 text-gray-200 px-3 py-2 rounded-lg text-sm transition-colors"
          >
            Add
          </button>
        </div>
        {form.tags.length > 0 && (
          <div className="flex flex-wrap gap-2 mt-2">
            {form.tags.map((tag, i) => (
              <span key={i} className="flex items-center gap-1 bg-indigo-800/50 text-indigo-300 text-xs px-2 py-0.5 rounded-full">
                {tag}
                <button
                  type="button"
                  onClick={() => setForm(prev => ({ ...prev, tags: prev.tags.filter((_, j) => j !== i) }))}
                  className="hover:text-white ml-0.5"
                  aria-label={`Remove tag ${tag}`}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Thumbnail */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Thumbnail</label>
        {thumbnailPreview && !clearThumbnail && (
          <div className="mb-2 relative inline-block">
            <img
              src={thumbnailPreview}
              alt="Thumbnail preview"
              className="rounded-lg max-h-40 object-cover border border-gray-700"
            />
            <button
              type="button"
              onClick={() => {
                if (objectUrlRef.current) {
                  URL.revokeObjectURL(objectUrlRef.current)
                  objectUrlRef.current = null
                }
                setThumbnailPreview(null)
                setThumbnailFile(null)
                setClearThumbnail(movie?.hasThumbnail ?? false)
                if (fileInputRef.current) fileInputRef.current.value = ''
              }}
              className="absolute top-1 right-1 bg-gray-900/80 hover:bg-red-800/80 text-gray-300 hover:text-white rounded-full w-6 h-6 flex items-center justify-center text-xs leading-none"
              aria-label="Remove thumbnail"
            >
              ×
            </button>
          </div>
        )}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png"
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) {
              if (objectUrlRef.current) {
                URL.revokeObjectURL(objectUrlRef.current)
              }
              const url = URL.createObjectURL(file)
              objectUrlRef.current = url
              setThumbnailFile(file)
              setClearThumbnail(false)
              setThumbnailPreview(url)
            }
          }}
          className="block w-full text-sm text-gray-400 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-sm file:bg-gray-700 file:text-gray-200 hover:file:bg-gray-600 cursor-pointer"
        />
        <p className="text-xs text-gray-500 mt-1">JPEG or PNG, 26×26 to 2000×2000</p>
      </div>

      {/* Video File Path */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Video File Path *</label>
        <div className="flex gap-2">
          <input
            name="videoFilePath"
            value={form.videoFilePath}
            onChange={handleChange}
            placeholder="/path/to/movie.mkv"
            className={`flex-1 bg-gray-800 border rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 font-mono text-sm ${errors.videoFilePath ? 'border-red-500' : 'border-gray-700'}`}
          />
          <button
            type="button"
            onClick={() => setShowFileBrowser(true)}
            className="bg-gray-700 hover:bg-gray-600 text-gray-200 px-3 py-2 rounded-lg text-sm transition-colors whitespace-nowrap"
          >
            Browse…
          </button>
        </div>
        {errors.videoFilePath && <p className="text-red-400 text-xs mt-1">{errors.videoFilePath}</p>}
      </div>

      {/* Watched */}
      <label className="flex items-center gap-2 text-sm text-gray-300 cursor-pointer">
        <input
          name="watched"
          type="checkbox"
          checked={form.watched}
          onChange={handleChange}
          className="w-4 h-4 accent-indigo-500"
        />
        Marked as watched
      </label>

      {/* Buttons */}
      <div className="flex gap-3 pt-2">
        <button
          type="submit"
          disabled={noGenresAvailable || genresLoading || Boolean(genresError) || Boolean(hasMissingSelectedGenre)}
          className="flex-1 bg-indigo-600 hover:bg-indigo-500 disabled:bg-gray-700 disabled:text-gray-400 disabled:cursor-not-allowed text-white font-medium py-2 rounded-lg transition-colors"
        >
          {isEditing ? 'Save Changes' : 'Add Movie'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="flex-1 bg-gray-700 hover:bg-gray-600 text-gray-200 font-medium py-2 rounded-lg transition-colors"
        >
          Cancel
        </button>
      </div>
    </form>

    {showFileBrowser && (
      <FileBrowserModal
        initialPath={form.videoFilePath || initialBrowsePath}
        onSelect={(path) => {
          const normalizedPath = path.trim()
          setForm((prev) => ({ ...prev, videoFilePath: normalizedPath }))
          setErrors((prev) => ({ ...prev, videoFilePath: undefined }))
          setShowFileBrowser(false)
        }}
        onClose={() => setShowFileBrowser(false)}
      />
    )}
    </>
  )
}
