import { useState, useEffect, useCallback } from 'react'

const FILES_API = '/api/files'

const VIDEO_EXTENSIONS = new Set([
  'mp4', 'mkv', 'avi', 'mov', 'wmv', 'flv', 'webm', 'm4v', 'mpg', 'mpeg', 'ts', 'm2ts',
])

function isVideoFile(name) {
  const dotIndex = name.lastIndexOf('.')
  if (dotIndex === -1) return false
  const ext = name.slice(dotIndex + 1).toLowerCase()
  return VIDEO_EXTENSIONS.has(ext)
}

export default function FileBrowserModal({ initialPath, onSelect, onClose }) {
  const [currentPath, setCurrentPath] = useState(initialPath || '/')
  const [parentPath, setParentPath] = useState('/')
  const [entries, setEntries] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [pathInput, setPathInput] = useState(initialPath || '/')

  const loadDirectory = useCallback(async (path) => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`${FILES_API}?path=${encodeURIComponent(path)}`)
      if (!res.ok) throw new Error(`Failed to list directory (${res.status})`)
      const data = await res.json()
      setCurrentPath(data.path)
      setParentPath(data.parent)
      setPathInput(data.path)
      setEntries(data.entries)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadDirectory(initialPath || '/')
  }, [initialPath, loadDirectory])

  const handleEntryClick = (entry) => {
    if (entry.type === 'directory') {
      loadDirectory(entry.path)
    } else {
      onSelect(entry.path)
    }
  }

  const handlePathSubmit = (e) => {
    e.preventDefault()
    loadDirectory(pathInput)
  }

  return (
    <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-[60] p-4">
      <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-xl flex flex-col max-h-[80vh]">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-700">
          <h3 className="text-lg font-bold text-white">Browse for Video File</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white text-xl leading-none"
            aria-label="Close file browser"
          >
            ×
          </button>
        </div>

        {/* Path bar */}
        <form onSubmit={handlePathSubmit} className="flex gap-2 px-5 py-3 border-b border-gray-700">
          <input
            value={pathInput}
            onChange={(e) => setPathInput(e.target.value)}
            className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-3 py-1.5 text-gray-100 text-sm focus:outline-none focus:border-indigo-500 font-mono"
            aria-label="Current path"
          />
          <button
            type="submit"
            className="bg-gray-700 hover:bg-gray-600 text-gray-200 px-3 py-1.5 rounded-lg text-sm transition-colors"
          >
            Go
          </button>
        </form>

        {/* Navigation */}
        <div className="px-5 py-2 border-b border-gray-700">
          <button
            onClick={() => loadDirectory(parentPath)}
            disabled={currentPath === parentPath}
            className="text-indigo-400 hover:text-indigo-300 text-sm disabled:opacity-40 disabled:cursor-not-allowed"
          >
            ↑ Parent directory
          </button>
        </div>

        {/* File listing */}
        <div className="flex-1 overflow-y-auto px-5 py-2">
          {loading && (
            <div className="text-center text-gray-400 py-8">Loading…</div>
          )}
          {error && (
            <div className="text-red-400 text-sm py-4">{error}</div>
          )}
          {!loading && !error && entries.length === 0 && (
            <div className="text-gray-500 text-sm py-4 text-center">No files found</div>
          )}
          {!loading && !error && entries.map((entry) => {
            const isDir = entry.type === 'directory'
            const isVideo = !isDir && isVideoFile(entry.name)
            return (
              <button
                key={`${entry.type}:${entry.name}`}
                onClick={() => handleEntryClick(entry)}
                className={`w-full text-left flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors ${
                  isDir
                    ? 'text-indigo-300 hover:bg-gray-800'
                    : isVideo
                      ? 'text-gray-200 hover:bg-gray-800'
                      : 'text-gray-500 hover:bg-gray-800/50 cursor-default'
                }`}
                disabled={!isDir && !isVideo}
              >
                <span className="text-base leading-none">
                  {isDir ? '📁' : isVideo ? '🎬' : '📄'}
                </span>
                <span className="truncate">{entry.name}</span>
                {isDir && <span className="ml-auto text-gray-600 text-xs">→</span>}
              </button>
            )
          })}
        </div>
      </div>
    </div>
  )
}
