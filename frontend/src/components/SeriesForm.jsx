import { useEffect, useRef, useState } from 'react'

const SERIES_API = '/api/series'

const EMPTY_FORM = {
  id: null,
  name: '',
  description: '',
  year: '',
}

export default function SeriesForm({ series, onSave, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [thumbnailFile, setThumbnailFile] = useState(null)
  const [thumbnailPreview, setThumbnailPreview] = useState(null)
  const [clearThumbnail, setClearThumbnail] = useState(false)
  const fileInputRef = useRef(null)
  const objectUrlRef = useRef(null)

  useEffect(() => {
    return () => {
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current)
      }
    }
  }, [])

  useEffect(() => {
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current)
      objectUrlRef.current = null
    }

    if (series) {
      setForm({
        id: series.id,
        name: series.name ?? '',
        description: series.description ?? '',
        year: series.year ?? '',
      })
      setThumbnailPreview(series.hasThumbnail ? `${SERIES_API}/${series.id}/thumbnail` : null)
    } else {
      setForm(EMPTY_FORM)
      setThumbnailPreview(null)
    }

    setErrors({})
    setThumbnailFile(null)
    setClearThumbnail(false)
  }, [series])

  const validate = () => {
    const nextErrors = {}
    if (!form.name.trim()) {
      nextErrors.name = 'Name is required.'
    }
    if (form.year && (isNaN(form.year) || form.year < 1888 || form.year > 2100)) {
      nextErrors.year = 'Enter a valid year (1888–2100).'
    }
    return nextErrors
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }))
    }
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    const nextErrors = validate()
    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors)
      return
    }

    onSave({
      id: form.id,
      name: form.name.trim(),
      description: form.description?.trim() || null,
      _thumbnail: thumbnailFile,
      _clearThumbnail: clearThumbnail,
      year: form.year !== '' ? Number(form.year) : null,
    })
  }

  const isEditing = series != null

  return (
    <form onSubmit={handleSubmit} className="p-6 flex flex-col gap-4">
      <h2 className="text-xl font-bold text-white">{isEditing ? 'Edit Series' : 'Add Series'}</h2>

      <div>
        <label className="block text-sm text-gray-400 mb-1">Name *</label>
        <input
          name="name"
          value={form.name}
          onChange={handleChange}
          placeholder="e.g. The Office"
          className={`w-full bg-gray-800 border rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 ${errors.name ? 'border-red-500' : 'border-gray-700'}`}
        />
        {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name}</p>}
      </div>

      <div>
        <label className="block text-sm text-gray-400 mb-1">Description</label>
        <textarea
          name="description"
          value={form.description}
          onChange={handleChange}
          rows={4}
          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-gray-100 focus:outline-none focus:border-indigo-500 resize-none"
        />
      </div>

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
        <label className="block text-sm text-gray-400 mb-1">Thumbnail</label>
        {thumbnailPreview && !clearThumbnail && (
          <div className="mb-2 relative inline-block">
            <img
              src={thumbnailPreview}
              alt="Series thumbnail preview"
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
                setClearThumbnail(series?.hasThumbnail ?? false)
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
          onChange={(event) => {
            const file = event.target.files?.[0]
            if (!file) return

            if (objectUrlRef.current) {
              URL.revokeObjectURL(objectUrlRef.current)
            }
            const url = URL.createObjectURL(file)
            objectUrlRef.current = url
            setThumbnailFile(file)
            setClearThumbnail(false)
            setThumbnailPreview(url)
          }}
          className="block w-full text-sm text-gray-400 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-sm file:bg-gray-700 file:text-gray-200 hover:file:bg-gray-600 cursor-pointer"
        />
        <p className="text-xs text-gray-500 mt-1">JPEG or PNG, max size 2000x2000, recommend a 2:1 aspect ratio</p>
      </div>

      <div className="flex gap-3 pt-2">
        <button
          type="submit"
          className="flex-1 bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-2 rounded-lg transition-colors"
        >
          {isEditing ? 'Save Changes' : 'Add Series'}
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

