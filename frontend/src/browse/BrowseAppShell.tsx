import { Outlet } from 'react-router-dom';
import { BrowseHeader } from './BrowseHeader';

export function BrowseAppShell(): JSX.Element {
  return (
    <div className="min-h-screen bg-bg-app">
      <BrowseHeader />
      <main className="page-shell">
        <Outlet />
      </main>
    </div>
  );
}
