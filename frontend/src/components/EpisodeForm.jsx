import { useState, useEffect, useRef } from 'react'

const EPISODES_API = '/api/episodes'

const EMPTY_FORM = {
  seriesName: '',
  episodeTitle: '',
  season: '',
  episode: '',
  description: '',
  watched: false,
  tags: [],
}

export default function EpisodeForm({ episode, onSave, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [tagInput, setTagInput] = useState('')
  const [thumbnailFile, setThumbnailFile] = useState(null)
  const [thumbnailPreview, setThumbnailPreview] = useState(null)
  const [clearThumbnail, setClearThumbnail] = useState(false)
  const fileInputRef = useRef(null)

  useEffect(() => {
    if (episode) {
      setForm({
        ...episode,
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
    if (!form.seriesName.trim()) errs.seriesName = 'Series name is required.'
    return errs
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
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
      ...form,
      season: form.season !== '' ? Number(form.season) : null,
      episode: form.episode !== '' ? Number(form.episode) : null,
      tags: pendingTags,
      _thumbnail: thumbnailFile,
      _clearThumbnail: clearThumbnail,
    })
  }

  const isEditing = episode != null

  return (
    <form onSubmit={handleSubmit} className="p-6 flex flex-col gap-4">
      <h2 className="text-xl font-bold text-white">{isEditing ? 'Edit Episode' : 'Add Episode'}</h2>

      {/* Series Name */}
      <div>
        <label className="block text-sm text-gray-400 mb-1">Series Name *</label>
        <input
          name="seriesName"
          value={form.seriesName}
          onChange={handleChange}
          placeholder="e.g. Dexter"
          className={`w-full bg-gray-800 border rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 ${errors.seriesName ? 'border-red-500' : 'border-gray-700'}`}
        />
        {errors.seriesName && <p className="text-red-400 text-xs mt-1">{errors.seriesName}</p>}
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
              className="rounded-lg max-h-40 object-cover border border-gray-700"
            />
            <button
              type="button"
              onClick={() => {
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
              setThumbnailFile(file)
              setClearThumbnail(false)
              setThumbnailPreview(URL.createObjectURL(file))
            }
          }}
          className="block w-full text-sm text-gray-400 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-sm file:bg-gray-700 file:text-gray-200 hover:file:bg-gray-600 cursor-pointer"
        />
        <p className="text-xs text-gray-500 mt-1">JPEG or PNG, 26×26 to 2000×2000</p>
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
          className="flex-1 bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-2 rounded-lg transition-colors"
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
  )
}
