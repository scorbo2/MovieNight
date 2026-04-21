import { useState, useEffect } from 'react'

const EMPTY_FORM = {
  title: '',
  year: '',
  genre: '',
  director: '',
  rating: '',
  description: '',
  watched: false,
}

export default function MovieForm({ movie, onSave, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    if (movie) {
      setForm({
        ...movie,
        year: movie.year ?? '',
        rating: movie.rating ?? '',
      })
    } else {
      setForm(EMPTY_FORM)
    }
    setErrors({})
  }, [movie])

  const validate = () => {
    const errs = {}
    if (!form.title.trim()) errs.title = 'Title is required.'
    if (form.year && (isNaN(form.year) || form.year < 1888 || form.year > 2100)) {
      errs.year = 'Enter a valid year (1888–2100).'
    }
    if (form.rating && (isNaN(form.rating) || form.rating < 0 || form.rating > 10)) {
      errs.rating = 'Rating must be between 0 and 10.'
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
    onSave({
      ...form,
      year: form.year !== '' ? Number(form.year) : null,
      rating: form.rating !== '' ? Number(form.rating) : null,
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

      {/* Director & Rating */}
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-sm text-gray-400 mb-1">Director</label>
          <input
            name="director"
            value={form.director}
            onChange={handleChange}
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-400 mb-1">Rating (0–10)</label>
          <input
            name="rating"
            type="number"
            step="0.1"
            min="0"
            max="10"
            value={form.rating}
            onChange={handleChange}
            className={`w-full bg-gray-800 border rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 ${errors.rating ? 'border-red-500' : 'border-gray-700'}`}
          />
          {errors.rating && <p className="text-red-400 text-xs mt-1">{errors.rating}</p>}
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
