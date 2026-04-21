import { useState, useEffect } from 'react'

const EMPTY_FORM = {
  title: '',
  year: '',
  genre: '',
  description: '',
  watched: false,
  tags: [],
}

export default function MovieForm({ movie, onSave, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [tagInput, setTagInput] = useState('')

  useEffect(() => {
    if (movie) {
      setForm({
        ...movie,
        year: movie.year ?? '',
        tags: movie.tags ?? [],
      })
    } else {
      setForm(EMPTY_FORM)
    }
    setErrors({})
    setTagInput('')
  }, [movie])

  const validate = () => {
    const errs = {}
    if (!form.title.trim()) errs.title = 'Title is required.'
    if (form.year && (isNaN(form.year) || form.year < 1888 || form.year > 2100)) {
      errs.year = 'Enter a valid year (1888–2100).'
    }
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
    // Flush any pending tag input on submit
    const pendingTags = tagInput.trim()
      ? [...form.tags, ...tagInput.split(',').map(t => t.trim()).filter(Boolean)]
      : form.tags
    onSave({
      ...form,
      year: form.year !== '' ? Number(form.year) : null,
      tags: pendingTags,
    })
  }

  const isEditing = movie != null

  return (
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
          <label className="block text-sm text-gray-400 mb-1">Genre</label>
          <input
            name="genre"
            value={form.genre}
            onChange={handleChange}
            placeholder="e.g. Drama"
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

      {/* Watched */}
      <label className="flex items-center gap-2 text-sm text-gray-300 cursor-pointer">        <input
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
  )
}
