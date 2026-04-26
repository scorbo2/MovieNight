import { useState, useEffect, useRef } from 'react'
import FileBrowserModal from './FileBrowserModal'

const EPISODES_API = '/api/episodes'
const SERIES_API = '/api/series'
const LAST_DIR_KEY = 'movienight:lastBrowseDir'

const EMPTY_FORM = {
  seriesId: '',
  episodeTitle: '',
  season: '',
  episode: '',
  description: '',
  tags: [],
  videoFilePath: '',
}

export default function EpisodeForm({ episode, onSave, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [series, setSeries] = useState([])
  const [seriesLoading, setSeriesLoading] = useState(true)
  const [seriesError, setSeriesError] = useState(null)
  const [errors, setErrors] = useState({})
  const [tagInput, setTagInput] = useState('')
  const [thumbnailFile, setThumbnailFile] = useState(null)
  const [thumbnailPreview, setThumbnailPreview] = useState(null)
  const [clearThumbnail, setClearThumbnail] = useState(false)
  const [showFileBrowser, setShowFileBrowser] = useState(false)
  const [initialBrowsePath] = useState(() => {
    try {
      return sessionStorage.getItem(LAST_DIR_KEY) || '/'
    } catch {
      return '/'
    }
  })
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

    const fetchSeries = async () => {
      try {
        setSeriesLoading(true)
        const res = await fetch(SERIES_API)
        if (!res.ok) throw new Error('Failed to fetch series')
        const data = await res.json()
        if (!ignore) {
          const sortedSeries = Array.isArray(data)
            ? [...data].sort((a, b) => a.name.localeCompare(b.name))
            : []
          setSeries(sortedSeries)
          setSeriesError(null)
        }
      } catch {
        if (!ignore) setSeriesError('Failed to load series.')
      } finally {
        if (!ignore) setSeriesLoading(false)
      }
    }

    fetchSeries()

    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    // Revoke any existing object URL when the episode prop changes (form reset)
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current)
      objectUrlRef.current = null
    }
    if (episode) {
      setForm({
        id: episode.id,
        episodeTitle: episode.episodeTitle ?? '',
        description: episode.description ?? '',
        videoFilePath: episode.videoFilePath ?? '',
        seriesId: episode.series?.id != null ? String(episode.series.id) : '',
        season: episode.season ?? '',
        episode: episode.episode ?? '',
        tags: episode.tags ?? [],
      })
      setThumbnailPreview(episode.hasThumbnail ? `${EPISODES_API}/${episode.id}/thumbnail` : null)
    } else {
      setForm(EMPTY_FORM)
      setThumbnailPreview(null)
    }
    setErrors({})
    setTagInput('')
    setThumbnailFile(null)
    setClearThumbnail(false)
  }, [episode])

  const validate = () => {
    const errs = {}
    if (!form.seriesId) errs.seriesId = 'Series is required.'
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
    const pendingTags = tagInput.trim()
      ? [...form.tags, ...tagInput.split(',').map(t => t.trim()).filter(Boolean)]
      : form.tags
    onSave({
        id: form.id,
        series: { id: Number(form.seriesId) },
        episodeTitle: form.episodeTitle.trim() || null,
        description: form.description.trim() || null,
        videoFilePath: form.videoFilePath.trim(),
      season: form.season !== '' ? Number(form.season) : null,
      episode: form.episode !== '' ? Number(form.episode) : null,
      tags: pendingTags,
      _thumbnail: thumbnailFile,
      _clearThumbnail: clearThumbnail,
    })
  }

  const isEditing = episode != null
  const hasMissingSelectedSeries = form.seriesId && !series.some((g) => String(g.id) === String(form.seriesId))
  const noSeriesAvailable = !seriesLoading && series.length === 0

  return (
    <>
    <form onSubmit={handleSubmit} className="p-6 flex flex-col gap-4">
      <h2 className="text-xl font-bold text-white">{isEditing ? 'Edit Episode' : 'Add Episode'}</h2>

      {/* Series Name */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Series *</label>
        <select
          name="seriesId"
          value={form.seriesId}
          onChange={handleChange}
          disabled={seriesLoading || series.length === 0}
          className={`w-full bg-gray-800 border rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 ${errors.seriesId ? 'border-red-500' : 'border-gray-700'}`}
         >
              <option value="">{seriesLoading ? 'Loading series…' : 'Select a series'}</option>
              {series.map((series) => (
                <option key={series.id} value={String(series.id)}>
                  {series.name}
                </option>
              ))}
            </select>
            {errors.seriesId && <p className="text-red-400 text-xs mt-1">{errors.seriesId}</p>}
            {hasMissingSelectedSeries && (
              <p className="text-red-400 text-xs mt-1">The previously selected series no longer exists. Please select a valid series.</p>
            )}
            {seriesError && <p className="text-red-400 text-xs mt-1">{seriesError}</p>}
            {noSeriesAvailable && !seriesError && (
              <p className="text-amber-300 text-xs mt-1">No series exist yet. Create one first, then add episodes.</p>
            )}
      </div>

      {/* Episode Title */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Episode Title</label>
        <input
          name="episodeTitle"
          value={form.episodeTitle}
          onChange={handleChange}
          placeholder="e.g. Hungry Man"
          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
        />
      </div>

      {/* Season & Episode */}
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-sm text-gray-400 mb-1">Season</label>
          <input
            name="season"
            type="number"
            value={form.season}
            onChange={handleChange}
            placeholder="e.g. 4"
            min="1"
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-400 mb-1">Episode</label>
          <input
            name="episode"
            type="number"
            value={form.episode}
            onChange={handleChange}
            placeholder="e.g. 9"
            min="1"
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
          />
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
              className="rounded-lg max-h-40 object-cover object-top border border-gray-700"
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
                setClearThumbnail(episode?.hasThumbnail ?? false)
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
        <p className="text-xs text-gray-500 mt-1">JPEG or PNG, Recommend 2:1 aspect ratio</p>
      </div>

      {/* Video File Path */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Video File Path *</label>
        <div className="flex gap-2">
          <input
            name="videoFilePath"
            value={form.videoFilePath}
            onChange={handleChange}
            placeholder="/path/to/episode.mkv"
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

      {/* Buttons */}
      <div className="flex gap-3 pt-2">
        <button
          type="submit"
            disabled={noSeriesAvailable || seriesLoading || Boolean(seriesError) || Boolean(hasMissingSelectedSeries)}
          className="flex-1 bg-indigo-600 hover:bg-indigo-500 disabled:bg-gray-700 disabled:text-gray-400 disabled:cursor-not-allowed text-white font-medium py-2 rounded-lg transition-colors"
        >
          {isEditing ? 'Save Changes' : 'Add Episode'}
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
          const normalizedPath = typeof path === 'string' ? path.trim() : ''
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
