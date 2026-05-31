import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { getPlaylistLocalPreference, setPlaylistLocalPreference } from '../lib/playlist';
import { useTheme } from '../theme/ThemeProvider';

function ThemeToggle(): JSX.Element {
  const { theme, toggleTheme } = useTheme();

  return (
    <Button variant="secondary" size="sm" onClick={toggleTheme}>
      {theme === 'light' ? 'Dark mode' : 'Light mode'}
    </Button>
  );
}

function PlaylistLocalToggle(): JSX.Element {
  const [isLocal, setIsLocal] = useState<boolean>(() => getPlaylistLocalPreference());

  const onChange = (nextValue: boolean): void => {
    setIsLocal(nextValue);
    setPlaylistLocalPreference(nextValue);
  };

  return (
    <label className="flex items-center gap-2 text-sm text-content-secondary" htmlFor="playlist-local-toggle">
      <input
        id="playlist-local-toggle"
        type="checkbox"
        checked={isLocal}
        onChange={(event) => onChange(event.currentTarget.checked)}
      />
      Local VLC paths
    </label>
  );
}

export function BrowseHeader(): JSX.Element {
  return (
    <header className="sticky top-0 z-30 border-b border-border-subtle bg-header-bg/95 backdrop-blur">
      <div className="mx-auto flex max-w-screen-2xl items-center justify-between gap-2 px-2 py-2 sm:px-4 lg:px-6">
        <div className="flex items-center gap-6">
        <Link to="/browse" className="text-3xl font-bold leading-none text-content no-underline">
          🎬 MovieNight
        </Link>
          <nav className="hidden items-center gap-4 text-sm text-content-secondary md:flex">
            <NavLink to="/browse" end className={({ isActive }) => (isActive ? 'text-content font-medium' : '')}>
              Browse
            </NavLink>
            <NavLink to="/browse/search" className={({ isActive }) => (isActive ? 'text-content font-medium' : '')}>
              Search
            </NavLink>
            <NavLink to="/admin" className="">
              Admin
            </NavLink>
          </nav>
        </div>
        <div className="flex items-center gap-3">
          <PlaylistLocalToggle />
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
