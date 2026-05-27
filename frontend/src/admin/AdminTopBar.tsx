import { Link } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { useTheme } from '../theme/ThemeProvider';

export function AdminTopBar(): JSX.Element {
  const { theme, toggleTheme } = useTheme();

  return (
    <header className="sticky top-0 z-30 border-b border-border-subtle bg-header-bg/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-content-muted">MovieNight</p>
          <h1 className="text-lg font-semibold text-content">Admin</h1>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/browse" className="text-sm font-medium text-content-secondary">
            Browse app
          </Link>
          <Button variant="secondary" size="sm" onClick={toggleTheme}>
            {theme === 'light' ? 'Dark mode' : 'Light mode'}
          </Button>
        </div>
      </div>
    </header>
  );
}
