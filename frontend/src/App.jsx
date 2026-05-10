import { BrowserRouter, NavLink, Navigate, Route, Routes } from 'react-router-dom'
import MediaLibraryPage from './pages/MediaLibraryPage'
import { AppConfigProvider, useAppConfig } from './context/AppConfigContext'

function NavItem({ to, children }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) => `px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
        isActive
          ? 'bg-indigo-600 text-white'
          : 'bg-gray-800 text-gray-300 hover:bg-gray-700 hover:text-white'
      }`}
    >
      {children}
    </NavLink>
  )
}

function FullyLocalCheckbox() {
  const { fullyLocal, setFullyLocal } = useAppConfig()
  return (
    <label
      className="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-gray-300"
      title="Select this to generate file-based playlists instead of streaming ones"
    >
      <input
        type="checkbox"
        checked={fullyLocal}
        onChange={e => setFullyLocal(e.target.checked)}
        className="w-4 h-4 accent-indigo-500 cursor-pointer"
      />
      Fully local
    </label>
  )
}

export default function App() {
  return (
    <AppConfigProvider>
      <BrowserRouter>
        <div className="min-h-screen bg-gray-950 text-gray-100">
          <header className="bg-gray-900 border-b border-gray-800 shadow-lg">
            <div className="max-w-screen-2xl mx-auto px-4 py-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-3">
                <span className="text-3xl" aria-hidden="true">🎬</span>
                <div>
                  <h1 className="text-2xl font-bold text-white tracking-tight">MovieNight</h1>
                </div>
              </div>
              <nav className="flex items-center gap-2">
                <NavItem to="/">Browse</NavItem>
                <NavItem to="/admin">Admin</NavItem>
                <FullyLocalCheckbox />
              </nav>
            </div>
          </header>

          <main className="max-w-screen-2xl mx-auto px-4 py-8">
            <Routes>
              <Route path="/" element={<MediaLibraryPage mode="user" />} />
              <Route path="/admin" element={<MediaLibraryPage mode="admin" />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </AppConfigProvider>
  )
}
